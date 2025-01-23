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

import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceReferenceConverter;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.MacroTagHandler.ConfluenceMacro;
import org.xwiki.rendering.wikimodel.WikiParameter;
import org.xwiki.rendering.wikimodel.xhtml.handler.TagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Handles users.
 * Preceding whitespaces are handled by adding ri:user to EMPTYVISIBLE_ELEMENTS in ConfluenceXHTMLWhitespaceXMLFilter.
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
    private static final String REFERENCE = "reference";

    private final ConfluenceReferenceConverter referenceConverter;

    /**
     * @param referenceConverter the reference converter to use (can be null).
     */
    public UserTagHandler(ConfluenceReferenceConverter referenceConverter)
    {
        super(false);
        this.referenceConverter = referenceConverter;
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
                macro.parameters = macro.parameters.setParameter(REFERENCE, usernameParameter.getValue());
                userContainer.setUser(usernameParameter.getValue());
            } else if (userkeyParameter != null) {
                macro.parameters = macro.parameters.setParameter(REFERENCE, userkeyParameter.getValue());
                userContainer.setUser(userkeyParameter.getValue());
            }
            context.getScannerContext().onMacroInline(macro.name, macro.parameters, macro.content);
        } else if (container instanceof ConfluenceMacro) {
            ConfluenceMacro macro = (ConfluenceMacro) container;
            WikiParameter macroParam = context.getParentContext().getParams().getParameter("ac:name");
            String paramKey = macroParam != null ? macroParam.getValue() : ".user";
            WikiParameter currentMacroParam = macro.parameters.getParameter(paramKey);
            String currentParamValue = currentMacroParam != null
                ? currentMacroParam.getValue() + ","
                : "";
            if (usernameParameter != null) {
                String usernameParameterValue = usernameParameter.getValue();
                // Manage a really specific use case for show-if/hide-if macro, related to:
                // https://github.com/xwikisas/application-confluence-migrator-pro/blob/a3e57c93ae47b85ce81cd279857b6177937031e7/application-confluence-migrator-pro-converters/src/main/java/com/xwiki/confluencepro/converters/internal/ShowIfHideIfMacroConverter.java#L82
                // In the old release of the confluence macro instead of having a specific parameter for the special
                // value of the user, they used the user parameter to match by example all users with the value
                // @authenticated. We need to avoid any transformation of this value, so we are able to map correctly
                // the parameter in the macro converter.
                // Note, if at some point another macro need some customization like this, it would be good to provide
                // in the converter a way to access to the raw parameter, and so we can manage this in a clean way.
                if (("show-if".equals(macro.name) || "hide-if".equals(macro.name))
                    && "user".equals(paramKey)
                    && ("@authenticated".equalsIgnoreCase(usernameParameterValue)
                        || "@anonymous".equalsIgnoreCase(usernameParameterValue)
                        || "@self".equalsIgnoreCase(usernameParameterValue))) {
                    macro.parameters = macro.parameters.setParameter(paramKey,
                        currentParamValue + usernameParameterValue);
                } else {
                    macro.parameters = macro.parameters.setParameter(paramKey,
                        currentParamValue + referenceConverter.convertUserReference(usernameParameterValue));
                }
            } else if (userkeyParameter != null) {
                String user = userkeyParameter.getValue();
                macro.parameters = macro.parameters.setParameter(paramKey,
                    currentParamValue + referenceConverter.convertUserReference(user));
            }
        }
    }
}
