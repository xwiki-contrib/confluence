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

import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.renderer.PrintRenderer;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Normalized Plain Filter.
 * @since 9.92.0
 */
class RightContextAnnotationFilter extends NormalizedPlainFilter
{
    private final Consumer<String> callback;
    private boolean stopped;

    /* we don't send end* inline events that had no corresponding "begin" inline event to the printer. This can happen
     * because we are at the middle of a line when creating a right selection context. */
    private int formats;
    private int groups;
    private int metdataCount;
    private int links;

    RightContextAnnotationFilter(PrintRenderer plainRenderer, Listener wrappedListener, Consumer<String> callback)
    {
        super(plainRenderer, wrappedListener);
        this.callback = callback;
    }

    void stop()
    {
        if (!stopped) {
            stopped = true;
            callback.accept(consumeString());
        }
    }

    @Override
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        ++formats;
        super.beginFormat(format, parameters);
    }

    @Override
    public void beginGroup(Map<String, String> parameters)
    {
        ++groups;
        super.beginGroup(parameters);
    }

    @Override
    public void beginLink(ResourceReference reference, boolean freestanding, Map<String, String> parameters)
    {
        ++links;
        super.beginLink(reference, freestanding, parameters);
    }

    @Override
    public void beginMetaData(MetaData metadata)
    {
        ++metdataCount;
        super.beginMetaData(metadata);
    }

    @Override
    public void endFormat(Format format, Map<String, String> parameters)
    {
        if (--formats >= 0) {
            super.endFormat(format, parameters);
        } else {
            getWrappedListener().endFormat(format, parameters);
        }
    }

    @Override
    public void endGroup(Map<String, String> parameters)
    {
        if (--groups >= 0) {
            super.endGroup(parameters);
        } else {
            getWrappedListener().endGroup(parameters);
        }
    }

    @Override
    public void endLink(ResourceReference reference, boolean freestanding, Map<String, String> parameters)
    {
        if (--links >= 0) {
            super.endLink(reference, freestanding, parameters);
        } else {
            getWrappedListener().endLink(reference, freestanding, parameters);
        }
    }

    @Override
    public void endMetaData(MetaData metadata)
    {
        if (--metdataCount >= 0) {
            super.endMetaData(metadata);
        } else {
            getWrappedListener().endMetaData(metadata);
        }
    }

    @Override
    public void endDefinitionDescription()
    {
        stop();
        super.endDefinitionDescription();
    }

    @Override
    public void endDefinitionList(Map<String, String> parameters)
    {
        stop();
        super.endDefinitionList(parameters);
    }

    @Override
    public void endDefinitionTerm()
    {
        stop();
        super.endDefinitionTerm();
    }

    @Override
    public void endDocument(MetaData metadata)
    {
        stop();
        super.endDocument(metadata);
    }

    @Override
    public void endFigure(Map<String, String> parameters)
    {
        stop();
        super.endFigure(parameters);
    }

    @Override
    public void endFigureCaption(Map<String, String> parameters)
    {
        stop();
        super.endFigureCaption(parameters);
    }

    @Override
    public void endHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        stop();
        super.endHeader(level, id, parameters);
    }

    @Override
    public void endList(ListType type, Map<String, String> parameters)
    {
        stop();
        super.endList(type, parameters);
    }

    @Override
    public void endListItem()
    {
        stop();
        super.endListItem();
    }

    @Override
    public void endListItem(Map<String, String> parameters)
    {
        stop();
        super.endListItem(parameters);
    }

    @Override
    public void endParagraph(Map<String, String> parameters)
    {
        stop();
        super.endParagraph(parameters);
    }

    @Override
    public void endQuotation(Map<String, String> parameters)
    {
        stop();
        super.endQuotation(parameters);
    }

    @Override
    public void endQuotationLine()
    {
        stop();
        super.endQuotationLine();
    }

    @Override
    public void endSection(Map<String, String> parameters)
    {
        stop();
        super.endSection(parameters);
    }

    @Override
    public void endTable(Map<String, String> parameters)
    {
        stop();
        super.endTable(parameters);
    }

    @Override
    public void endTableCell(Map<String, String> parameters)
    {
        stop();
        super.endTableCell(parameters);
    }

    @Override
    public void endTableRow(Map<String, String> parameters)
    {
        stop();
        super.endTableRow(parameters);
    }

    @Override
    public void endTableHeadCell(Map<String, String> parameters)
    {
        stop();
        super.endTableHeadCell(parameters);
    }

    @Override
    public void endMacroMarker(String name, Map<String, String> macroParameters, String content, boolean isInline)
    {
        stop();
        super.endMacroMarker(name, macroParameters, content, isInline);
    }

    @Override
    public void onEmptyLines(int count)
    {
        stop();
        super.onEmptyLines(count);
    }

    @Override
    public void onHorizontalLine(Map<String, String> parameters)
    {
        stop();
        super.onHorizontalLine(parameters);
    }

    @Override
    public void onImage(ResourceReference reference, boolean freestanding, Map<String, String> parameters)
    {
        stop();
        super.onImage(reference, freestanding, parameters);
    }

    @Override
    public void onImage(ResourceReference reference, boolean freestanding, String id, Map<String, String> parameters)
    {
        stop();
        super.onImage(reference, freestanding, id, parameters);
    }

    @Override
    public void onMacro(String id, Map<String, String> parameters, String content, boolean inline)
    {
        stop();
        super.onMacro(id, parameters, content, inline);
    }

    @Override
    public void onNewLine()
    {
        stop();
        super.onNewLine();
    }
}
