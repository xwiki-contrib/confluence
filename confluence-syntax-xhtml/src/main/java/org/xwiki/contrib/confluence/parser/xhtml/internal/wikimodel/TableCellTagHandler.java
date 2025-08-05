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
package org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.rendering.wikimodel.WikiParameter;
import org.xwiki.rendering.wikimodel.WikiParameters;
import org.xwiki.rendering.wikimodel.xhtml.handler.TableDataTagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

import static org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceTagHandler.CONFLUENCE_TABLE_COLUMN_ATTRIBUTES;
import static org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceTagHandler.CONFLUENCE_TABLE_CURRENT_COL;
import static org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceTagHandler.CONFLUENCE_TABLE_ROWSPANS;

/**
 * Make sure to produce something that won't break xwiki/2.x table. See https://jira.xwiki.org/browse/XRENDERING-488
 * 
 * @version $Id$
 * @since 9.1.5
 */
public class TableCellTagHandler extends TableDataTagHandler
{

    private static final Logger LOGGER = LoggerFactory.getLogger(TableCellTagHandler.class);
    private static final String SEMI = ";";

    @Override
    protected void begin(TagContext context)
    {
        beginCell(context);
        beginDocument(context);
    }

    static void beginCell(TagContext context)
    {
        WikiParameters wikiParameters = advanceCurrentColAndFetchColumnStyle(context);
        context.getScannerContext().beginTableCell(
            context.isTag("th"),
            wikiParameters);
    }

    @Override
    protected void end(TagContext context)
    {
        endDocument(context);
        super.end(context);
    }

    private static WikiParameters fetchColumnStyle(TagContext context, int currentCol, int colspan, int rowspan)
    {
        Map<String, String> attributes = fetchColumnAttributes(context, currentCol);
        if (attributes == null) {
            return context.getParams();
        }

        for (Map.Entry<String, String> attr : attributes.entrySet()) {
            String attrName = attr.getKey();
            WikiParameter existingParam = context.getParams().getParameter(attrName);
            boolean isStyle = "style".equals(attrName);
            if (isStyle) {
                // merge the styles, giving precedence to the more specific one by putting it at the end
                String existingStyle = existingParam == null ? "" : existingParam.getValue();
                String newStyle = StringUtils.isEmpty(existingStyle)
                    ? attr.getValue()
                    : removeSemicolon(attr.getValue()) + "; " + existingStyle;
                return context.getParams().setParameter(attrName, maybeRemoveSize(newStyle, colspan, rowspan));
            } else if (existingParam == null) {
                // we don't overwrite the more specific attributes
                return context.getParams().setParameter(attrName, attr.getValue());
            }
        }
        return context.getParams();
    }

    private static String maybeRemoveSize(String newStyle, int colspan, int rowspan)
    {
        if ((colspan == 1 && rowspan == 1) || StringUtils.isEmpty(newStyle)) {
            return newStyle;
        }

        Stream<String> stream = Arrays.stream(newStyle.split(SEMI));

        if (colspan != 1) {
            stream = stream.filter(s -> !s.matches("^\\s*width\\s*:[\\s\\S]+"));
        }

        if (rowspan != 1) {
            stream = stream.filter(s -> !s.matches("^\\s*height\\s*:[\\s\\S]+"));
        }

        return stream.collect(Collectors.joining(SEMI)).trim();
    }

    private static String removeSemicolon(String value)
    {
        String v = value.trim();
        if (v.endsWith(SEMI)) {
            v = v.substring(0, v.length() - 1).trim();
        }
        return v;
    }

    private static Map<String, String> fetchColumnAttributes(TagContext context, int currentCol)
    {
        List<Map<String, String>> tableAttributes =
            (List<Map<String, String>>) context.getTagStack().getStackParameter(CONFLUENCE_TABLE_COLUMN_ATTRIBUTES);
        if (tableAttributes == null) {
            return null;
        }
        if (currentCol >= tableAttributes.size()) {
            return null;
        }
        return tableAttributes.get(currentCol);
    }

    private static WikiParameters advanceCurrentColAndFetchColumnStyle(TagContext context)
    {
        Integer currentCol = (Integer) context.getTagStack().getStackParameter(CONFLUENCE_TABLE_CURRENT_COL);
        if (currentCol == null) {
            LOGGER.error("Could not determine the current column. This should not happen.");
            return null;
        }
        int colspan = getSpanParam(context, TableCellTagHandler.LOGGER, "colspan");
        int rowspan = getSpanParam(context, TableCellTagHandler.LOGGER, "rowspan");
        int beginCol = currentCol + 1;
        List<Integer> rowspans = (List<Integer>) context.getTagStack().getStackParameter(CONFLUENCE_TABLE_ROWSPANS);
        if (rowspans != null) {
            // if there are cells spanning rows in the previous row, we skip the indices
            while (beginCol < rowspans.size() && !Objects.equals(rowspans.get(beginCol), 0)) {
                beginCol++;
            }
        }
        updateRowspans(context, rowspans, rowspan, beginCol, colspan);
        int nextCol = beginCol + Math.max(0, colspan - 1);
        // note: we don't handle colspan=0 in a particular way. nextCol will have a nonsensitical value if colspan=0,
        // but it wouldn't make sense for this value to be used in this case: we don't expect any further cell in the
        // row after a colspan=0 cell which is supposed to span to the end of the line.
        context.getTagStack().setStackParameter(CONFLUENCE_TABLE_CURRENT_COL, nextCol);
        return fetchColumnStyle(context, beginCol, colspan, rowspan);
    }

    private static void updateRowspans(TagContext context, List<Integer> rowspans, int rowspan,
        int beginCol, int colspan)
    {
        List<Integer> updatedRowspans = rowspans;
        if (updatedRowspans == null) {
            if (rowspan == 1) {
                // Happy path.
                // We've not seen any rowspan ≠ 1 in previous rows and still don't need to track any, nothing to do
                return;
            }
            updatedRowspans = new ArrayList<>(beginCol + colspan);
        }

        boolean updated = false;

        for (int i = updatedRowspans.size(); i < beginCol; i++) {
            updatedRowspans.add(0);
        }

        for (int i = beginCol; i < beginCol + colspan; i++) {
            if (i < updatedRowspans.size()) {
                int val = updatedRowspans.get(i);
                if (val > -1) {
                    // We only update non-negative values. Negative values indicate columns that are already taken
                    // forever by cells in previous rows
                    updatedRowspans.set(i, rowspan == 0 ? -1 : (val + rowspan));
                    updated = true;
                }
            } else {
                updatedRowspans.add(rowspan == 0 ? -1 : rowspan);
                updated = true;
            }
        }

        if (updated) {
            context.getTagStack().setStackParameter(CONFLUENCE_TABLE_ROWSPANS, updatedRowspans);
        }
    }

    private static int getSpanParam(TagContext context, Logger logger, String paramName)
    {
        int colspan = 1;
        WikiParameter spanParam = context.getParams().getParameter(paramName);
        if (spanParam != null) {
            try {
                colspan = Integer.parseInt(spanParam.getValue());
            } catch (Exception e) {
                logger.error("Could not parse the [{}] parameter of the cell (value: [{}]), expected an integer",
                    paramName, spanParam.getValue(), e);
            }
        }
        return colspan;
    }
}
