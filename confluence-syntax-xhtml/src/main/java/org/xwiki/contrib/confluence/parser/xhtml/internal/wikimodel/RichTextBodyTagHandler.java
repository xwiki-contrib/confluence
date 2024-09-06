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
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.MacroTagHandler.ConfluenceMacro;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Handles text inside a macro which might contain markup.
 * <p>
 * Example:
 * 
 * <pre>
 * {@code <ac:rich-text-body><p>some <em>text</em> here</p></ac:rich-text-body>}
 * </pre>
 *
 * @version $Id$
 * @since 9.0
 */
public class RichTextBodyTagHandler extends AbstractRichContentTagHandler
{
    /**
     * @param parser is used to access the parser and the rendering to use to manipulate the content
     */
    public RichTextBodyTagHandler(ConfluenceXHTMLParser parser)
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
