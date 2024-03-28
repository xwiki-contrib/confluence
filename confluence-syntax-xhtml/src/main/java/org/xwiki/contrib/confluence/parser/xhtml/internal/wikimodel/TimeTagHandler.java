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

import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceMacroSupport;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Handles time tags.
 * <p>
 * Example:
 * <p>
 * {@code
 * <time datetime="2020-01-12" />
 * }
 *
 * @version $Id$
 * @since 9.5
 */
public class TimeTagHandler extends MacroTagHandler
{
    private static final String DATETIME_PARAMETER = "datetime";

    /**
     * Default constructor.
     *
     * @param macroSupport macro support.
     */
    public TimeTagHandler(ConfluenceMacroSupport macroSupport)
    {
        super(macroSupport);
    }

    @Override
    protected void begin(TagContext context)
    {
        ConfluenceMacro macro = new ConfluenceMacro();

        macro.name = context.getName();
        String dateTime = context.getParams().getParameter(DATETIME_PARAMETER).getValue();
        macro.parameters = macro.parameters.setParameter(DATETIME_PARAMETER, dateTime);
        context.getTagStack().pushStackParameter(CONFLUENCE_CONTAINER, macro);
    }

    @Override
    protected void end(TagContext context)
    {
        ConfluenceMacro macro = (ConfluenceMacro) context.getTagStack().popStackParameter(CONFLUENCE_CONTAINER);

        context.getScannerContext().onMacroInline(macro.name, macro.parameters, macro.content);
    }
}
