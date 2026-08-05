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
package org.xwiki.contrib.confluence.filter.internal;

import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_DATE_VALUE;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_ACTIVE;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_COMMENT_CONTAINERCONTENT;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_CONTENT;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_CONTENTPERMISSION_GROUP;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_CONTENT_PERMISSION_OWNING_SET;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_CONTENT_PERMISSION_SET_OWNING_CONTENT;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_LABELLING_CONTENT;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_LABELLING_LABEL;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_LABEL_OWNINGUSER;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_PAGE_BODY;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_PAGE_BODY_TYPE;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_PAGE_CONTENT_STATUS;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_PAGE_CREATION_AUTHOR_KEY;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_PAGE_CREATION_DATE;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_PAGE_ORIGINAL_VERSION;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_PAGE_PARENT;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_PAGE_POSITION;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_PAGE_REVISION;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_PAGE_REVISION_AUTHOR_KEY;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_PAGE_REVISION_COMMENT;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_PAGE_REVISION_DATE;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_PAGE_SPACE;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_PAGE_TITLE;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_PERMISSION_ALLUSERSSUBJECT;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_PERMISSION_TYPE;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_SPACEPERMISSION_GROUP;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_SPACEPERMISSION_USERNAME;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_SPACE_DESCRIPTION;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_SPACE_HOMEPAGE;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_SPACE_KEY;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_SPACE_NAME;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_SPACE_PERMISSION_SPACE;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_SPACE_STATUS;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_USER_NAME;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_LONG_VALUE;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_STRING_VALUE;
import static org.xwiki.contrib.confluence.filter.internal.ConfluenceXMLStreamReader.KEY_NAME;

/**
 * The tables found in Confluence CSV exports. Enum value names are the table names, and a field mapping (that maps from
 * the csv field name to the field name used internally in ConfluenceXMLPackage - mostly the ones from the XML format)
 * is attached to each table.
 * @since 9.96.0
 * @version $Id$
 */
public enum ConfluenceCSVTable
{
    /**
     * bodycontent table.
     */
    bodycontent(new ConfluenceCSVFieldMapping(
        CSVField.BODYCONTENTID,
        KEY_PAGE_BODY_TYPE, CSVField.BODYTYPEID,
        KEY_CONTENT, CSVField.CONTENTID,
        KEY_PAGE_BODY, CSVField.BODY
    )),

     /**
     * user_mapping table.
     */
    user_mapping(new ConfluenceCSVFieldMapping(
        CSVField.USER_KEY,
        KEY_USER_NAME, CSVField.USERNAME
    )),

     /**
     * contentproperties table.
     */
    contentproperties(new ConfluenceCSVFieldMapping(
        CSVField.PROPERTYID,
        KEY_NAME, CSVField.PROPERTYNAME,
            KEY_STRING_VALUE, CSVField.STRINGVAL,
            KEY_LONG_VALUE, CSVField.LONGVAL,
            KEY_DATE_VALUE, CSVField.DATEVAL,
        CSVField.CONTENTID, CSVField.CONTENTID
    )),

     /**
     * label table.
     */
    label(new ConfluenceCSVFieldMapping(
        CSVField.LABELID,
        KEY_NAME, CSVField.NAME,
        CSVField.NAMESPACE, CSVField.NAMESPACE,
        KEY_PAGE_CREATION_DATE, CSVField.CREATIONDATE,
        KEY_PAGE_REVISION_DATE, CSVField.LASTMODDATE
    )),

     /**
     * content table.
     */
    content(new ConfluenceCSVFieldMapping(
        CSVField.CONTENTID,
        UnusedXMLField.HIBERNATE_VERSION, CSVField.HIBERNATEVERSION,
        CSVField.CONTENTTYPE, CSVField.CONTENTTYPE,
        KEY_PAGE_TITLE, CSVField.TITLE,
        UnusedXMLField.LOWER_TITLE, CSVField.LOWERTITLE,
        KEY_PAGE_REVISION, CSVField.VERSION,
        KEY_PAGE_CREATION_AUTHOR_KEY, CSVField.CREATOR,
        KEY_PAGE_CREATION_DATE, CSVField.CREATIONDATE,
        KEY_PAGE_REVISION_AUTHOR_KEY, CSVField.LASTMODIFIER,
        KEY_PAGE_REVISION_DATE, CSVField.LASTMODDATE,
        KEY_PAGE_REVISION_COMMENT, CSVField.VERSIONCOMMENT,
        KEY_PAGE_ORIGINAL_VERSION, CSVField.PREVVER,
        KEY_PAGE_CONTENT_STATUS, CSVField.CONTENT_STATUS,
        KEY_COMMENT_CONTAINERCONTENT, CSVField.PAGEID,
        KEY_PAGE_SPACE, CSVField.SPACEID,
        KEY_PAGE_POSITION, CSVField.CHILD_POSITION,
        KEY_PAGE_PARENT, CSVField.PARENTID,
        // We have no proof the following column is used, but to be safe... (duplicate KEY_PAGE_PARENT intended)
        KEY_PAGE_PARENT, CSVField.PARENTCOMMENTID,
        UnusedXMLField.NAVIGATION_TYPE, CSVField.NAVIGATIONTYPE
    )),

     /**
     * spaces table.
     */
    spaces(new ConfluenceCSVFieldMapping(
        CSVField.SPACEID,
        KEY_SPACE_NAME, CSVField.SPACENAME,
        KEY_SPACE_KEY, CSVField.SPACEKEY,
        UnusedXMLField.LOWER_KEY, CSVField.LOWERSPACEKEY,
        KEY_SPACE_DESCRIPTION, CSVField.SPACEDESCID,
        KEY_SPACE_HOMEPAGE, CSVField.HOMEPAGE,
        KEY_PAGE_CREATION_AUTHOR_KEY, CSVField.CREATOR,
        KEY_PAGE_CREATION_DATE, CSVField.CREATIONDATE,
        KEY_PAGE_REVISION_AUTHOR_KEY, CSVField.LASTMODIFIER,
        KEY_PAGE_REVISION_DATE, CSVField.LASTMODDATE,
        UnusedXMLField.SPACE_TYPE, CSVField.SPACETYPE,
        KEY_SPACE_STATUS, CSVField.SPACESTATUS
    )),

     /**
     * pagetemplates table.
     */
    pagetemplates(new ConfluenceCSVFieldMapping(
        CSVField.TEMPLATEID,
         UnusedXMLField.HIBERNATE_VERSION, CSVField.HIBERNATEVERSION,
        KEY_SPACE_NAME, CSVField.TEMPLATENAME,
        KEY_SPACE_DESCRIPTION, CSVField.TEMPLATEDESC,
        KEY_CONTENT, CSVField.CONTENT,
        KEY_PAGE_SPACE, CSVField.SPACEID,
        KEY_PAGE_ORIGINAL_VERSION, CSVField.PREVVER,
        KEY_PAGE_REVISION, CSVField.VERSION,
        KEY_PAGE_CREATION_AUTHOR_KEY, CSVField.CREATOR,
        KEY_PAGE_CREATION_DATE, CSVField.CREATIONDATE,
        KEY_PAGE_REVISION_AUTHOR_KEY, CSVField.LASTMODIFIER,
        KEY_PAGE_REVISION_DATE, CSVField.LASTMODDATE,
        KEY_PAGE_BODY_TYPE, CSVField.BODYTYPEID
    )),

     /**
     * spacepermissions table.
     */
    spacepermissions(new ConfluenceCSVFieldMapping(
        CSVField.PERMID,
        KEY_SPACE_PERMISSION_SPACE, CSVField.SPACEID,
        KEY_PERMISSION_TYPE, CSVField.PERMTYPE,
        KEY_CONTENTPERMISSION_GROUP, CSVField.PERMGROUPNAME,
        KEY_SPACEPERMISSION_GROUP, CSVField.EXTERNALGROUPID,
        KEY_SPACEPERMISSION_USERNAME, CSVField.PERMUSERNAME,
        KEY_PERMISSION_ALLUSERSSUBJECT, CSVField.PERMALLUSERSSUBJECT,
        KEY_PAGE_CREATION_AUTHOR_KEY, CSVField.CREATOR,
        KEY_PAGE_CREATION_DATE, CSVField.CREATIONDATE,
        KEY_PAGE_REVISION_AUTHOR_KEY, CSVField.LASTMODIFIER,
        KEY_PAGE_REVISION_DATE, CSVField.LASTMODDATE,
        KEY_ACTIVE, CSVField.ACTIVE
    )),

     /**
     * content_perm table.
     */
    content_perm(new ConfluenceCSVFieldMapping(
        CSVField.ID,
        KEY_PERMISSION_TYPE, CSVField.CP_TYPE,
        KEY_SPACEPERMISSION_USERNAME, CSVField.USERNAME,
        KEY_CONTENTPERMISSION_GROUP, CSVField.GROUPNAME,
        KEY_SPACEPERMISSION_GROUP, CSVField.EXTERNAL_GROUP_ID,
        KEY_CONTENT_PERMISSION_OWNING_SET, CSVField.CPS_ID,
        KEY_PAGE_CREATION_AUTHOR_KEY, CSVField.CREATOR,
        KEY_PAGE_CREATION_DATE, CSVField.CREATIONDATE,
        KEY_PAGE_REVISION_AUTHOR_KEY, CSVField.LASTMODIFIER,
        KEY_PAGE_REVISION_DATE, CSVField.LASTMODDATE
    )),

     /**
     * content_perm_set table.
     */
    content_perm_set(new ConfluenceCSVFieldMapping(
        CSVField.ID,
        KEY_PERMISSION_TYPE, CSVField.CONT_PERM_TYPE,
        KEY_CONTENT_PERMISSION_SET_OWNING_CONTENT, CSVField.CONTENT_ID,
        KEY_PAGE_CREATION_DATE, CSVField.CREATIONDATE,
        KEY_PAGE_REVISION_DATE, CSVField.LASTMODDATE
    )),

     /**
     * content_label table.
     */
    content_label(new ConfluenceCSVFieldMapping(
        CSVField.ID,
        KEY_LABELLING_LABEL, CSVField.LABELID,
        KEY_LABELLING_CONTENT, CSVField.CONTENTID,
        UnusedXMLField.PAGE_TEMPLATE, CSVField.PAGETEMPLATEID,
        KEY_LABEL_OWNINGUSER, CSVField.OWNER,
        KEY_PAGE_CREATION_DATE, CSVField.CREATIONDATE,
        KEY_PAGE_REVISION_DATE, CSVField.LASTMODDATE
    ));

    /**
     * The field mapping.
     */
    public final ConfluenceCSVFieldMapping fieldMapping;

    ConfluenceCSVTable(ConfluenceCSVFieldMapping fieldMapping)
    {
        this.fieldMapping = fieldMapping;
    }

    private static final class CSVField
    {
        static final String BODYTYPEID = "bodytypeid";
        static final String BODYCONTENTID = "bodycontentid";
        static final String CONTENTID = "contentid";
        static final String BODY = "body";
        static final String USER_KEY = "user_key";
        static final String USERNAME = "username";
        static final String PROPERTYID = "propertyid";
        static final String PROPERTYNAME = "propertyname";
        static final String STRINGVAL = "stringval";
        static final String LONGVAL = "longval";
        static final String DATEVAL = "dateval";
        static final String LABELID = "labelid";
        static final String NAME = "name";
        static final String NAMESPACE = "namespace";
        static final String CREATIONDATE = "creationdate";
        static final String LASTMODDATE = "lastmoddate";
        static final String HIBERNATEVERSION = "hibernateversion";
        static final String CONTENTTYPE = "contenttype";
        static final String LOWERTITLE = "lowertitle";
        static final String VERSION = "version";
        static final String TITLE = "title";
        static final String CREATOR = "creator";
        static final String LASTMODIFIER = "lastmodifier";
        static final String VERSIONCOMMENT = "versioncomment";
        static final String PREVVER = "prevver";
        static final String CONTENT_STATUS = "content_status";
        static final String PAGEID = "pageid";
        static final String SPACEID = "spaceid";
        static final String CHILD_POSITION = "child_position";
        static final String PARENTID = "parentid";
        static final String PARENTCOMMENTID = "parentcommentid";
        static final String NAVIGATIONTYPE = "navigationtype";
        static final String SPACENAME = "spacename";
        static final String SPACEKEY = "spacekey";
        static final String LOWERSPACEKEY = "lowerspacekey";
        static final String SPACEDESCID = "spacedescid";
        static final String HOMEPAGE = "homepage";
        static final String SPACETYPE = "spacetype";
        static final String SPACESTATUS = "spacestatus";
        static final String TEMPLATEID = "templateid";
        static final String TEMPLATENAME = "templatename";
        static final String TEMPLATEDESC = "templatedesc";
        static final String CONTENT = "content";
        static final String PERMID = "permid";
        static final String PERMTYPE = "permtype";
        static final String PERMGROUPNAME = "permgroupname";
        static final String EXTERNALGROUPID = "externalgroupid";
        static final String PERMUSERNAME = "permusername";
        static final String PERMALLUSERSSUBJECT = "permalluserssubject";
        static final String ACTIVE = "active";
        static final String ID = "id";
        static final String CP_TYPE = "cp_type";
        static final String GROUPNAME = "groupname";
        static final String EXTERNAL_GROUP_ID = "external_group_id";
        static final String CPS_ID = "cps_id";
        static final String CONT_PERM_TYPE = "cont_perm_type";
        static final String CONTENT_ID = "content_id";
        static final String PAGETEMPLATEID = "pagetemplateid";
        static final String OWNER = "owner";
    }

    // We map some CSV fields to the corresponding fields founds in the XML exports, but we don't actually use them yet.
    // Should this happen, we shall probably define a propert KEY_XXX constant in ConfluenceXMLPackage.
    private static final class UnusedXMLField
    {
        static final String NAVIGATION_TYPE = "navigationType";
        static final String HIBERNATE_VERSION = "hibernateVersion";
        static final String LOWER_TITLE = "lowerTitle";
        static final String LOWER_KEY = "lowerKey";
        static final String PAGE_TEMPLATE = "pageTemplate";
        static final String SPACE_TYPE = "spaceType";
    }
}
