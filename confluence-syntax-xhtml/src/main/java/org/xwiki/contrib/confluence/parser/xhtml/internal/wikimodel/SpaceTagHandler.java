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
import org.xwiki.rendering.wikimodel.WikiParameter;
import org.xwiki.rendering.wikimodel.xhtml.handler.TagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Handles spaces.
 * Preceding whitespaces are handled by adding ri:space to EMPTYVISIBLE_ELEMENTS in ConfluenceXHTMLWhitespaceXMLFilter.
 * <p>
 * Example:
 * <p>
 * {@code
 * <ri:space ri:space-key="ds" />
 * }
 *
 * @version $Id$
 * @since 9.0
 */
public class SpaceTagHandler extends TagHandler implements ConfluenceTagHandler
{
    private final ConfluenceReferenceConverter referenceConverter;

    /**
     * Default constructor.
     * @param referenceConverter the reference converter to use (can be null)
     */
    public SpaceTagHandler(ConfluenceReferenceConverter referenceConverter)
    {
        super(false);
        this.referenceConverter = referenceConverter;
    }

    @Override
    protected void begin(TagContext context)
    {
        WikiParameter spaceParameter = context.getParams().getParameter("ri:space-key");
        if (spaceParameter == null) {
            return;
        }

        String space = spaceParameter.getValue();
        if (referenceConverter != null) {
            space = referenceConverter.convertSpaceReference(space);
        }

        if (context.getTagStack().getStackParameter(AbstractMacroParameterTagHandler.IN_CONFLUENCE_PARAMETER) != null) {
            // We are in a confluence macro parameter, we store the space in it.
            TagContext parentContext = context.getParentContext();
            String parameterContent = parentContext.getContent();
            if (parameterContent != null && !parameterContent.isEmpty()) {
                parentContext.appendContent(",");
            }
            parentContext.appendContent(space);
            return;
        }

        Object container = context.getTagStack().getStackParameter(CONFLUENCE_CONTAINER);
        if (container instanceof SpaceContainer) {
            ((SpaceContainer) container).setSpace(space);
        }
    }
}
