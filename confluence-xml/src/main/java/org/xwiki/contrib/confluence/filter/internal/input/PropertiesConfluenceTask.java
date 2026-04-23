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
import org.xwiki.contrib.confluence.filter.input.ConfluenceProperties;
import org.xwiki.contrib.confluence.filter.task.ConfluenceTask;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.DATE_FORMAT;

/**
 * a Confluence task backed by a Confluence property.
 *
 * @since 9.94.0
 * @version $Id$
 */
public class PropertiesConfluenceTask implements ConfluenceTask
{
    private final ConfluenceProperties r;

    /**
     * @param r the csv record of this task
     */
    public PropertiesConfluenceTask(ConfluenceProperties r)
    {
        this.r = r;
    }

    private Date getDate(String field)
    {

        String d = r.getString(field);
        if (StringUtils.isEmpty(d)) {
            return null;
        }

        try {
            return new SimpleDateFormat(DATE_FORMAT).parse(d);
        } catch (ParseException e) {
            // FIXME: maybe log something here?
        }

        return null;
    }

    private long getLong(String field)
    {
        String id = r.getString(field);
        if (StringUtils.isEmpty(id)) {
            return -1;
        }
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean getBool(String field)
    {
        return "true".equals(r.getString(field));
    }

    private String getString(String field)
    {
        String v = r.getString(field);
        return StringUtils.isEmpty(v) ? null : v;
    }

    @Override
    public String getCreatorUserKey()
    {
        return getString("CREATOR_USER_KEY");
    }

    @Override
    public String getAssigneeUserKey()
    {
        return getString("ASSIGNEE_USER_KEY");
    }

    @Override
    public String getCompleteUserKey()
    {
        return getString("COMPLETE_USER_KEY");
    }

    @Override
    public Date getCreateDate()
    {
        return getDate("CREATE_DATE");
    }

    @Override
    public Date getUpdateDate()
    {
        return getDate("UPDATE_DATE");
    }

    @Override
    public Date getDueDate()
    {
        return getDate("DUE_DATE");
    }

    @Override
    public Date getCompleteDate()
    {
        return getDate("COMPLETE_DATE");
    }
    @Override
    public long getContentId()
    {
        return getLong("CONTENT_ID");
    }

    @Override
    public long getGlobalId()
    {
        return getLong("GLOBAL_ID");
    }

    @Override
    public long getId()
    {
        return getLong("ID");
    }

    @Override
    public long getSpaceId()
    {
        return getLong("SPACE_ID");
    }

    @Override
    public boolean isCompleted()
    {
        return "CHECKED".equals(r.getString("TASK_STATUS"));
    }

    @Override
    public boolean isContentVisible()
    {
        return getBool("CONTENT_VISIBLE");
    }

    @Override
    public boolean isSpaceVisible()
    {
        return getBool("SPACE_VISIBLE");
    }

    @Override
    public String getPageTitle()
    {
        return getString("PAGE_TITLE");
    }

    @Override
    public String getBody()
    {
        return r.getString("BODY");
    }
}
