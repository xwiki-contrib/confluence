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

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceReferenceConverter;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.UserResourceReference;

/**
 * Confluence Filter Reference Converter.
 * @since 9.89.0
 * @version $Id$
 */
@Role
public interface ConfluenceFilterReferenceConverter extends ConfluenceReferenceConverter
{
    /**
     * @param name the name to validate
     * @return the validated name
     */
    String toEntityName(String name);

    /**
     * @param groupName the Confluence username
     * @return the corresponding XWiki username, without forbidden characters
     */
    String toGroupReferenceName(String groupName);

    /**
     * @param userName the Confluence username
     * @return the corresponding XWiki username, without forbidden characters
     */
    String toUserReferenceName(String userName);

    /**
     * @param userName the Confluence username
     * @return the corresponding XWiki user reference
     */
    String toUserReference(String userName);

    /**
     * @param groupName the Confluence username
     * @return the corresponding XWiki user reference
     */
    String toGroupReference(String groupName);

    /**
     * @return the serialized guest user
     */
    String getGuestUser();

    /**
     * @param reference the reference of a user that can be either a username or a user key.
     * @return a XWiki user reference.
     */
    ResourceReference resolveUserReference(UserResourceReference reference);

    /**
     * Convert an external group ID to a XWiki reference.
     * @param groupId confluence external group ID
     * @return serialized XWiki group reference
     */
    String convertGroupId(String groupId);

    /**
     * Converts a page ID into a XWiki reference.
     *
     * @param pageId confluence if of the page
     * @param asSpace if you want the reference as a space
     * @return a valid XWiki reference or null
     */
    EntityReference convertDocumentReference(long pageId, boolean asSpace);
}
