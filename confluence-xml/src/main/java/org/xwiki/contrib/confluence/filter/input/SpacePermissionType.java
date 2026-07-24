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

import org.xwiki.security.authorization.Right;

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
    VIEWSPACE(Right.VIEW),

    /**
     * Delete user's own content in the space.
     */
    REMOVEOWNCONTENT(Right.ILLEGAL),

    /**
     * Delete own content.
     * Seen in a Confluence Cloud CSV export.
     */
    DELETE_OWN_COMMENT(Right.ILLEGAL),

    /**
     * Delete space.
     * Seen in a Confluence Cloud CSV export.
     */
    DELETE_SPACE(Right.DELETE),

    /**
     * Edit native content.
     * Seen in a Confluence Cloud CSV export.
     * NOTE: could not find a corresponding checkbox in Confluence's UI, nor
     *       any documentation on this permission.
     */
    EDIT_NATIVE_CONTENT(Right.EDIT),

    /**
     * Export content.
     * Seen in a Confluence Cloud CSV export.
     */
    EXPORT_CONTENT(Right.ILLEGAL),

    /**
     * Guest user management.
     * Seen in a Confluence Cloud CSV export.
     */
    GUEST_USER_MANAGEMENT(Right.ILLEGAL),

    /**
     * Manage content.
     * Seen in a Confluence Cloud CSV export.
     * NOTE: could not find a corresponding checkbox in Confluence's UI, nor
     *       any documentation on this permission.
     */
    MANAGE_CONTENT(Right.EDIT, Right.DELETE),

    /**
     * Manage look and feel.
     * Seen in a Confluence Cloud CSV export.
     */
    MANAGE_LOOK_AND_FEEL(Right.ILLEGAL),

    /**
     * Manage public links.
     * Seen in a Confluence Cloud CSV export.
     */
    MANAGE_PUBLIC_LINKS(Right.ILLEGAL),

    /**
     * Non-licensed user management.
     * Seen in a Confluence Cloud CSV export.
     */
    NONLICENSED_USER_MANAGEMENT(Right.ILLEGAL),

    /**
     * Add and/or edit pages in the space.
     */
    EDITSPACE(Right.EDIT),

    /**
     * Create a space.
     */
    CREATESPACE(null),

    /**
     * To be confirmed.
     */
    PERSONALSPACE(null),

    /**
     * Remove pages in the space.
     */
    REMOVEPAGE(Right.DELETE),

    /**
     * Add attachments to pages in the space.
     */
    CREATEATTACHMENT(Right.ILLEGAL),

    /**
     * Delete attachments from pages in the Space.
     */
    REMOVEATTACHMENT(Right.ILLEGAL),

    /**
     * Add comments to pages in the space.
     */
    COMMENT(Right.COMMENT),

    /**
     * Remove comments from pages in the space.
     */
    REMOVECOMMENT(Right.ILLEGAL),

    /**
     * Add or delete restrictions in pages in the space.
     */
    SETPAGEPERMISSIONS(Right.ADMIN),

    /**
     * Export the space.
     */
    EXPORTSPACE(Right.ILLEGAL),

    /**
     * Have administrate space permissions.
     */
    SETSPACEPERMISSIONS(Right.ADMIN),

    /**
     * Export page.
     * NOTE(RJ): I haven't found any documentation on this right, but found it in a Confluence export.
     */
    EXPORTPAGE(Right.ILLEGAL),

    /**
     * Archive page.
     * Seen in a blog export.
     */
    ARCHIVEPAGE(Right.ILLEGAL),

    /**
     * Archive space.
     * Seen in a Confluence Cloud CSV export
      */
    ARCHIVE_SPACE(Right.ILLEGAL),

    /**
     * Access analytics.
     * Seen in a Confluence Cloud CSV export
     */
    ACCESSANALYTICS(Right.ILLEGAL),

    /**
     * Create blog.
     * Seen in a Confluence Cloud CSV export
     */
    CREATE_BLOG(Right.ILLEGAL),

    /**
     * To be confirmed.
     */
    ADMINISTRATECONFLUENCE(Right.ADMIN),

    /**
     * To be confirmed.
     */
    PROFILEATTACHMENTS(Right.ILLEGAL),

    /**
     * To be confirmed.
     */
    SYSTEMADMINISTRATOR(Right.ADMIN),

    /**
     * To be confirmed.
     */
    UPDATEUSERSTATUS(Right.ILLEGAL),

    /**
     * To be confirmed.
     */
    USECONFLUENCE(Right.ILLEGAL),

    /**
     * User management.
     * Seen in Confluence Cloud CSV export.
     */
    USER_MANAGEMENT(Right.ADMIN),

    /**
     * Legacy permission.
     */
    REMOVEMAIL(Right.ILLEGAL);

    /**
     * Default rights.
     */
    public static final EnumSet<SpacePermissionType> DEFAULT = EnumSet.of(
            EDITSPACE, VIEWSPACE, REMOVEOWNCONTENT, CREATEATTACHMENT, COMMENT);

    private final Right[] convertedTo;

    SpacePermissionType(Right... convertedTo)
    {
        this.convertedTo = convertedTo;
    }

    /**
     * @return the corresponding XWiki right. Right.ILLEGAL means the permission should be converted to nothing.
     *         null means the conversion should raise a warning.
     * @since 9.60
     * @deprecated since 9.96.0
     */
    @Deprecated(since = "9.96.0")
    public Right toXWikiRight()
    {
        return this.convertedTo[0];
    }

    public Right[] toXWikiRights() { return this.convertedTo; }
}
