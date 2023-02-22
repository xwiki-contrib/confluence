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
import org.xwiki.rendering.wikimodel.xhtml.handler.TagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Handles users.
 * <p>
 * Example:
 * <p>
 * {@code
 * <ri:user ri:username="admin" />
 * <ri:user ri:userkey="admin" />
 * }
 *
 * @version $Id$
 * @since 9.0
 */
public class UserTagHandler extends TagHandler implements ConfluenceTagHandler
{
    public UserTagHandler()
    {
        super(false);
    }

    @Override
    protected void begin(TagContext context)
    {
        Object container = context.getTagStack().getStackParameter(CONFLUENCE_CONTAINER);

        // Name based user reference
        WikiParameter usernameParameter = context.getParams().getParameter("ri:username");

        // Key based user reference
        WikiParameter userkeyParameter = context.getParams().getParameter("ri:userkey");
        if (userkeyParameter == null) {
            userkeyParameter = context.getParams().getParameter("ri:account-id");
        }

        if (container instanceof UserContainer) {
            ConfluenceMacro macro = new ConfluenceMacro();
            UserContainer userContainer = (UserContainer) container;

            macro.name = "mention";

            if (usernameParameter != null) {
                macro.parameters = macro.parameters.setParameter("reference", usernameParameter.getValue());
                userContainer.setUser(usernameParameter.getValue());
            } else if (userkeyParameter != null) {
                macro.parameters = macro.parameters.setParameter("reference", userkeyParameter.getValue());
                userContainer.setUser(userkeyParameter.getValue());
            }
            context.getScannerContext().onMacroInline(macro.name, macro.parameters, macro.content);
        } else if (container instanceof ConfluenceMacro) {
            ConfluenceMacro macro = (ConfluenceMacro) container;            
            if (usernameParameter != null) {
                macro.parameters = macro.parameters.setParameter(".user", usernameParameter.getValue());
            } else if (userkeyParameter != null) {
                macro.parameters = macro.parameters.setParameter(".user", userkeyParameter.getValue());
            }
        }
    }
}
