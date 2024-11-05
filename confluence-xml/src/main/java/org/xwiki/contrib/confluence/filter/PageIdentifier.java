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

import java.util.Map;
import java.util.TreeMap;

/**
 * Stores different information about the filtering of a page.
 *
 * @version $Id$
 * @since 9.22.0
 */
public class PageIdentifier
{
    private static final String PAGE_ID = "pageId";
    private static final String PAGE_TITLE = "pageTitle";
    private static final String PARENT_TITLE = "parentTitle";
    private static final String SPACE_KEY = "spaceKey";
    private static final String PAGE_REVISION = "pageRevision";

    private Map<String, Object> m = new TreeMap<>();

    /**
     * @param pageId see {@link #getPageId()}
     */
    public PageIdentifier(Long pageId)
    {
        setPageId(pageId);
    }

    /**
     * @return the id of the page
     */
    public Long getPageId()
    {
        return (Long) m.get(PAGE_ID);
    }

    /**
     * @param pageId see {@link #getPageId()}
     */
    public void setPageId(Long pageId)
    {
        m.put(PAGE_ID, pageId);
    }

    /**
     * @return the title of the page
     */
    public String getPageTitle()
    {
        return (String) m.get(PAGE_TITLE);
    }

    /**
     * @param pageTitle see {@link #getPageTitle()}
     */
    public void setPageTitle(String pageTitle)
    {
        m.put(PAGE_TITLE, pageTitle);
    }

    /**
     * @return the title of the parent of the page
     */
    public String getParentTitle()
    {
        return (String) m.get(PARENT_TITLE);
    }

    /**
     * @param parentTitle see {@link #getParentTitle()}
     */
    public void setParentTitle(String parentTitle)
    {
        m.put(PARENT_TITLE, parentTitle);
    }

    /**
     * @return the name of the space where the page belongs
     * @deprecated since 9.63.0
     * use #getSpaceKey()
     */
    @Deprecated
    public String getSpaceTitle()
    {
        return getSpaceKey();
    }

    /**
     * @return the name of the space where the page belongs
     * @since 9.63.0
     */
    public String getSpaceKey()
    {
        return (String) m.get(SPACE_KEY);
    }

    /**
     * @param spaceKey see {@link #getSpaceTitle()}
     * @deprecated since 9.63.0
     * use setSpaceKey()
     */
    public void setSpaceTitle(String spaceKey)
    {
        setSpaceKey(spaceKey);
    }

    /**
     * @param spaceKey see {@link #getSpaceKey()}
     * @since 9.63.0
     */
    public void setSpaceKey(String spaceKey)
    {
        m.put(SPACE_KEY, spaceKey);
    }

    /**
     * @return the revision of the page
     */
    public String getPageRevision()
    {
        return (String) m.get(PAGE_REVISION);
    }

    /**
     * @param pageRevision see {@link #getPageRevision()}
     */
    public void setPageRevision(String pageRevision)
    {
        m.put(PAGE_REVISION, pageRevision);
    }

    /**
     * @param parentId set the parent id
     * @since 9.63.0
     */
    public void setParentId(Long parentId)
    {
        m.put("parentId", parentId);
    }

    /**
     * @param originalVersion set the original version
     * @since 9.63.0
     */
    public void setOriginalVersion(Long originalVersion)
    {
        m.put("originalVersion", originalVersion);
    }

    @Override
    public String toString()
    {
        return m.toString();
    }

    /**
     * @return a map representing this page identifier
     * @since 9.63.0
     */
    public Map<String, Object> getMap()
    {
        return m;
    }
}
