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

import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Handles adf attributes.
 * @version $Id$
 * @since 9.25.0
 */
public class ADFAttributeHandler extends AbstractMacroParameterTagHandler
{
    protected static final String ADF_ATTRIBUTE_NAME = "adf-attribute-name";

    @Override
    protected void begin(TagContext context)
    {
        super.begin(context);
        String attrName = context.getParams().getParameter("key").getValue();
        context.getTagStack().pushStackParameter(ADF_ATTRIBUTE_NAME, attrName);
    }

    @Override
    protected void setParameter(MacroTagHandler.ConfluenceMacro macro, TagContext context)
    {
        String nodeType = macro.name.substring(ADFNodeHandler.CONFLUENCE_ADF_MACRO_PREFIX.length());
        String attrName = (String) context.getTagStack().popStackParameter(ADF_ATTRIBUTE_NAME);
        if (attrName.startsWith(nodeType + "-")) {
            attrName = attrName.substring(nodeType.length() + 1);
        }
        String value = getContent(context);
        if ("panel".equals(nodeType) && "type".equals(attrName) && "note".equals(value)) {
            // panels of type note are notes. We already have a {{note}} macro.
            macro.name = value;
        } else {
            macro.parameters = macro.parameters.setParameter(attrName, value);
        }
    }
}
