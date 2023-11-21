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

import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.AttachmentTagHandler.ConfluenceAttachment;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.wikimodel.WikiReference;

import java.util.HashMap;
import java.util.Map;

/**
 * @version $Id$
 * @since 9.0
 */
public class ConfluenceImageWikiReference extends WikiReference implements AttachmentContainer, URLContainer
{
    private ConfluenceAttachment attachment;

    private String url;

    private XDOM caption;

    private final Map<String, String> imageParameters = new HashMap<>();

    public ConfluenceImageWikiReference()
    {
        super("");
    }

    public ConfluenceAttachment getAttachment()
    {
        return this.attachment;
    }

    @Override
    public void setAttachment(ConfluenceAttachment attachment)
    {
        this.attachment = attachment;
    }

    public String getURL()
    {
        return this.url;
    }

    @Override
    public void setURL(String url)
    {
        this.url = url;
    }

    /**
     * @since 9.29.0
     */
    public XDOM getCaption()
    {
        return caption;
    }

    /**
     * @since 9.29.0
     */
    public void setCaption(XDOM caption)
    {
        this.caption = caption;
    }

    /**
     * @since 9.29.0
     */
    public Map<String, String> getImageParameters()
    {
        return imageParameters;
    }
}
