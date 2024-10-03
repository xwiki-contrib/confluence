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
     * @param parentSpaceReference the confluence space in which the document lives
     * @param documentReference the document reference to convert
     */
    String convertDocumentReference(String parentSpaceReference, String documentReference);

    /**
     * @return the converted space reference, as SPACE
     * @param spaceReference the space reference to convert
     */
    String convertSpaceReference(String spaceReference);

    /**
     * @return the converted anchor
     * @param spaceKey the space key to use if targetting the home page, empty/null for the current space or if the
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
     * @param spaceReference the space reference to convert
     * @param asDocument whether the space reference should be the WebHome of this space (type DOCUMENT)
     * @since 9.47.0
     */
    default String convertSpaceReference(String spaceReference, boolean asDocument)
    {
        String ref = convertSpaceReference(spaceReference);
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
     */
    default ResourceReference convertURL(String url)
    {
        return new ResourceReference(url, ResourceType.URL);
    }
}
