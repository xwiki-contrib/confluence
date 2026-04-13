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
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.contrib.usercommon.formatter.UserFormatterFactory;

/**
 * Convert username and group name from confluence to XWiki. This is taking care of the specificity of confluence, so
 * the default groups are automatically mapped and some cases for retro compatibility are also kept.
 *
 * @version $Id$
 * @since 9.92.1
 */
public class UserGroupConverter
{
    private static final String XWIKI_ADMIN_GROUP_NAME = "XWikiAdminGroup";

    private static final String XWIKI_ALL_GROUP_NAME = "XWikiAllGroup";

    private static final Pattern FORBIDDEN_USER_CHARACTERS = Pattern.compile("[. /]");

    private static final Mapping DEFAULT_GROUP_MAPPING = new Mapping(Map.of(
        "confluence-administrators", XWIKI_ADMIN_GROUP_NAME,
        "administrators", XWIKI_ADMIN_GROUP_NAME,
        "site-admins", XWIKI_ADMIN_GROUP_NAME,
        "system-administrators", XWIKI_ADMIN_GROUP_NAME,
        "confluence-users", XWIKI_ALL_GROUP_NAME,
        "users", XWIKI_ALL_GROUP_NAME,
        "_licensed-confluence", ""
    ));

    private final String userFormat;

    private final String groupFormat;

    private final Mapping userIdMapping;

    private final Mapping groupMapping;

    private final UserFormatterFactory userFormatterFactory;

    /**
     * Constructor.
     *
     * @param userFormatterFactory the user formatter factory to build the required formatters
     * @param userFormat the format for the user to apply
     * @param groupFormat the format for the group to apply
     * @param userIdMapping the user ID mapping (could be empty)
     * @param groupMapping the gorup mapping (could be empty)
     */
    public UserGroupConverter(UserFormatterFactory userFormatterFactory, String userFormat, String groupFormat,
        Mapping userIdMapping, Mapping groupMapping)
    {
        this.userFormatterFactory = userFormatterFactory;
        this.userFormat = userFormat;
        this.groupFormat = groupFormat;
        this.userIdMapping = userIdMapping;
        this.groupMapping = groupMapping;
        this.groupMapping.putAll(DEFAULT_GROUP_MAPPING);
    }

    /**
     * @param groupName the Confluence username
     * @return the corresponding XWiki username, without forbidden characters
     */
    public String toGroupReferenceName(String groupName)
    {
        if (groupMapping != null) {
            String group = groupMapping.get(groupName);
            if (group != null) {
                return group;
            }
        }

        if (StringUtils.isEmpty(groupFormat)) {
            return groupName;
        }

        return userFormatterFactory.create(Map.of("group", groupName)).format(groupFormat);
    }

    /**
     * @param userName the Confluence username
     * @return the corresponding XWiki username, without forbidden characters
     */
    public String toUserReferenceName(String userName)
    {
        // Apply the configured mapping
        if (userIdMapping != null) {
            String mappedName = userIdMapping.getOrDefault(userName, "").trim();
            if (!mappedName.isEmpty()) {
                return mappedName;
            }
        }

        // Translate the usual default admin user in Confluence to its XWiki counterpart
        if (userName.equals("admin")) {
            return "Admin";
        }

        // Apply the user format
        if (StringUtils.isEmpty(userFormat)) {
            // Do some minimal cleanup which is backward compatible with older versions of the filter.
            return FORBIDDEN_USER_CHARACTERS.matcher(userName).replaceAll("_");
        }

        return userFormatterFactory.create(Map.of("username", userName)).format(userFormat);
    }
}
