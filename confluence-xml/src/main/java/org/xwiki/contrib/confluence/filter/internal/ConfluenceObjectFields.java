package org.xwiki.contrib.confluence.filter.internal;

import java.util.HashMap;

import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.DATE_VALUE;
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
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.LONG_VALUE;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.STRING_VALUE;
import static org.xwiki.contrib.confluence.filter.internal.XMLConfluenceObjectReader.KEY_NAME;

public class ConfluenceObjectFields extends HashMap<String, String>
{
    public static final ConfluenceObjectFields BODY_CONTENT = new ConfluenceObjectFields(
            "bodycontentid",
        KEY_PAGE_BODY_TYPE, "bodytypeid",
            KEY_CONTENT, "contentid",
            KEY_PAGE_BODY, "body"
    );

    public static final ConfluenceObjectFields USER_MAPPING = new ConfluenceObjectFields(
            "user_key",
            KEY_USER_NAME, "username");

    public static final ConfluenceObjectFields CONTENTPROPERTIES = new ConfluenceObjectFields(
        "propertyid",
        KEY_NAME, "propertyname",
        STRING_VALUE, "stringval",
        LONG_VALUE, "longval",
        DATE_VALUE, "dateval",
        "contentid", "contentid"
    );

    public static final ConfluenceObjectFields LABEL = new ConfluenceObjectFields(
            "labelid",
            KEY_NAME, "name",
            "namespace", "namespace",
            KEY_PAGE_CREATION_DATE, "creationdate",
            KEY_PAGE_REVISION_DATE, "lastmoddate"
    );

    public static final ConfluenceObjectFields CONTENT = new ConfluenceObjectFields(
            "contentid",
             "hibernateVersion", "hibernateversion",
             "contenttype", "contenttype",
             KEY_PAGE_TITLE, "title",
             "lowerTitle", "lowertitle",
             KEY_PAGE_REVISION, "version",
             KEY_PAGE_CREATION_AUTHOR_KEY, "creator",
             KEY_PAGE_CREATION_DATE, "creationdate",
             KEY_PAGE_REVISION_AUTHOR_KEY, "lastmodifier",
             KEY_PAGE_REVISION_DATE, "lastmoddate",
             KEY_PAGE_REVISION_COMMENT, "versioncomment",
             KEY_PAGE_ORIGINAL_VERSION, "prevver",
             KEY_PAGE_CONTENT_STATUS, "content_status",
             KEY_COMMENT_CONTAINERCONTENT, "pageid",
             KEY_PAGE_SPACE, "spaceid",
             KEY_PAGE_POSITION, "child_position",
             KEY_PAGE_PARENT, "parentid",
             // We have no proof the following column is used, but to be safe... (duplicate KEY_PAGE_PARENT intended)
             KEY_PAGE_PARENT, "parentcommentid",
             "navigationType", "navigationtype"
    );

    public static final ConfluenceObjectFields SPACES = new ConfluenceObjectFields(
            "spaceid",
            KEY_SPACE_NAME, "spacename",
            KEY_SPACE_KEY, "spacekey",
            "lowerKey", "lowerspacekey",
            KEY_SPACE_DESCRIPTION, "spacedescid",
            KEY_SPACE_HOMEPAGE, "homepage",
            KEY_PAGE_CREATION_AUTHOR_KEY, "creator",
            KEY_PAGE_CREATION_DATE, "creationdate",
            KEY_PAGE_REVISION_AUTHOR_KEY, "lastmodifier",
            KEY_PAGE_REVISION_DATE, "lastmoddate",
            "spaceType", "spacetype",
            KEY_SPACE_STATUS, "spacestatus"
    );

    public static final ConfluenceObjectFields PAGETEMPLATES = new ConfluenceObjectFields(
          "templateid",
             "hibernateVersion", "hibernateversion",
            KEY_SPACE_NAME, "templatename",
            KEY_SPACE_DESCRIPTION, "templatedesc",
            KEY_CONTENT, "content",
            KEY_PAGE_SPACE, "spaceid",
            KEY_PAGE_ORIGINAL_VERSION, "prevver",
            KEY_PAGE_REVISION, "version",
            KEY_PAGE_CREATION_AUTHOR_KEY, "creator",
            KEY_PAGE_CREATION_DATE, "creationdate",
            KEY_PAGE_REVISION_AUTHOR_KEY, "lastmodifier",
            KEY_PAGE_REVISION_DATE, "lastmoddate",
            KEY_PAGE_BODY_TYPE, "bodytypeid"
    );

    public static final ConfluenceObjectFields SPACEPERMISSIONS = new ConfluenceObjectFields(
        "permid",
            KEY_SPACE_PERMISSION_SPACE, "spaceid",
            KEY_PERMISSION_TYPE, "permtype",
            KEY_CONTENTPERMISSION_GROUP, "permgroupname",
            KEY_SPACEPERMISSION_GROUP, "externalgroupid",
            KEY_SPACEPERMISSION_USERNAME, "permusername",
            KEY_PERMISSION_ALLUSERSSUBJECT, "permalluserssubject",
            KEY_PAGE_CREATION_AUTHOR_KEY, "creator",
            KEY_PAGE_CREATION_DATE, "creationdate",
            KEY_PAGE_REVISION_AUTHOR_KEY, "lastmodifier",
            KEY_PAGE_REVISION_DATE, "lastmoddate",
            KEY_ACTIVE, "active"

    );
    public static final ConfluenceObjectFields CONTENT_PERM = new ConfluenceObjectFields(
            "id",
            KEY_PERMISSION_TYPE, "cp_type",
            KEY_SPACEPERMISSION_USERNAME, "username",
            KEY_CONTENTPERMISSION_GROUP, "groupname",
            KEY_SPACEPERMISSION_GROUP, "external_group_id",
            KEY_CONTENT_PERMISSION_OWNING_SET, "cps_id",
            KEY_PAGE_CREATION_AUTHOR_KEY, "creator",
            KEY_PAGE_CREATION_DATE, "creationdate",
            KEY_PAGE_REVISION_AUTHOR_KEY, "lastmodifier",
            KEY_PAGE_REVISION_DATE, "lastmoddate"
    );
    public static final ConfluenceObjectFields CONTENT_PERM_SET = new ConfluenceObjectFields(
            "id",
            KEY_PERMISSION_TYPE, "cont_perm_type",
            KEY_CONTENT_PERMISSION_SET_OWNING_CONTENT, "content_id",
            KEY_PAGE_CREATION_DATE, "creationdate",
            KEY_PAGE_REVISION_DATE, "lastmoddate"
    );

    public static final ConfluenceObjectFields CONTENT_LABEL = new ConfluenceObjectFields(
            "id",
            KEY_LABELLING_LABEL, "labelid",
            KEY_LABELLING_CONTENT, "contentid",
            "pageTemplate", "pagetemplateid",
            KEY_LABEL_OWNINGUSER, "owner",
            KEY_PAGE_CREATION_DATE, "creationdate",
            KEY_PAGE_REVISION_DATE, "lastmoddate"
    );

    private final String csvFieldId;

    ConfluenceObjectFields(String csvFieldId, String... xmlAndCsvFieldPairs)
    {
        this.csvFieldId = csvFieldId;

        for (int i = 0; i < xmlAndCsvFieldPairs.length; i += 2) {
            put(xmlAndCsvFieldPairs[i], xmlAndCsvFieldPairs[i + 1]);
        }
    }

    public String getCSVFieldId()
    {
        return csvFieldId;
    }
}
