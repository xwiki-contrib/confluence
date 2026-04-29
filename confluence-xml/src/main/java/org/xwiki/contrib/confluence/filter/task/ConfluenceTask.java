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
package org.xwiki.contrib.confluence.filter.task;

import java.util.Date;

/**
 * Represents a Confluence task, with all its data.
 * Note: null can be returned from any of those methods if the data is unknown.
 * @since 9.94.0
 * @version $Id$
 */
public interface ConfluenceTask
{
    /**
     * @return the creator's user key
     */
    String getCreatorUserKey();

    /**
     * @return the assignee's user key
     */
    String getAssigneeUserKey();

    /**
     * @return the key of the user who completed this task
     */
    String getCompleteUserKey();

    /**
     * @return the task's creation date
     */
    Date getCreateDate();

    /**
     * @return the task's update date
     */
    Date getUpdateDate();

    /**
     * @return the task's due date
     */
    Date getDueDate();

    /**
     * @return the task's creation date
     */
    Date getCompleteDate();

    /**
     * @return the id of the space containing this task
     */
    long getSpaceId();

    /**
     * @return the id of the content containing this task
     */
    long getContentId();

    /**
     * @return the page title of the content containing this task
     */
    String getPageTitle();

    /**
     * @return the global id of this task
     */
    long getGlobalId();

    /**
     * @return the id of this task
     */
    long getId();

    /**
     * @return whether this task is completed (status = "CHECKED")
     */
    boolean isCompleted();

    /**
     * @return whether the content is visible
     */
    boolean isContentVisible();

    /**
     * @return whether the space is visible
     */
    boolean isSpaceVisible();

    /**
     * @return whether the Confluence XHTML code of the body of this task
     */
    String getBody();
}
