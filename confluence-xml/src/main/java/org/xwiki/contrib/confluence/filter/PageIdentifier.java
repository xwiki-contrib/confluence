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
package org.xwiki.contrib.confluence.filter;

/**
 * Stores different information about the filtering of a page.
 *
 * @version $Id$
 * @since 9.22.0
 */
public class PageIdentifier
{
    private Long pageId;

    private String pageTitle;

    private String parentTitle;

    private String spaceTitle;

    private String pageRevision;

    /**
     * @param pageId see {@link #getPageId()}
     */
    public PageIdentifier(Long pageId)
    {
        this.pageId = pageId;
    }

    /**
     * @return the id of the page
     */
    public Long getPageId()
    {
        return pageId;
    }

    /**
     * @param pageId see {@link #getPageId()}
     */
    public void setPageId(Long pageId)
    {
        this.pageId = pageId;
    }

    /**
     * @return the title of the page
     */
    public String getPageTitle()
    {
        return pageTitle;
    }

    /**
     * @param pageTitle see {@link #getPageTitle()}
     */
    public void setPageTitle(String pageTitle)
    {
        this.pageTitle = pageTitle;
    }

    /**
     * @return the title of the parent of the page
     */
    public String getParentTitle()
    {
        return parentTitle;
    }

    /**
     * @param parentTitle see {@link #getParentTitle()}
     */
    public void setParentTitle(String parentTitle)
    {
        this.parentTitle = parentTitle;
    }

    /**
     * @return the name of the space where the page belongs
     */
    public String getSpaceTitle()
    {
        return spaceTitle;
    }

    /**
     * @param spaceTitle see {@link #getSpaceTitle()}
     */
    public void setSpaceTitle(String spaceTitle)
    {
        this.spaceTitle = spaceTitle;
    }

    /**
     * @return the revision of the page
     */
    public String getPageRevision()
    {
        return pageRevision;
    }

    /**
     * @param pageRevision see {@link #getPageRevision()}
     */
    public void setPageRevision(String pageRevision)
    {
        this.pageRevision = pageRevision;
    }

    @Override
    public String toString()
    {
        return '{' + "pageId=" + pageId + ", pageTitle='" + pageTitle + '\'' + ", parentTitle='" + parentTitle + '\''
            + ", spaceTitle='" + spaceTitle + '\'' + ", pageRevision='" + pageRevision + '\'' + '}';
    }
}
