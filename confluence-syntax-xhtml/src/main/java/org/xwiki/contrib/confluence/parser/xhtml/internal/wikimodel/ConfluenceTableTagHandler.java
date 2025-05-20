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
import java.util.Map;

import org.xwiki.rendering.wikimodel.xhtml.handler.TableTagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

import static org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceTagHandler.CONFLUENCE_TABLE_COLUMN_ATTRIBUTES;

/**
 * Table tag handler.
 * @since 9.85.0
 * @version $Id$
 */
public class ConfluenceTableTagHandler extends TableTagHandler
{
    /**
     * Constructor.
     */
    public ConfluenceTableTagHandler()
    {
        super();
    }

    @Override
    protected void begin(TagContext context)
    {
        context.getTagStack().pushStackParameter(CONFLUENCE_TABLE_COLUMN_ATTRIBUTES,
            new ArrayList<Map<String, String>>());
        super.begin(context);
    }

    @Override
    protected void end(TagContext context)
    {
        context.getTagStack().popStackParameter(CONFLUENCE_TABLE_COLUMN_ATTRIBUTES);
        super.end(context);
    }
}
