/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.confluence.filter.internal.input;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.listener.CompositeListener;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.QueueListener;
import org.xwiki.rendering.listener.WrappingListener;
import org.xwiki.rendering.listener.chaining.EventType;
import org.xwiki.rendering.renderer.PrintRenderer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;

final class ConfluenceWrappingListener extends WrappingListener
{
    /**
     * A stack of queues that are used to record the content of a paragraph with the auto-cursor-target class. For
     * the unlikely case that paragraphs are nested (e.g., because there is a paragraph in a nested macro), a stack
     * is used instead of a single listener.
     */
    private final Deque<Listener> contentListenerStack = new ArrayDeque<>();

    /**
     * A stack of previous listeners that is used to record the previous wrapped listener when a new listener is set
     * while examining the content of a paragraph with the auto-cursor-target class. This is used to restore the
     * previous listener at the end of the paragraph. Again, a stack is used instead of a single listener to handle
     * the unlikely case of nested paragraphs.
     */
    private final Deque<Listener> previousListenerStack = new ArrayDeque<>();

    private Map<String, Integer> macroIds;
    private final QueueListener queueListener = new QueueListener();
    private final WrappingListener wrappingListener = new WrappingListener();
    private ComponentManager componentManager;

    ConfluenceWrappingListener()
    {
        CompositeListener compositeListener = new CompositeListener();
        compositeListener.addListener(wrappingListener);
        compositeListener.addListener(queueListener);
        super.setWrappedListener(compositeListener);
    }

    @Override
    public void onMacro(String id, Map<String, String> parameters, String content, boolean inline)
    {
        countMacro(id);
        if (inline) {
            addEmptyLineIfRightAfterBlockMacro();
        }
        super.onMacro(id, parameters, content, inline);
    }

    @Override
    public void onId(String name)
    {
        addEmptyLineIfRightAfterBlockMacro();
        super.onId(name);
    }

    private void addEmptyLineIfRightAfterBlockMacro()
    {
        // works around a bad interaction with preceding block macros (the generated id macro is stuck next to the block
        // macro if we issue an id instead of a macro)
        QueueListener.Event event = peekLast();
        if (event != null && event.eventType == EventType.ON_MACRO && !((boolean) event.eventParameters[3])) {
            super.onEmptyLines(2);
        }
    }

    private QueueListener.Event peekLast()
    {
        Iterator<Listener> listenerIterator = contentListenerStack.descendingIterator();
        while (listenerIterator.hasNext()) {
            Listener maybeQueueListener = listenerIterator.next();
            if (maybeQueueListener instanceof QueueListener) {
                QueueListener q = (QueueListener) maybeQueueListener;
                if (!q.isEmpty()) {
                    return q.peekLast();
                }
            }
        }

        if (!queueListener.isEmpty()) {
            return queueListener.peekLast();
        }

        return null;
    }

    void recordPlainTextEvents() throws ComponentLookupException
    {
        // Get the renderer in charge of normalizing the coming text content
        PrintRenderer renderer = this.componentManager.getInstance(PrintRenderer.class, "normalizer-plain/1.0");

        // Add the normalizer renderer to the receiving renders
        queueEvents(new NormalizedPlainFilter(renderer, super.getWrappedListener()));
    }

    NormalizedPlainFilter stopRecordingPlainTextEvents()
    {
        if (this.contentListenerStack.peek() instanceof NormalizedPlainFilter) {
            return (NormalizedPlainFilter) dequeueEvents();
        }

        return null;
    }

    void queueEvents()
    {
        queueEvents(new QueueListener());
    }

    void queueEvents(Listener listener)
    {
        this.previousListenerStack.push(super.getWrappedListener());
        this.contentListenerStack.push(listener);
        super.setWrappedListener(listener);
    }

    Listener dequeueEvents()
    {
        Listener previousListener = this.previousListenerStack.pop();
        super.setWrappedListener(previousListener);
        return this.contentListenerStack.pop();
    }

    @Override
    public void setWrappedListener(Listener listener)
    {
        if (wrappingListener.getWrappedListener() != null) {
            throw new UnsupportedOperationException(
                    "BUG: setWrappedListener was called a second time on ConfluenceWrappingListener. "
                            + "This should not happen. Please report a bug.");
        }

        wrappingListener.setWrappedListener(listener);
    }

    @Override
    public Listener getWrappedListener()
    {
        return wrappingListener.getWrappedListener();
    }

    boolean isQueuingEvents()
    {
        return !this.contentListenerStack.isEmpty();
    }

    String getSelectionLeftContext()
    {
        return AnnotationUtils.getSelectionLeftContext(componentManager, contentListenerStack, queueListener);
    }

    void countMacro(String id)
    {
        if (!id.startsWith("CONFLUENCE_xwiki-") && macroIds != null && !isQueuingEvents()) {
            // Don't count the macros if we are recording events, we only count them when actually rendering.
            // Don't count macros that start with CONFLUENCE_xwiki-, they are hacks generated by macro converters.
            macroIds.put(id, macroIds.getOrDefault(id, 0) + 1);
        }
    }

    void setMacroIds(Map<String, Integer> macroIds)
    {
        this.macroIds = macroIds;
    }

    public void setComponentManager(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }
}
