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

import org.xwiki.rendering.wikimodel.xhtml.handler.TagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Handles list items. There is already ListItemTagHandler, but we override this to handle complex cases where list
 * items contain complex content, including nested lists.
 * @version $Id$
 * @since 9.52.0
 */
public class ConfluenceListItemTagHandler extends TagHandler
{
    /**
     * Constructor.
     */
    public ConfluenceListItemTagHandler()
    {
        super(true);
    }

    @Override
    protected void begin(TagContext context)
    {
        context.getScannerContext().onMacro("confluence_li_start", context.getParams(), null, false);
    }

    @Override
    protected void end(TagContext context)
    {
        // See ConfluenceXWikiGeneratorListener#onMacroInline to see the complex treatment we reserve for this macro
        context.getScannerContext().onMacro("confluence_li_end", context.getParams(), null, false);
    }
}
