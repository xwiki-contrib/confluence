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

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage;
import org.xwiki.contrib.confluence.filter.task.ConfluenceTask;
import org.xwiki.environment.Environment;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.DefaultFileInputSource;
import org.xwiki.test.XWikiTempDirUtil;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ComponentTest
class TasksTest
{
    @InjectMockComponents
    private ConfluenceXMLPackage confluencePackage;

    @BeforeEach
    void setup(MockitoComponentManager componentManager) throws Exception
    {
        Environment environment = componentManager.registerMockComponent(Environment.class);
        when(environment.getTemporaryDirectory()).thenReturn(XWikiTempDirUtil.createTemporaryDirectory());

        // We want a fresh, blank component for each test
        confluencePackage = componentManager.getInstance(ConfluenceXMLPackage.class);
    }

    @Test
    void testPackage1() throws FilterException, ParseException
    {
        URL p = getClass().getClassLoader().getResource("taskscsv/package1");
        assert p != null;
        confluencePackage.setSource(new DefaultFileInputSource(new File(p.getFile())));
        // non-existing page
        assertNull(confluencePackage.getTask(42, 1));
        // existing page, non-existing task
        assertNull(confluencePackage.getTask(534478858, 1));
        assertNull(confluencePackage.getTask(534478858, 1));

        assertFullyDetailedTask();
        assertNotSoDetailedTask();
    }

    private void assertFullyDetailedTask() throws FilterException, ParseException
    {
        SimpleDateFormat f = new SimpleDateFormat(ConfluenceXMLPackage.DATE_FORMAT);
        ConfluenceTask task = confluencePackage.getTask(534478858, 5);
        assertEquals("6321bed3051efc6985671eed", task.getAssigneeUserKey());
        assertEquals("<span class=\"placeholder-inline-tasks\">Some action that is already done! <ac:link><ri:user ri:account-id=\"6321bed3051efc6985671eed\" /></ac:link> <time datetime=\"2022-09-27\" /> </span>", task.getBody());
        assertEquals("6321bed3051efc6985671e42", task.getCompleteUserKey());
        assertEquals("4221bed3051efc6985671eed", task.getCreatorUserKey());
        assertEquals(f.parse("2022-09-28 10:05:45.51"), task.getCompleteDate());
        assertEquals(534937605, task.getSpaceId());
        assertEquals(534478858, task.getContentId());
        assertEquals("Some nice page", task.getPageTitle());
        assertEquals(60, task.getGlobalId());
        assertEquals(5, task.getId());
        assertEquals(f.parse("2022-09-27 14:11:34.292"), task.getCreateDate());
        assertEquals(f.parse("2022-09-27 00:00:00.0"), task.getDueDate());
        assertEquals(f.parse("2022-09-28 10:05:42.402"), task.getUpdateDate());
        assertTrue(task.isCompleted());
        assertTrue(task.isContentVisible());
        assertTrue(task.isSpaceVisible());
    }

    private void assertNotSoDetailedTask() throws FilterException
    {
        ConfluenceTask task = confluencePackage.getTask(534478858, 29);
        assertNull(task.getUpdateDate());
        assertNull(task.getDueDate());
        assertNull(task.getCompleteDate());
        assertNull(task.getAssigneeUserKey());
        assertNull(task.getCompleteUserKey());
        assertEquals("", task.getBody());
        assertFalse(task.isCompleted());
    }

    @Test
    void tryingToReadTasksWhenFileIsNotPresentIsFine() throws FilterException, ConfigurationException, IOException
    {
        URL p = getClass().getClassLoader().getResource("confluencexml/emptypage");
        assert p != null;
        confluencePackage.setSource(new DefaultFileInputSource(new File(p.getFile())));
        confluencePackage.readTasks();
        assertNull(confluencePackage.getTask(42, 1));
    }
}
