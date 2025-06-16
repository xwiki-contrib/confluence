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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.rendering.wikimodel.WikiParameter;
import org.xwiki.rendering.wikimodel.WikiParameters;
import org.xwiki.rendering.wikimodel.xhtml.handler.TableDataTagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

import static org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceTagHandler.CONFLUENCE_TABLE_COLUMN_ATTRIBUTES;
import static org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceTagHandler.CONFLUENCE_TABLE_CURRENT_COL;

/**
 * Make sure to produce something that won't break xwiki/2.x table. See https://jira.xwiki.org/browse/XRENDERING-488
 * 
 * @version $Id$
 * @since 9.1.5
 */
public class TableCellTagHandler extends TableDataTagHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TableCellTagHandler.class);

    @Override
    protected void begin(TagContext context)
    {
        beginCell(context);
        beginDocument(context);
    }

    static void beginCell(TagContext context)
    {
        WikiParameters wikiParameters = fetchColumnStyle(context);
        context.getScannerContext().beginTableCell(
            context.isTag("th"),
            wikiParameters);
    }

    @Override
    protected void end(TagContext context)
    {
        endDocument(context);
        advanceCurrentCol(context, LOGGER);
        super.end(context);
    }

    static WikiParameters fetchColumnStyle(TagContext context)
    {
        Map<String, String> attributes = fetchColumnAttributes(context);
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
                return context.getParams().setParameter(attrName, newStyle);
            } else if (existingParam == null) {
                // we don't overwrite the more specific attributes
                return context.getParams().setParameter(attrName, attr.getValue());
            }
        }
        return context.getParams();
    }

    private static String removeSemicolon(String value)
    {
        String v = value.trim();
        if (v.endsWith(";")) {
            v = v.substring(0, v.length() - 1).trim();
        }
        return v;
    }

    private static Map<String, String> fetchColumnAttributes(TagContext context)
    {
        Integer currentCol = (Integer) context.getTagStack().getStackParameter(CONFLUENCE_TABLE_CURRENT_COL);
        if (currentCol == null || currentCol.equals(-1)) {
            return null;
        }
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

    static void advanceCurrentCol(TagContext context, Logger logger)
    {
        Integer currentCol = (Integer) context.getTagStack().getStackParameter(CONFLUENCE_TABLE_CURRENT_COL);
        if (currentCol == null) {
            LOGGER.error("Could not determine the current column. This should not happen.");
            return;
        }
        int colspan = 1;
        WikiParameter spanParam = context.getParams().getParameter("colspan");
        if (spanParam != null) {
            try {
                colspan = Integer.parseInt(spanParam.getValue());
            } catch (Exception e) {
                logger.error("Could not parse the colspan parameter of the cell (value: [{}]), expected an integer",
                    spanParam.getValue(), e);
            }
        }

        context.getTagStack().setStackParameter(CONFLUENCE_TABLE_CURRENT_COL, currentCol + colspan);
    }
}
