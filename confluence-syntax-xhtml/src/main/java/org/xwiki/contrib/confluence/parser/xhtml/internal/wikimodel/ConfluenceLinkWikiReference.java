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
import org.xwiki.rendering.wikimodel.WikiReference;

/**
 * @version $Id$
 * @since 9.0
 */
public class ConfluenceLinkWikiReference extends WikiReference
    implements UserContainer, AttachmentContainer, PageContainer, SpaceContainer, LabelContainer
{
    private String anchor;

    private ConfluenceAttachment attachment;

    private String document;

    private String space;

    private String user;

    private String label;

    /**
     * Default constructor.
     */
    public ConfluenceLinkWikiReference()
    {
        super("");
    }

    /**
     * @return the link's anchor.
     */
    public String getAnchor()
    {
        return this.anchor;
    }

    /**
     * Set the link's anchor.
     * @param anchor the anchor to set
     */
    public void setAnchor(String anchor)
    {
        this.anchor = anchor;
    }

    /**
     * @return the attachment.
     */
    public ConfluenceAttachment getAttachment()
    {
        return this.attachment;
    }

    @Override
    public void setAttachment(ConfluenceAttachment attachment)
    {
        this.attachment = attachment;
    }

    /**
     * @return the user.
     */
    public String getUser()
    {
        return this.user;
    }

    @Override
    public void setUser(String user)
    {
        this.user = user;
    }

    /**
     * @return the space.
     */
    public String getSpace()
    {
        return this.space;
    }

    @Override
    public void setSpace(String space)
    {
        this.space = space;
    }

    /**
     * @return the document.
     */
    public String getDocument()
    {
        return this.document;
    }

    @Override
    public void setDocument(String document)
    {
        this.document = document;
    }

    @Override
    public String getLabel()
    {
        return this.label;
    }

    @Override
    public void setLabel(String label)
    {
        this.label = label;
    }
}
