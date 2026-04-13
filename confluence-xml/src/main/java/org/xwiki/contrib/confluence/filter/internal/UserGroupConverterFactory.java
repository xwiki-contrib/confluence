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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.Mapping;
import org.xwiki.contrib.usercommon.formatter.UserFormatterFactory;

/**
 * Factory for the {@link UserGroupConverter}.
 *
 * @version $Id$
 * @since 9.92.1
 */
@Component(roles = UserGroupConverterFactory.class)
@Singleton
public class UserGroupConverterFactory
{
    @Inject
    private UserFormatterFactory userFormatterFactory;

    /**
     * Build a {@link  UserGroupConverter} based on the provided parameters.
     *
     * @param userFormat user format to apply.
     * @param groupFormat group format to apply.
     * @param userIdMapping a mapping for users.
     * @param groupMapping a specific mapping for groups. Default one are already taken into account.
     * @return a {@link  UserGroupConverter} object.
     */
    public UserGroupConverter create(String userFormat, String groupFormat,
        Mapping userIdMapping, Mapping groupMapping)
    {
        return new UserGroupConverter(userFormatterFactory, userFormat, groupFormat, userIdMapping, groupMapping);
    }
}
