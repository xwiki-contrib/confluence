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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.contrib.confluence.parser.xhtml.internal.ConfluenceXHTMLParser;
import org.xwiki.rendering.wikimodel.WikiParameters;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Convert an element into a macro.
 * 
 * @version $Id$
 * @since 9.8
 */
public class ElementMacroTagHandler extends AbstractRichContentTagHandler
{
    class UnknownMacro
    {
        private String name;

        private WikiParameters parameters = new WikiParameters();

        UnknownMacro(String name, WikiParameters parameters)
        {
            this.name = name;
            this.parameters = parameters;
        }
    }

    /**
     * @param parser is used access the parser and the rendering to use to manipulate the content
     */
    public ElementMacroTagHandler(ConfluenceXHTMLParser parser)
    {
        super(parser);
    }

    /**
     * @param context the tag context
     * @return the macro name to use
     */
    protected String getMacroName(TagContext context)
    {
        String name = context.getName();

        // Remove any prefix
        return StringUtils.substringAfter(name, ":");
    }

    @Override
    protected void begin(TagContext context)
    {
        UnknownMacro macro = new UnknownMacro(getMacroName(context), context.getParams());

        context.getTagStack().pushStackParameter(CONFLUENCE_CONTAINER, macro);

        super.begin(context);
    }

    @Override
    protected void endContent(String content, TagContext context)
    {
        UnknownMacro macro = (UnknownMacro) context.getTagStack().popStackParameter(CONFLUENCE_CONTAINER);

        context.getScannerContext().onMacroBlock(macro.name, macro.parameters, content);
    }
}
