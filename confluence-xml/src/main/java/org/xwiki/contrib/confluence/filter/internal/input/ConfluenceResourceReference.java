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
package org.xwiki.contrib.confluence.filter.internal.input;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

class ConfluenceResourceReference extends ResourceReference
{
    private static final ResourceType CONFLUENCE_PAGE = new ResourceType("confluencePage");
    private static final ResourceType CONFLUENCE_SPACE = new ResourceType("confluenceSpace");
    private static final ResourceType CONFLUENCE_ATTACH = new ResourceType("confluenceAttach");

    private final String spaceKey;
    private final long pageId;
    private final String pageTitle;
    private final String anchor;
    private final String attachmentFilename;
    private final boolean isHomePage;

    ConfluenceResourceReference(String spaceKey, String pageTitle, String attachmentFilename, String anchor,
        boolean isHomePage)
    {
        super(null, ResourceType.UNKNOWN);
        this.spaceKey = spaceKey;
        this.pageId = -1;
        this.pageTitle = pageTitle;
        this.anchor = anchor;
        this.attachmentFilename = attachmentFilename;
        this.isHomePage = isHomePage;
        setType();
        setReference();
    }

    ConfluenceResourceReference(long pageId, String attachmentFilename, String anchor)
    {
        super(null, ResourceType.UNKNOWN);
        this.spaceKey = null;
        this.pageTitle = null;
        this.pageId = pageId;
        this.anchor = anchor;
        this.attachmentFilename = attachmentFilename;
        this.isHomePage = false;
        setType();
        setReference();
    }

    private void setType()
    {
        if (StringUtils.isEmpty(attachmentFilename)) {
            if (StringUtils.isEmpty(pageTitle) && !isHomePage && pageId == -1) {
                setType(CONFLUENCE_SPACE);
            } else {
                setType(CONFLUENCE_PAGE);
            }
        } else {
            setType(CONFLUENCE_ATTACH);
        }
    }

    private static String escapeAtAndDash(String pageTitle)
    {
        return escapeDash(pageTitle).replace("@", "\\@");
    }

    private static String escapeDash(String s)
    {
        return s.replace("\\", "\\\\").replace("#", "\\#");
    }

    private void setReference()
    {
        String baseRef = getBaseReference();
        if (StringUtils.isBlank(anchor)) {
            setReference(baseRef);
        } else {
            setReference(baseRef + '#' + anchor);
        }
    }

    private String getBaseReference()
    {
        if (StringUtils.isEmpty(attachmentFilename)) {
            setType(CONFLUENCE_ATTACH);
            return getPageOrSpaceReference(false);
        }
        return getPageOrSpaceReference(true) + '@' + escapeDash(attachmentFilename);
    }

    private String getPageOrSpaceReference(boolean isAttachment)
    {
        if (StringUtils.isNotEmpty(pageTitle)) {
            maybeSetPageType(isAttachment);
            return "page:" + spaceKey + '.' + escapeAtAndDash(pageTitle);
        }

        if (pageId != -1) {
            maybeSetPageType(isAttachment);
            return "id:" + pageId;
        }

        if (spaceKey != null) {
            return getSpaceReference(isAttachment);
        }

        return "";
    }

    private String getSpaceReference(boolean isAttachment)
    {
        if (isAttachment || isHomePage) {
            maybeSetPageType(isAttachment);
            return "spaceHome:" + spaceKey;
        }

        setType(CONFLUENCE_SPACE);
        return spaceKey;
    }

    private void maybeSetPageType(boolean isAttachment)
    {
        if (!isAttachment) {
            setType(CONFLUENCE_PAGE);
        }
    }
}
