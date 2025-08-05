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

import org.xwiki.rendering.wikimodel.xhtml.handler.TableRowTagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

import static org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceTagHandler.CONFLUENCE_TABLE_CURRENT_COL;
import static org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceTagHandler.CONFLUENCE_TABLE_ROWSPANS;

/**
 * Table row tag handler.
 * @since 9.85.0
 * @version $Id$
 */
public class ConfluenceTableRowTagHandler extends TableRowTagHandler
{
    /**
     * Constructor.
     */
    public ConfluenceTableRowTagHandler()
    {
        super();
    }

    @Override
    protected void begin(TagContext context)
    {
        context.getTagStack().pushStackParameter(CONFLUENCE_TABLE_CURRENT_COL, -1);
        updateRowspans(context);
        super.begin(context);
    }

    private static void updateRowspans(TagContext context)
    {
        // For rowspan values we have, we decrement all values that are not already 0.
        // negative values mean the affected columns are taken by some previous row spanning cell for the whole
        // table (rowspan = 0).
        List<Integer> rowspans = (List<Integer>) context.getTagStack().getStackParameter(CONFLUENCE_TABLE_ROWSPANS);
        if (rowspans == null) {
            return;
        }

        boolean allZeros = true;
        boolean updated = false;
        int length = rowspans.size();
        for (int i = 0; i < length; i++) {
            int val = rowspans.get(i);
            if (val > 0) {
                val--;
                rowspans.set(i, val);
                updated = true;
            }

            if (val != 0) {
                allZeros = false;
            }
        }

        if (updated) {
            context.getTagStack().setStackParameter(CONFLUENCE_TABLE_ROWSPANS, allZeros ? null : rowspans);
        }
    }

    @Override
    protected void end(TagContext context)
    {
        context.getTagStack().popStackParameter(CONFLUENCE_TABLE_CURRENT_COL);
        super.end(context);
    }
}
