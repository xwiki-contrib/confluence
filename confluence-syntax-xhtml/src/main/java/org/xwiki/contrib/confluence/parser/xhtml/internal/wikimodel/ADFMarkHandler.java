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

import org.xwiki.rendering.wikimodel.WikiParameter;
import org.xwiki.rendering.wikimodel.xhtml.handler.TagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Handles ADFMark tags.
 * <p>
 * Example:
 * <p>
 * {@code <ac:image> <ac:adf-mark key="border" size="2" color="#091e4224" /> </ac:image> }
 *
 * @version $Id$
 * @since 9.29
 */
public class ADFMarkHandler extends TagHandler implements ConfluenceTagHandler
{
    /**
     * Default constructor.
     */
    public ADFMarkHandler()
    {
        super(false);
    }

    @Override
    protected void begin(TagContext context)
    {
        ConfluenceImageWikiReference image =
            (ConfluenceImageWikiReference) context.getTagStack().getStackParameter(CONFLUENCE_CONTAINER);
        WikiParameter keyParam = context.getParams().getParameter("key");
        if (image != null && keyParam != null && "border".equals(keyParam.getValue())) {
            image.getImageParameters().put("data-xwiki-image-style-border", "true");
        }
    }
}
