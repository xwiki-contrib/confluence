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

import org.xwiki.rendering.internal.parser.wikimodel.WikiModelStreamParser;
import org.xwiki.rendering.internal.parser.xhtml.wikimodel.XWikiReferenceTagHandler;
import org.xwiki.rendering.wikimodel.WikiParameter;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

import static org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.AbstractMacroParameterTagHandler.IN_CONFLUENCE_PARAMETER;
import static org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceTagHandler.CONFLUENCE_CONTAINER;

/**
 * Handles <a> tags present in confluence parameters.
 * Example:
 * <ac:structured-macro ac:name="macroName">
 *   <ac:parameter ac:name="linkParam">
 *     <a href="https://www.bing.com/images/search?q=icon">https://www.bing.com/images/search?q=icon</a>
 *   </ac:parameter>
 * </ac:structured-macro>
 *
 * @version $Id$
 * @since 9.57.0
 */
public class ConfluenceReferenceTagHandler extends XWikiReferenceTagHandler
{
    /**
     * @param parser the XHTML parser, used for the label
     */
    public ConfluenceReferenceTagHandler(WikiModelStreamParser parser)
    {
        super(parser);
    }

    @Override
    protected void begin(TagContext context)
    {
        if (context.getTagStack().getStackParameter(IN_CONFLUENCE_PARAMETER) == null) {
            super.begin(context);
            return;
        }

        setAccumulateContent(true);
    }

    @Override
    protected void end(TagContext context)
    {
        if (context.getTagStack().getStackParameter(IN_CONFLUENCE_PARAMETER) == null) {
            super.end(context);
        }

        MacroTagHandler.ConfluenceMacro macro =
            (MacroTagHandler.ConfluenceMacro) context.getTagStack().getStackParameter(CONFLUENCE_CONTAINER);

        if (macro != null) {
            WikiParameter name = context.getParent().getParams().getParameter("ac:name");
            WikiParameter hrefParam = context.getParams().getParameter("href");
            String href = hrefParam == null ? null : hrefParam.getValue();
            if (href != null && name != null) {
                macro.parameters = macro.parameters.addParameter(name.getValue(), href);
            }
        }
    }

}
