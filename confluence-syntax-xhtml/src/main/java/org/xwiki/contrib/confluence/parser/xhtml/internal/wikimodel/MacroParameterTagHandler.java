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

import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.MacroTagHandler.ConfluenceMacro;
import org.xwiki.rendering.wikimodel.WikiParameter;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Handles parameters.
 * <p>
 * Example:
 * <p>
 * {@code
 * <ac:parameter ac:name="title">State default macro</ac:parameter>
 * <ac:parameter ac:name="users">
 *   <ri:user ri:username="UserName" />
 *   <ri:user ri:userkey="adasd123213asdas" />
 * </ac:parameter>
 * }
 *
 * @version $Id$
 * @since 9.0
 */
public class MacroParameterTagHandler extends AbstractMacroParameterTagHandler implements ConfluenceTagHandler
{
    @Override
    protected void setParameter(ConfluenceMacro macro, TagContext context)
    {
        WikiParameter nameParameter = context.getParams().getParameter("ac:name");

        if (nameParameter != null) {
            String value = getContent(context);

            if (!value.trim().isEmpty()) {
                macro.parameters = macro.parameters.setParameter(nameParameter.getValue(), value);
            }
        }
    }
}
