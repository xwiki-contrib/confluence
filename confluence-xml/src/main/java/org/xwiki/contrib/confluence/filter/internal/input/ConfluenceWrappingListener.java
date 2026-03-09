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

import org.slf4j.LoggerFactory;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

final class ConfluenceWrappingListener extends WrappingListener
{
    private static final String NORMALIZER_PLAIN_1_0 = "normalizer-plain/1.0";

    private Map<String, Integer> macroIds;
    private ComponentManager componentManager;

    private final Deque<Listener> queuedListeners = new ArrayDeque<>();
    private final Deque<NormalizedPlainFilter> recorders = new ArrayDeque<>();
    private final CompositeListener compositeListener = new CompositeListener();
    private final QueueListener queueListener = new QueueListener();
    private final WrappingListener wrappingListener = new WrappingListener();
    private final Map<String, RightContextAnnotationFilter> rightSelectionHandlers = new HashMap<>();

    ConfluenceWrappingListener()
    {
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
        Iterator<Listener> listenerIterator = queuedListeners.descendingIterator();
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
        PrintRenderer renderer = this.componentManager.getInstance(PrintRenderer.class, NORMALIZER_PLAIN_1_0);

        // Add the normalizer renderer to the receiving renders
        NormalizedPlainFilter recorder = new NormalizedPlainFilter(renderer, super.getWrappedListener());
        recorders.push(recorder);
        queueEvents(recorder);
    }

    NormalizedPlainFilter stopRecordingPlainTextEvents()
    {
        if (this.queuedListeners.peek() instanceof NormalizedPlainFilter) {
            NormalizedPlainFilter recorder = recorders.pop();
            dequeueEvents(recorder);
            return recorder;
        }

        return null;
    }

    void queueEvents(Listener listener)
    {
        queuedListeners.push(listener);
        super.setWrappedListener(listener);
    }

    /**
     * Remove the given listener from the queue. When there are wrapping listeners around, rewire so that the removed
     * listener doesn't receive events anymore.
     * @param listenerToRemove the listener to remove
     */
    void dequeueEvents(Listener listenerToRemove)
    {
        Listener prev = null;
        Iterator<Listener> it = queuedListeners.iterator();
        while (it.hasNext()) {
            Listener cur = it.next();
            if (cur == listenerToRemove) {
                it.remove();
                if (prev instanceof WrappingListener) {
                    Listener next = it.hasNext() ? it.next() : compositeListener;
                    ((WrappingListener) prev).setWrappedListener(next);
                }
                break;
            } else if (cur instanceof RightContextAnnotationFilter) {
                /* FIXME: Calling stop for RightContextAnnotationFilter specifically doesn't feel completely right,
                           a more generic way of handling this would be nice.
                           This complexity mostly comes from the titles being replayed to generate their Confluence
                           anchors in ConfluenceConverterListener.
                */
                ((RightContextAnnotationFilter) cur).stop();
            }
            prev = cur;
        }
        if (queuedListeners.isEmpty()) {
            super.setWrappedListener(compositeListener);
        }
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

    String getSelectionLeftContext()
    {
        return AnnotationUtils.getSelectionLeftContext(componentManager, queuedListeners, queueListener);
    }

    void getSelectionRightContext(String ref, Consumer<String> callback)
    {
        RightContextAnnotationFilter existingFilter = rightSelectionHandlers.remove(ref);
        if (existingFilter != null) {
            dequeueEvents(existingFilter);
        }

        PrintRenderer renderer;
        try {
            renderer = this.componentManager.getInstance(PrintRenderer.class, NORMALIZER_PLAIN_1_0);
        } catch (ComponentLookupException e) {
            LoggerFactory.getLogger(getClass()).error("Failed to get the normalizer printer. "
                + "Right selection contexts of annotations will be missing.");
            return;
        }

        RightContextAnnotationFilter rightContextAnnotationFilter = new RightContextAnnotationFilter(
                renderer,
                super.getWrappedListener(),
                rightSelectionContext -> {
                    dequeueEvents(rightSelectionHandlers.remove(ref));
                    callback.accept(rightSelectionContext);
                }
        );
        rightSelectionHandlers.put(ref, rightContextAnnotationFilter);
        queueEvents(rightContextAnnotationFilter);
    }

    void countMacro(String id)
    {
        if (!id.startsWith("CONFLUENCE_xwiki-") && macroIds != null && this.queuedListeners.isEmpty()) {
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
