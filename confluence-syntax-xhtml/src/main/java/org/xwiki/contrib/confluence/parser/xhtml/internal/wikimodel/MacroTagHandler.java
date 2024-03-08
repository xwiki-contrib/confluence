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

import org.xwiki.rendering.wikimodel.WikiParameters;
import org.xwiki.rendering.wikimodel.impl.IWikiScannerContext;
import org.xwiki.rendering.wikimodel.xhtml.handler.TagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Handles macros.
 * <p>
 * Example:
 * <p>
 * {@code
 * <ac:macro ac:name="code">
 *   <ac:parameter ac:name="title">Bloc code title</ac:parameter>
 *   <ac:parameter ac:name="theme">theme</ac:parameter>
 *   <ac:parameter ac:name="language">language</ac:parameter>
 *   <ac:plain-text-body><![CDATA[Content of bloc code]]></ac:plain-text-body>
 * </ac:macro>
 * }
 *
 * @version $Id$
 * @since 9.0
 */
public class MacroTagHandler extends TagHandler implements ConfluenceTagHandler
{
    /**
     * A Confluence Macro.
     */
    public static class ConfluenceMacro
    {
        /**
         * The name of the Confluence macro.
         */
        public String name;

        /**
         * The macro parameters.
         */
        public WikiParameters parameters = new WikiParameters();

        /**
         * The macro content.
         */
        public String content;

        /**
         * The macro index.
         */
        public int index = -1;
    }

    /**
     * Default constructor.
     */
    public MacroTagHandler()
    {
        super(false);
    }

    @Override
    protected void begin(TagContext context)
    {
        ConfluenceMacro macro = new ConfluenceMacro();

        macro.name = context.getParams().getParameter("ac:name").getValue();

        context.getTagStack().pushStackParameter(CONFLUENCE_CONTAINER, macro);
    }

    @Override
    protected void end(TagContext context)
    {
        ConfluenceMacro macro = (ConfluenceMacro) context.getTagStack().popStackParameter(CONFLUENCE_CONTAINER);

        IWikiScannerContext internContext = context.getScannerContext().getContext();
        // Use reflection to get the fBlockType field from the internContext.
        // This is a hack to get the current block type.
        // This is needed because the WikiModel API doesn't provide a way to get the current block type.
        boolean isInline = false;
        try {
            java.lang.reflect.Field fBlockType = internContext.getClass().getDeclaredField("fBlockType");
            fBlockType.setAccessible(true);
            int blockType = fBlockType.getInt(internContext);
            isInline = blockType != 0;
        } catch (Exception e) {
            // Ignore
        }

        context.getScannerContext().onMacro(macro.name, macro.parameters, macro.content, isInline);
    }
}
