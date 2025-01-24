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
package org.xwiki.contrib.confluence.parser.xhtml;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

/**
 * Converts references from Confluence to XWiki, using the parameters set for the conversion.
 * @version $Id$
 * @since 9.29.0
 */
public interface ConfluenceReferenceConverter
{
    /**
     * @return the converted user reference
     * @param userId the user identifier to convert
     */
    String convertUserReference(String userId);

    /**
     * @return the converted document reference
     * @param spaceKey the confluence space in which the document lives
     * @param pageTitle the page title to convert
     */
    String convertDocumentReference(String spaceKey, String pageTitle);

    /**
     * @return the converted document reference
     * @param pageId the page id to convert
     */
    default String convertDocumentReference(long pageId)
    {
        return "confluencePage:id:" + pageId;
    }

    /**
     * @return the converted space reference, as SPACE (serialized)
     * @param spaceKey the space reference to convert
     */
    String convertSpaceReference(String spaceKey);

    /**
     * @return the converted attachment reference, as ATTACHMENT (serialized)
     * @param spaceKey the confluence space in which the document lives
     * @param pageTitle the document reference to convert
     * @param filename the attachment filename
     * @since 9.63.0
     */
    default String convertAttachmentReference(String spaceKey, String pageTitle, String filename)
    {
        String docRef = convertDocumentReference(spaceKey, pageTitle);
        if (StringUtils.isEmpty(docRef)) {
            return escapeAtAndHash(filename);
        }

        return docRef + '@' + escapeAtAndHash(filename);
    }

    /**
     * @return the converted anchor
     * @param spaceKey the space key to use if targeting the home page, empty/null for the current space or if the
     *                 page title is provided
     * @param pageTitle the page title, or empty/null for the current page
     * @param anchor the anchor to convert
     * @since 9.53.0
     */
    default String convertAnchor(String spaceKey, String pageTitle, String anchor)
    {
        return anchor;
    }

    /**
     * @return the converted space reference
     * @param spaceKey the space reference to convert
     * @param asDocument whether the space reference should be the WebHome of this space (type DOCUMENT)
     * @since 9.47.0
     */
    default String convertSpaceReference(String spaceKey, boolean asDocument)
    {
        String ref = convertSpaceReference(spaceKey);
        if (asDocument && ref != null && !ref.isEmpty()) {
            return ref + ".WebHome";
        }
        return ref;
    }

    /**
     * @param url the URL to convert
     * @return the URL converted to a resource reference (that can link to a document, attachment or the URL itself if
     *         it doesn't need conversion or cannot be converted
     * @since 9.56.0
     * @deprecated since 9.76.0
     * use #getSpaceKey()
     */
    @Deprecated(since = "9.76.0")
    default ResourceReference convertURL(String url)
    {
        return new ResourceReference(url, ResourceType.URL);
    }

    /**
     * @return the resource reference of the given element.
     * @param pageId the page id of or in which to return the reference
     * @param filename the optional attachment filename in the specified page (can be null or empty)
     * @param anchor the optional anchor (can be null or empty)
     * @since 9.66.0
     */
    default ResourceReference getResourceReference(long pageId, String filename, String anchor)
    {
        String base = "id:" + pageId;
        String attachmentPart = StringUtils.isEmpty(filename) ? "" : '@' + escapeHash(filename);
        String anchorPart = StringUtils.isEmpty(anchor) ? "" : '#' + anchor;
        ResourceType resourceType = StringUtils.isEmpty(filename)
            ? getConfluencePageResourceType()
            : getConfluenceAttachResourceType();
        return new ResourceReference(base + attachmentPart + anchorPart, resourceType);
    }

    /**
     * @return the resource reference of the given element.
     * @param spaceKey the optional key of the wanted Confluence space
     * @param pageTitle the optional title of the wanted page
     * @param filename the optional attachment filename in the specified page (can be null or empty)
     * @param anchor the optional anchor (can be null or empty)
     * @since 9.66.0
     */
    default ResourceReference getResourceReference(String spaceKey, String pageTitle, String filename, String anchor)
    {
        String attachmentPart = StringUtils.isEmpty(filename) ? "" : '@' + escapeAtAndHash(filename);
        String anchorPart = StringUtils.isEmpty(anchor) ? "" : '#' + anchor;

        ResourceType resourceType;
        String base;
        if (StringUtils.isEmpty(pageTitle)) {
            if (StringUtils.isEmpty(spaceKey)) {
                return getResourceReference(filename, anchor);
            }
            if (StringUtils.isEmpty(filename)) {
                resourceType = new ResourceType("confluenceSpace");
                base = spaceKey;
            } else {
                resourceType = getConfluenceAttachResourceType();
                base = "spaceHome:" + spaceKey;
            }
        } else {
            resourceType = StringUtils.isEmpty(filename)
                ? getConfluencePageResourceType()
                : getConfluenceAttachResourceType();
            base = "page:" + (StringUtils.isEmpty(spaceKey) ? "@self" : spaceKey) + '.' + escapeAtAndHash(pageTitle);
        }
        return new ResourceReference(base + attachmentPart + anchorPart, resourceType);
    }

    private static ResourceType getConfluenceAttachResourceType()
    {
        return new ResourceType("confluenceAttach");
    }

    private static ResourceType getConfluencePageResourceType()
    {
        return new ResourceType("confluencePage");
    }

    private static ResourceReference getResourceReference(String filename, String anchor)
    {
        if (StringUtils.isEmpty(filename)) {
            if (StringUtils.isEmpty(anchor)) {
                // This is a link to the current document.
                return new DocumentResourceReference("");
            }
            DocumentResourceReference docRef = new DocumentResourceReference("");
            docRef.setAnchor(anchor);
            return docRef;
        }
        AttachmentResourceReference attachmentResourceReference = new AttachmentResourceReference(
            escapeAtAndHash(filename));
        if (StringUtils.isNotEmpty(anchor)) {
            attachmentResourceReference.setAnchor(anchor);
        }
        return attachmentResourceReference;
    }

    private static String escapeAtAndHash(String s)
    {
        return escapeHash(s).replace("@", "\\@");
    }

    private static String escapeHash(String s)
    {
        return s.replace("\\", "\\\\").replace("#", "\\#");
    }
}
