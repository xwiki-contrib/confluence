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
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceReferenceConverter;
import org.xwiki.rendering.wikimodel.WikiParameter;
import org.xwiki.rendering.wikimodel.xhtml.handler.TagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Handles links.
 * <p>
 * Example:
 * <p>
 * {@code
 * <ac:link ac:anchor="anchor">
 *   <ri:page ri:content-title="Page" ri:space-key="SPACE" />
 *   <ac:plain-text-link-body><![CDATA[label]] ></ac:plain-text-link-body>
 * </ac:link>
 * <ac:link ac:anchor="anchor">
 *   <ri:attachment ri:filename="file.png">
 *     <ri:page ri:content-title="xhtml" ri:space-key="SPACE" />
 *   </ri:attachment>
 *   <ac:plain-text-link-body><![CDATA[image1234.png]]></ac:plain-text-link-body>
 * </ac:link>
 * <ac:link ac:card-appearance="inline">
 *   <ri:page ri:content-title="Page with / in title" ri:version-at-save="1" />
 *   <ac:link-body>Page with / in title</ac:link-body>
 * </ac:link>
 * <ac:link ac:anchor="anchor">
 *   <ri:space ri:space-key="ds" />
 * </ac:link>
 * <ac:link>
 *   <ri:user ri:username="admin" />
 * </ac:link>
 * }
 *
 * @version $Id$
 * @since 9.0
 */
public class LinkTagHandler extends TagHandler implements ConfluenceTagHandler
{
    private final ConfluenceReferenceConverter referenceConverter;

    /**
     * @param referenceConverter the reference converter to use (can be null)
     */
    public LinkTagHandler(ConfluenceReferenceConverter referenceConverter)
    {
        super(false);
        this.referenceConverter = referenceConverter;
    }

    @Override
    protected void begin(TagContext context)
    {
        ConfluenceLinkWikiReference link = new ConfluenceLinkWikiReference();

        WikiParameter anchorParameter = context.getParams().getParameter("ac:anchor");

        if (anchorParameter != null) {
            link.setAnchor(anchorParameter.getValue());
        }

        context.getTagStack().pushStackParameter(CONFLUENCE_CONTAINER, link);
    }

    @Override
    protected void end(TagContext context)
    {
        ConfluenceLinkWikiReference link =
            (ConfluenceLinkWikiReference) context.getTagStack().popStackParameter(CONFLUENCE_CONTAINER);

        // If a user tag was inside the link tag, it was transformed into a mention macro.
        if (link.getUser() != null) {
            return;
        }
        // Make sure to have a label for local anchors
        if (link.getLabelXDOM() == null && link.getDocument() == null
            && link.getSpace() == null && link.getUser() == null && link.getAttachment() == null) {
            if (StringUtils.isEmpty(link.getAnchor())) {
                // Skip empty links.
                return;
            }
            link.setLabel(link.getAnchor());
        }

        if (context.getTagStack().getStackParameter(AbstractMacroParameterTagHandler.IN_CONFLUENCE_PARAMETER) != null) {
            // We are in a confluence macro parameter, we put the link in the content instead of issuing a reference.
            String ref;
            if (referenceConverter == null) {
                ref = link.getDocument();
                String space = link.getSpace();
                if (space != null && !space.isEmpty()) {
                    ref = space + "." + ref;
                }
            } else {
                ref = referenceConverter.convertDocumentReference(link.getSpace(), link.getDocument());
            }
            context.getParentContext().appendContent(ref);
        } else {
            context.getScannerContext().onReference(link);
        }
    }
}
