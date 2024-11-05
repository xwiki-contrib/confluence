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

import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.wikimodel.WikiReference;

/**
 * a WikiReference coming from the Confluence XHTML syntax. The reference is to an entity in Confluence and will need to
 * be converted to a reference pointing to something in XWiki, following the nested import.
 * @version $Id$
 * @since 9.54.0
 */
public class ConfluenceXHTMLWikiReference extends WikiReference
    implements UserContainer, AttachmentContainer, PageContainer, SpaceContainer, LabelContainer
{
    private String anchor;

    private ConfluenceXHTMLAttachment attachment;

    private String page;

    private String space;

    private String user;

    private String label;

    private XDOM labelXDOM;

    /**
     * Default constructor.
     */
    public ConfluenceXHTMLWikiReference()
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
    public ConfluenceXHTMLAttachment getAttachment()
    {
        return this.attachment;
    }

    @Override
    public void setAttachment(ConfluenceXHTMLAttachment attachment)
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
    public void setSpaceKey(String spaceKey)
    {
        this.space = spaceKey;
    }

    /**
     * @return the document.
     */
    public String getPage()
    {
        return this.page;
    }

    @Override
    public void setPageTitle(String pageTitle)
    {
        this.page = pageTitle;
    }

    /**
     * @return the label.
     */
    public XDOM getLabelXDOM()
    {
        return this.labelXDOM;
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

    @Override
    public void setLabel(XDOM label)
    {
        this.labelXDOM = label;
    }
}
