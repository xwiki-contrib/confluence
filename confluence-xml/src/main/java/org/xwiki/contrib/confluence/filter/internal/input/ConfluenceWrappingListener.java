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

import org.xwiki.contrib.confluence.filter.ConfluenceFilterReferenceConverter;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.QueueListener;
import org.xwiki.rendering.listener.WrappingListener;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import static org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverter.getConfluenceServerAnchor;
import static org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverter.spacesToDash;

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

    private ConfluenceFilterReferenceConverter confluenceConverter;
    private Map<String, Integer> macroIds;
    private boolean isConfluenceCloud;

    @Override
    public void onMacro(String id, Map<String, String> parameters, String content, boolean inline)
    {
        countMacro(id);

        if (ConfluenceConverterListener.ID_MACRO_NAME.equals(id)) {
            handleIdMacro(parameters, content, inline);
        } else {
            super.onMacro(id, parameters, content, inline);
        }
    }

    private void handleIdMacro(Map<String, String> parameters, String content, boolean inline)
    {
        String name = parameters.get(ConfluenceConverterListener.ID_MACRO_NAME_PARAMETER);
        if (name == null || name.isEmpty()) {
            return;
        }

        String currentPageTitle = ((ConfluenceConverter) confluenceConverter).getCurrentPageTitleForAnchor();

        if (isConfluenceCloud) {
            String dashedName = spacesToDash(name);
            getWrappedListener().onMacro(
                    ConfluenceConverterListener.ID_MACRO_NAME,
                    Map.of(ConfluenceConverterListener.ID_MACRO_NAME_PARAMETER, dashedName),
                    content,
                    inline
            );

            if (currentPageTitle != null) {
                getWrappedListener().onId(spacesToDash(currentPageTitle) + '-' + dashedName);
            }
        } else if (currentPageTitle != null) {
            String confluenceServerAnchor = getConfluenceServerAnchor(currentPageTitle, name);
            getWrappedListener().onMacro(
                    ConfluenceConverterListener.ID_MACRO_NAME,
                    Map.of(ConfluenceConverterListener.ID_MACRO_NAME_PARAMETER, confluenceServerAnchor),
                    content,
                    inline
            );
        }
    }

    void queueEvents()
    {
        queueEvents(new QueueListener());
    }

    void queueEvents(Listener listener)
    {
        this.previousListenerStack.push(getWrappedListener());
        this.contentListenerStack.push(listener);
        super.setWrappedListener(listener);
    }

    Listener dequeueEvents()
    {
        Listener previousListener = this.previousListenerStack.pop();
        super.setWrappedListener(previousListener);
        return this.contentListenerStack.pop();
    }

    <T> T dequeueEvents(Class<T> clazz)
    {
        if (clazz.isInstance(this.contentListenerStack.peek())) {
            return (T) dequeueEvents();
        }

        return null;
    }

    @Override
    public void setWrappedListener(Listener listener)
    {
        if (getWrappedListener() != null) {
            throw new UnsupportedOperationException(
                    "BUG: setWrappedListener was called a second time on ConfluenceWrappingListener. "
                            + "This should not happen. Please report a bug.");
        }

        super.setWrappedListener(listener);
    }

    boolean isQueuingEvents()
    {
        return !this.contentListenerStack.isEmpty();
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

    void setConfluenceConverter(ConfluenceFilterReferenceConverter confluenceConverter)
    {
        this.confluenceConverter = confluenceConverter;
    }

    void setConfluenceCloud(boolean confluenceCloud)
    {
        this.isConfluenceCloud = confluenceCloud;
    }
}
