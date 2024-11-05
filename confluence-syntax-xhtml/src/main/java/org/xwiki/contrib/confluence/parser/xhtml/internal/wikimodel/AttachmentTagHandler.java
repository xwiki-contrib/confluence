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
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.MacroTagHandler.ConfluenceMacro;
import org.xwiki.rendering.wikimodel.WikiParameter;
import org.xwiki.rendering.wikimodel.xhtml.handler.TagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Handles images.
 * <p>
 * Example:
 * <p>
 * {@code
 * <ri:attachment ri:filename="file.png"/>
 * <ri:attachment ri:filename="file.png"><ri:page ri:content-title="xhtml" ri:space-key="SPACE" /></ri:attachment>
 * }
 *
 * @version $Id$
 * @since 9.0
 */
public class AttachmentTagHandler extends TagHandler implements ConfluenceTagHandler
{
    private final ConfluenceReferenceConverter referenceConverter;

    /**
     * Default constructor.
     * @param referenceConverter the Confluence reference converter used to create attachment references, when needed
     */
    public AttachmentTagHandler(ConfluenceReferenceConverter referenceConverter)
    {
        super(false);
        this.referenceConverter = referenceConverter;
    }

    @Override
    protected void begin(TagContext context)
    {
        ConfluenceXHTMLAttachment attachment = new ConfluenceXHTMLAttachment();

        WikiParameter filenameParameter = context.getParams().getParameter("ri:filename");

        if (filenameParameter != null) {
            attachment.filename = filenameParameter.getValue();
        }

        context.getTagStack().pushStackParameter(CONFLUENCE_CONTAINER, attachment);
    }

    @Override
    protected void end(TagContext context)
    {
        ConfluenceXHTMLAttachment attachment =
            (ConfluenceXHTMLAttachment) context.getTagStack().popStackParameter(CONFLUENCE_CONTAINER);

        Object container = context.getTagStack().getStackParameter(CONFLUENCE_CONTAINER);

        if (container instanceof AttachmentContainer) {
            ((AttachmentContainer) container).setAttachment(attachment);
        }

        // set attachment filename as macro parameter if the attachment tag is part of a macro
        if (container instanceof ConfluenceMacro) {
            ConfluenceMacro macro = (ConfluenceMacro) container;
            macro.parameters = macro.parameters.setParameter(
                "att--filename",
                (referenceConverter == null || StringUtils.isEmpty(attachment.pageTitle))
                    ? attachment.filename
                    : referenceConverter.convertAttachmentReference(
                        attachment.spaceKey,
                        attachment.pageTitle,
                        attachment.filename
                    )
            );
        }
    }
}
