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
package org.xwiki.contrib.confluence.parser.confluence.internal.wikimodel;

import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

/**
 * a ResourceReference coming from the Confluence syntax. The reference is to an entity in Confluence and will need to
 * be converted to a reference pointing to something in XWiki, following the nested import.
 * @version $Id$
 * @since 9.54.0
 */
public class ConfluenceResourceReference extends ResourceReference
{
    // FIXME somewhat duplicate of ConfluenceXHTMLWikiReference
    private String anchor;

    private String filename;

    private String pageTitle;

    private String spaceKey;

    private String user;

    /**
     * @param reference see {@link #getReference()}
     * @param type      see {@link #getType()}
     */
    public ConfluenceResourceReference(String reference, ResourceType type)
    {
        super(reference, type);
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
    public String getFilename()
    {
        return this.filename;
    }

    /**
     * @param filename the attachment to set
     */
    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    /**
     * @return the user.
     */
    public String getUser()
    {
        return this.user;
    }

    /**
     * @param user the user to set.
     */
    public void setUser(String user)
    {
        this.user = user;
    }

    /**
     * @return the space.
     */
    public String getSpaceKey()
    {
        return this.spaceKey;
    }

    /**
     * @param spaceKey the space to set
     */
    public void setSpaceKey(String spaceKey)
    {
        this.spaceKey = spaceKey;
    }

    /**
     * @return the page.
     */
    public String getPageTitle()
    {
        return this.pageTitle;
    }

    /**
     * @param pageTitle the page to set
     */
    public void setPageTitle(String pageTitle)
    {
        this.pageTitle = pageTitle;
    }
}
