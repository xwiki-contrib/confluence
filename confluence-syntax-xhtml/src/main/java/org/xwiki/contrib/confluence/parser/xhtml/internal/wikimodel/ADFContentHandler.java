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

import org.xwiki.contrib.confluence.parser.xhtml.internal.ConfluenceXHTMLParser;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.MacroTagHandler.ConfluenceMacro;

/**
 * Handles the content of adf nodes.
 *
 * @version $Id$
 * @since 9.25.0
 */
public class ADFContentHandler extends AbstractRichContentTagHandler
{
    protected static final String ADF_CONTENT = "adf-content";

    /**
     * @param parser is used access the parser and the rendering to use to manipulate the content
     */
    public ADFContentHandler(ConfluenceXHTMLParser parser)
    {
        super(parser);
    }

    @Override
    protected void endContent(String content, TagContext context)
    {
        ConfluenceMacro macro = (ConfluenceMacro) context.getTagStack().getStackParameter(CONFLUENCE_CONTAINER);
        if (macro != null) {
            macro.content = content;
        }
    }
}
