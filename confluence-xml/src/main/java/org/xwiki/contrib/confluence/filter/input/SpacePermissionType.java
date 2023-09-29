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
package org.xwiki.contrib.confluence.filter.input;

import java.util.EnumSet;

/**
 * For Confluence documentation on this:
 * @see https://confluence.atlassian.com/doc/assign-space-permissions-139460.html (screenshots)
 * Constant names where found there:
 * @see https://github.com/Rinaldi-James-Michael/Atlassian-Scripts-Public/blob/2ddbd967f2cfcb04e7f69223bc3060cdfe792b5e/
%F0%9F%93%B0%20Confluence%20Groovy%20scripts/User%20related%20-%20Groovy%20Scripts%20-%20Confluence/
1.%20Ultimate%20user%20creation%20script%20for%20Confluence%20(Server%20or%20Data%20Center).groovy
 *
 * Some fields are documented as "To be confirmed": these fields have been seen
 * in a Confluence database export but not (yet) in a xml export.
 * This means they exist for this type, but might never been actually seen.
 */

/**
 * The list of space permission types.
 * @since 9.24.0
 * @version $Id$
 */
public enum SpacePermissionType
{
    /**
     * View pages in the space.
     */
    VIEWSPACE,

    /**
     * Delete user's own content in the space.
     */
    REMOVEOWNCONTENT,

    /**
     * Add and/or edit pages in the space.
     */
    EDITSPACE,

    /**
     * Create a space.
     */
    CREATESPACE,

    /**
     * To be confirmed.
     */
    PERSONALSPACE,

    /**
     * Remove pages in the space.
     */
    REMOVEPAGE,

    /**
     * Add and/or edit blogs in the space.
     */
    EDITBLOG,

    /**
     * Remove blogs in the space.
     */
    REMOVEBLOG,

    /**
     * Add attachments to pages in the space.
     */
    CREATEATTACHMENT,

    /**
     * Delete attachments from pages in the Space.
     */
    REMOVEATTACHMENT,

    /**
     * Add comments to pages in the space.
     */
    COMMENT,

    /**
     * Remove comments from pages in the space.
     */
    REMOVECOMMENT,

    /**
     * Add or delete restrictions in pages in the space.
     */
    SETPAGEPERMISSIONS,

    /**
     * Export the space.
     */
    EXPORTSPACE,

    /**
     * Have administrate space permissions.
     */
    SETSPACEPERMISSIONS,

    /**
     * Export page.
     * NOTE(RJ): I haven't found any documentation on this right, but found it in a Confluence export.
     */
    EXPORTPAGE,

    /**
     * Archive page.
     * Seen in a blog export.
     */
    ARCHIVEPAGE,

    /**
     * To be confirmed.
     */
    ADMINISTRATECONFLUENCE,

    /**
     * To be confirmed.
     */
    PROFILEATTACHMENTS,

    /**
     * To be confirmed.
     */
    SYSTEMADMINISTRATOR,

    /**
     * To be confirmed.
     */
    UPDATEUSERSTATUS,

    /**
     * To be confirmed.
     */
    USECONFLUENCE,

    /**
     * Legacy permission.
     */
    REMOVEMAIL;

    /**
     * Default rights.
     */
    public static final EnumSet<SpacePermissionType> DEFAULT = EnumSet.of(
            EDITSPACE, VIEWSPACE, REMOVEOWNCONTENT, CREATEATTACHMENT, COMMENT);
}
