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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.QueueListener;
import org.xwiki.rendering.listener.chaining.EventType;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

final class AnnotationUtils
{
    private static final List<EventType> START_TEXTAREA_EVENT_TYPES = List.of(
            EventType.BEGIN_FIGURE,
            EventType.BEGIN_TABLE,
            EventType.BEGIN_QUOTATION,
            EventType.BEGIN_QUOTATION_LINE,
            EventType.BEGIN_DEFINITION_DESCRIPTION,
            EventType.BEGIN_DEFINITION_LIST,
            EventType.BEGIN_DEFINITION_TERM,
            EventType.BEGIN_DOCUMENT,
            EventType.BEGIN_HEADER,
            EventType.BEGIN_LIST,
            EventType.BEGIN_LIST_ITEM,
            EventType.BEGIN_PARAGRAPH,
            EventType.BEGIN_TABLE_CELL,
            EventType.BEGIN_TABLE_HEAD_CELL,
            EventType.BEGIN_TABLE_ROW,
            EventType.ON_MACRO);

    private AnnotationUtils()
    {
        // ignore
    }

    static String getSelectionLeftContext(ComponentManager componentManager, Deque<Listener> listeners,
          QueueListener queueListener)
    {
        Iterator<Listener> listenerIterator = listeners.descendingIterator();
        List<ListIterator<QueueListener.Event>> iterators = new ArrayList<>(listeners.size() + 1);
        while (listenerIterator.hasNext()) {
            Listener maybeQueueListener = listenerIterator.next();
            if (maybeQueueListener instanceof QueueListener) {
                QueueListener q = (QueueListener) maybeQueueListener;
                if (recordEvents(q, iterators)) {
                    return iteratorsToString(componentManager, iterators);
                }
            }
        }

        recordEvents(queueListener, iterators);
        return iteratorsToString(componentManager, iterators);
    }

    private static boolean recordEvents(QueueListener q, List<ListIterator<QueueListener.Event>> iterators)
    {
        ListIterator<QueueListener.Event> queueIterator = q.listIterator(q.size());
        iterators.add(queueIterator);
        while (queueIterator.hasPrevious()) {
            QueueListener.Event e = queueIterator.previous();
            if (isEndOfBeforeSelection(e)) {
                queueIterator.next();
                return true;
            }
        }
        return false;
    }

    private static boolean isEndOfBeforeSelection(QueueListener.Event e)
    {
        return START_TEXTAREA_EVENT_TYPES.contains(e.eventType);
    }

    private static String iteratorsToString(ComponentManager componentManager,
        List<ListIterator<QueueListener.Event>> iterators)
    {
        PrintRenderer plainRenderer;
        try {
            plainRenderer = componentManager.getInstance(PrintRenderer.class, "normalizer-plain/1.0");
        } catch (ComponentLookupException e) {
            LoggerFactory.getLogger(AnnotationUtils.class).error(
                    "Failed to an instance of PrintRenderer, needed to import annotation contexts. "
                            + "Some inline comments may be misplaced", e);
            return "";
        }

        plainRenderer.setPrinter(new DefaultWikiPrinter());
        for (int i = iterators.size() - 1; i >= 0; i--) {
            ListIterator<QueueListener.Event> iterator = iterators.get(i);
            while (iterator.hasNext()) {
                QueueListener.Event e = iterator.next();
                e.eventType.fireEvent(plainRenderer, e.eventParameters);
            }
        }
        // the special symbol followed by the chop is a hack to keep the trailing whitespaces
        plainRenderer.onSpecialSymbol('!');
        return StringUtils.chop(plainRenderer.getPrinter().toString());
    }
}
