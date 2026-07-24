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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileSystem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.confluence.filter.internal.ConfluenceCSVFile;
import org.xwiki.contrib.confluence.filter.internal.ConfluenceConsumer;
import org.xwiki.contrib.confluence.filter.internal.ConfluenceObjectFields;
import org.xwiki.contrib.confluence.filter.internal.ConfluenceObjectReader;
import org.xwiki.contrib.confluence.filter.internal.WithoutControlCharactersReader;
import org.xwiki.contrib.confluence.filter.internal.XMLConfluenceObjectReader;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceCanceledException;
import org.xwiki.contrib.confluence.filter.internal.input.PropertiesConfluenceTask;
import org.xwiki.contrib.confluence.filter.task.ConfluenceTask;
import org.xwiki.environment.Environment;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.FileInputSource;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.InputStreamInputSource;
import org.xwiki.filter.input.URLInputSource;
import org.xwiki.job.Job;
import org.xwiki.job.JobContext;
import org.xwiki.job.event.status.CancelableJobStatus;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.xml.stax.StAXUtils;

import com.google.common.base.Strings;

/**
 * Prepare a Confluence package to make it easier to import.
 *
 * @version $Id$
 * @since 9.16
 */
@Component(roles = ConfluenceXMLPackage.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ConfluenceXMLPackage implements AutoCloseable
{
    /**
     * The name of the main package file.
     */
    public static final String FILE_ENTITIES = "entities.xml";

    /**
     * The name of the file containing information about the instance.
     */
    public static final String FILE_DESCRIPTOR = "exportDescriptor.properties";

    /**
     * The name of the file containing the tasks.
     */
    public static final String FILE_TASKS = "AO_BAF3AA_AOINLINE_TASK.csv";

    public static final String KEY_ACTIVE = "active";

    /**
     * The property key to access the object class.
     * 
     * @since 9.78.0
     */
    public static final String KEY_CLASS = "class";

    /**
     * The property key to access the content.
     * 
     * @since 9.78.0
     */
    public static final String KEY_CONTENT = "content";

    /**
     * The property key to access the space name.
     */
    public static final String KEY_SPACE_NAME = "name";

    /**
     * The property key to access the space key.
     */
    public static final String KEY_SPACE_KEY = "key";

    /**
     * The property key to access the space description.
     */
    public static final String KEY_SPACE_DESCRIPTION = "description";

    /**
     * The property key to access the space home page.
     */
    public static final String KEY_SPACE_HOMEPAGE = "homePage";

    /**
     * The property key to access the space permissions.
     * @since 9.24.0
     */
    public static final String KEY_SPACE_PERMISSIONS = "permissions";

    /**
     * The property key to access the space templates.
     * @since 9.79.0
     */
    public static final String KEY_SPACE_PAGE_TEMPLATES = "pageTemplates";

    /**
     * The property key to access the space status.
     * @since 9.31.0
     */
    public static final String KEY_SPACE_STATUS = "spaceStatus";

    /**
     * The property key to access the space permission type.
     * @since 9.24.0
     */
    public static final String KEY_PERMISSION_TYPE = "type";

    /**
     * The property key to access the space permission group.
     * @since 9.24.0
     */
    public static final String KEY_SPACEPERMISSION_GROUP = "group";

    /**
     * The property key to access the space permission group.
     * @since 9.24.0
     */
    public static final String KEY_CONTENTPERMISSION_GROUP = "groupName";

    /**
     * The property key to access the space or content permission user subject.
     * @since 9.24.0
     */
    public static final String KEY_PERMISSION_ALLUSERSSUBJECT = "allUsersSubject";

    /**
     * The property key to access the space or content permission user subject.
     * @since 9.24.0
     */
    public static final String KEY_PERMISSION_USERSUBJECT = "userSubject";

    /**
     * The property key to access the label name.
     */
    public static final String KEY_LABEL_NAME = KEY_SPACE_NAME;

    /**
     * The property key to access the page labellings.
     * 
     * @since 9.78.0
     */
    public static final String KEY_LABELLINGS = "labellings";

    /**
     * The property key to access the label id.
     */
    public static final String KEY_LABELLING_LABEL = "label";

    /**
     * The property key to access the page id to which the label is attached.
     */
    public static final String KEY_LABELLING_CONTENT = KEY_CONTENT;

    /**
     * The property key to access the object content properties.
     * 
     * @since 9.79.0
     */
    public static final String KEY_CONTENTPROPERTIES = "contentProperties";

    /**
     * The property key to access the space permission username.
     * @since 9.24.0
     */
    public static final String KEY_SPACEPERMISSION_USERNAME = "userName";

    /**
     * The property key to access the page home page.
     */
    public static final String KEY_PAGE_HOMEPAGE = "homepage";

    /**
     * The property key to access the page parent.
     */
    public static final String KEY_PAGE_PARENT = "parent";

    /**
     * The property key to access the page space.
     */
    public static final String KEY_PAGE_SPACE = "space";

    /**
     * The property key to access the space permission's space.
     * @since 9.47.0
     */
    public static final String KEY_SPACE_PERMISSION_SPACE = KEY_PAGE_SPACE;

    /**
     * The property key to access the page title.
     */
    public static final String KEY_PAGE_TITLE = "title";

    /**
     * The property key to access the page contents.
     */
    public static final String KEY_PAGE_CONTENTS = "bodyContents";

    /**
     * The property key to access the page creation author name.
     */
    public static final String KEY_PAGE_CREATION_AUTHOR = "creatorName";

    /**
     * The property key to access the page creation author key.
     */
    public static final String KEY_PAGE_CREATION_AUTHOR_KEY = "creator";

    /**
     * The property key to access the page creation date.
     */
    public static final String KEY_PAGE_CREATION_DATE = "creationDate";

    /**
     * The property key to access the page revision.
     */
    public static final String KEY_PAGE_REVISION = "version";

    /**
     * The property key to access the page revision author key.
     */
    public static final String KEY_PAGE_REVISION_AUTHOR_KEY = "lastModifier";

    /**
     * The property key to access the page revision author name.
     */
    public static final String KEY_PAGE_REVISION_AUTHOR = "lastModifierName";

    /**
     * The property key to access the page revision date.
     */
    public static final String KEY_PAGE_REVISION_DATE = "lastModificationDate";

    /**
     * The property key to access the page revision comment.
     */
    public static final String KEY_PAGE_REVISION_COMMENT = "versionComment";

    /**
     * The property key to access the page revisions.
     */
    public static final String KEY_PAGE_REVISIONS = "historicalVersions";

    /**
     * The property key to access the page content status.
     */
    public static final String KEY_PAGE_CONTENT_STATUS = "contentStatus";

    /**
     * The property key to access the page body.
     */
    public static final String KEY_PAGE_BODY = "body";

    /**
     * The property key to access the page body type.
     */
    public static final String KEY_PAGE_BODY_TYPE = "bodyType";

    /**
     * The property key to access the page labellings.
     * @deprecated use {@value #KEY_LABELLINGS} instead
     */
    @Deprecated(since = "9.78.0")
    public static final String KEY_PAGE_LABELLINGS = KEY_LABELLINGS;

    /**
     * Old property key to access the page comments.
     */
    public static final String KEY_PAGE_COMMENTS = "comments";

    /**
     * The property key to access the original version.
     */
    public static final String KEY_PAGE_ORIGINAL_VERSION = "originalVersion";

    /**
     * The property key to access the position page property.
     */
    public static final String KEY_PAGE_POSITION = "position";

    /**
     * Old property to indicate attachment name.
     *
     * @see #KEY_ATTACHMENT_TITLE
     */
    public static final String KEY_ATTACHMENT_NAME = "fileName";

    /**
     * The property key to access the attachment title.
     */
    public static final String KEY_ATTACHMENT_TITLE = KEY_PAGE_TITLE;

    /**
     * Old field containing attachment page id.
     *
     * @see #KEY_ATTACHMENT_CONTAINERCONTENT
     */
    public static final String KEY_ATTACHMENT_CONTENT = KEY_CONTENT;

    /**
     * Field containing content property page id or attachment id or comment id.
     * @since 9.47.0
     */
    public static final String KEY_CONTENT_PROPERTY_CONTENT = KEY_ATTACHMENT_CONTENT;

    /**
     * The property key to access the attachment content key.
     */
    public static final String KEY_ATTACHMENT_CONTAINERCONTENT = "containerContent";

    /**
     * The property key to access the comment content key.
     * @since 9.47.0
     */
    public static final String KEY_COMMENT_CONTAINERCONTENT = KEY_ATTACHMENT_CONTAINERCONTENT;

    /**
     * Old property to indicate attachment size.
     *
     * @see #KEY_ATTACHMENT_CONTENTPROPERTIES
     * @see #KEY_ATTACHMENT_CONTENT_FILESIZE
     */
    public static final String KEY_ATTACHMENT_CONTENT_SIZE = "fileSize";

    /**
     * Old property to indicate attachment media type.
     *
     * @see #KEY_ATTACHMENT_CONTENTPROPERTIES
     * @see #KEY_ATTACHMENT_CONTENT_MEDIA_TYPE
     */
    public static final String KEY_ATTACHMENT_CONTENTTYPE = "contentType";

    /**
     * The property key to access the owning user key of a label.
     * @since 9.88.0
     */
    public static final String KEY_LABEL_OWNINGUSER = "owningUser";

    /**
     * The property key to access the owning user of a label - previous version.
     * @since 9.88.0
     */
    public static final String KEY_LABEL_USER = "user";

    /**
     * The property key to access the attachment content properties.
     * 
     * @deprecated since 9.79.0, use {@link #KEY_CONTENTPROPERTIES} instead
     */
    @Deprecated (since = "9.79.0")
    public static final String KEY_ATTACHMENT_CONTENTPROPERTIES = KEY_CONTENTPROPERTIES;

    /**
     * The property key to access the comment content key.
     * 
     * @since 9.72.0
     * @deprecated since 9.79.0, use {@link #KEY_CONTENTPROPERTIES} instead
     */
    @Deprecated (since = "9.79.0")
    public static final String KEY_COMMENT_CONTENTPROPERTIES = KEY_CONTENTPROPERTIES;

    /**
     * The property key to access the attachment content status.
     */
    public static final String KEY_ATTACHMENT_CONTENTSTATUS = KEY_PAGE_CONTENT_STATUS;

    /**
     * The property key to access the attachment minor edit status.
     */
    public static final String KEY_ATTACHMENT_CONTENT_MINOR_EDIT = "MINOR_EDIT";

    /**
     * The property key to access the attachment content size.
     */
    public static final String KEY_ATTACHMENT_CONTENT_FILESIZE = "FILESIZE";

    /**
     * The property key to access the attachment content media type.
     */
    public static final String KEY_ATTACHMENT_CONTENT_MEDIA_TYPE = "MEDIA_TYPE";

    /**
     * The property key to access the attachment creation author name.
     */
    public static final String KEY_ATTACHMENT_CREATION_AUTHOR = KEY_PAGE_CREATION_AUTHOR;

    /**
     * The property key to access the attachment creation author key.
     */
    public static final String KEY_ATTACHMENT_CREATION_AUTHOR_KEY = KEY_PAGE_CREATION_AUTHOR_KEY;

    /**
     * The property key to access the attachment creation date.
     */
    public static final String KEY_ATTACHMENT_CREATION_DATE = KEY_PAGE_CREATION_DATE;

    /**
     * The property key to access the attachment revision author.
     */
    public static final String KEY_ATTACHMENT_REVISION_AUTHOR = KEY_PAGE_REVISION_AUTHOR;

    /**
     * The property key to access the attachment revision date.
     */
    public static final String KEY_ATTACHMENT_REVISION_DATE = KEY_PAGE_REVISION_DATE;

    /**
     * The property key to access the attachment revision comment.
     */
    public static final String KEY_ATTACHMENT_REVISION_COMMENT = "comment";

    /**
     * Old property to indicate attachment revision.
     *
     * @see #KEY_ATTACHMENT_VERSION
     */
    public static final String KEY_ATTACHMENT_ATTACHMENTVERSION = "attachmentVersion";

    /**
     * The property key to access the attachment version.
     */
    public static final String KEY_ATTACHMENT_VERSION = KEY_PAGE_REVISION;

    /**
     * Old property to indicate attachment original revision.
     *
     * @see #KEY_ATTACHMENT_ORIGINALVERSIONID
     */
    public static final String KEY_ATTACHMENT_ORIGINALVERSION = KEY_PAGE_ORIGINAL_VERSION;

    /**
     * The property key to access the attachment original version.
     */
    public static final String KEY_ATTACHMENT_ORIGINALVERSIONID = "originalVersionId";

    /**
     * The property key to access the attachment DTO.
     */
    public static final String KEY_ATTACHMENT_DTO = "imageDetailsDTO";

    /**
     * The property key to access the content id or the comment id which has this body content.
     * Note that we don't keep it in our internal representation currently.
     */
    public static final String KEY_BODY_CONTENT_CONTENT = KEY_ATTACHMENT_CONTENT;

    /**
     * The property key to access the group name.
     */
    public static final String KEY_GROUP_NAME = KEY_SPACE_NAME;

    /**
     * The property key to access the group active status.
     */
    public static final String KEY_GROUP_ACTIVE = "active";

    /**
     * The property key to access the group local status.
     */
    public static final String KEY_GROUP_LOCAL = "local";

    /**
     * The property key to access the group creation date.
     */
    public static final String KEY_GROUP_CREATION_DATE = "createdDate";

    /**
     * The property key to access the group revision date.
     */
    public static final String KEY_GROUP_REVISION_DATE = "updatedDate";

    /**
     * The property key to access the group description.
     */
    public static final String KEY_GROUP_DESCRIPTION = KEY_SPACE_DESCRIPTION;

    /**
     * The property key to access the group external ID.
     */
    public static final String KEY_GROUP_EXTERNAL_ID = "externalId";

    /**
     * The property key to access the group members.
     */
    public static final String KEY_GROUP_MEMBERUSERS = "memberusers";

    /**
     * The property key to access the group members.
     */
    public static final String KEY_GROUP_MEMBERGROUPS = "membergroups";

    /**
     * The property key to access the username.
     */
    public static final String KEY_USER_NAME = KEY_SPACE_NAME;

    /**
     * The property key to access the user active status.
     */
    public static final String KEY_USER_ACTIVE = KEY_GROUP_ACTIVE;

    /**
     * The property key to access the user creation date.
     */
    public static final String KEY_USER_CREATION_DATE = KEY_GROUP_CREATION_DATE;

    /**
     * The property key to access the user revision date.
     */
    public static final String KEY_USER_REVISION_DATE = KEY_GROUP_REVISION_DATE;

    /**
     * The property key to access the user first name.
     */
    public static final String KEY_USER_FIRSTNAME = "firstName";

    /**
     * The property key to access the user last name.
     */
    public static final String KEY_USER_LASTNAME = "lastName";

    /**
     * The property key to access the user display name.
     */
    public static final String KEY_USER_DISPLAYNAME = "displayName";

    /**
     * The property key to access the user email.
     */
    public static final String KEY_USER_EMAIL = "emailAddress";

    /**
     * The property key to access the user password.
     */
    public static final String KEY_USER_PASSWORD = "credential";

    /**
     * The property key used to mark blog pages.
     *
     * @since 9.24.0
     */
    public static final String KEY_PAGE_BLOGPOST = "blogpost";

    /**
     * The old property key to access the content owning a comment.
     */
    public static final String KEY_COMMENT_OWNER = "owner";

    /**
     * The property key to access the content owning a content permission set.
     */
    public static final String KEY_CONTENT_PERMISSION_SET_OWNING_CONTENT = "owningContent";

    /**
     * The property key to access the content permission set owning a content permission.
     */
    public static final String KEY_CONTENT_PERMISSION_OWNING_SET = "owningSet";

    /**
     * The date format in a Confluence package (2012-03-07 17:16:48.158).
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * The id property key.
     * @since 9.47.0
     */
    public static final String KEY_ID = "id";

    /**
     * The old content permission sets key in pages.
     * @since 9.47.0
     */
    public static final String KEY_CONTENT_CONTENT_PERMISSION_SETS = "contentPermissionSets";

    /**
     * The old content permission key in content permission sets.
     * @since 9.47.0
     */
    public static final String KEY_CONTENT_PERMISSION_SET_CONTENT_PERMISSIONS = "contentPermissions";

    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    private static final String FOLDER_INTERNALUSER = "internalusers";

    private static final String FOLDER_USERIMPL = "userimpls";

    private static final String FOLDER_GROUP = "groups";

    private static final String FOLDER_OBJECTS = "objects";

    private static final String FOLDER_BODY_CONTENTS = "bodies";

    private static final String FOLDER_SPACE_PAGE_TEMPLATE = "template";

    private static final String FOLDER_SPACE_PERMISSIONS = KEY_SPACE_PERMISSIONS;

    private static final String PROPERTIES_FILENAME = "properties.properties";

    private static final String SPACE_STATUS_ARCHIVED = "ARCHIVED";

    private static final String OBJECT_TYPE_SPACE = "Space";

    private static final String OBJECT_TYPE_SPACE_DESCRIPTION = "SpaceDescription";

    private static final String OBJECT_TYPE_PAGE = "Page";

    private static final String OBJECT_TYPE_BLOG_POST = "BlogPost";

    private static final String OBJECT_TYPE_COMMENT = "Comment";

    private static final String OBJECT_TYPE_CONTENT_ENTITY_OBJECT = "ContentEntityObject";

    private static final String OBJECT_TYPE_CONTENT_PERMISSION_SET = "ContentPermissionSet";

    private static final String OBJECT_TYPE_CONTENT_PERMISSION = "ContentPermission";

    private static final String OBJECT_TYPE_SPACE_PERMISSION = "SpacePermission";

    private static final String OBJECT_TYPE_ATTACHMENT = "Attachment";

    private static final String OBJECT_TYPE_BODY_CONTENT = "BodyContent";

    private static final String OBJECT_TYPE_CONTENT_PROPERTY = "ContentProperty";

    private static final String OBJECT_TYPE_INTERNAL_GROUP = "InternalGroup";

    private static final String OBJECT_TYPE_INTERNAL_USER = "InternalUser";

    private static final String OBJECT_TYPE_LABEL = "Label";

    private static final String OBJECT_TYPE_LABELLING = "Labelling";

    private static final String OBJECT_TYPE_HIBERNATE_MEMBERSHIP = "HibernateMembership";

    private static final String OBJECT_TYPE_CONFLUENCE_USER_IMPL = "ConfluenceUserImpl";

    private static final String OBJECT_TYPE_PAGE_TEMPLATE = "PageTemplate";

    private static final String[] PARENT_PROPERTIES = new String[] {
        KEY_CONTENT_PERMISSION_OWNING_SET,
        KEY_CONTENT_PERMISSION_SET_OWNING_CONTENT,
        KEY_COMMENT_OWNER,
        KEY_ATTACHMENT_CONTAINERCONTENT,
        KEY_ATTACHMENT_CONTENT,
        KEY_COMMENT_CONTAINERCONTENT,
        KEY_CONTENT_PROPERTY_CONTENT,
        KEY_PAGE_SPACE
    };

    private static final String RESTORING_FROM_ANOTHER_VERSION_UNSUPPORTED_WARNING =
        "Restoring from a different version is unsupported and may lead to unexpected results.";

    private static final String KEY = KEY_SPACE_KEY;
    private static final String KEY_NAME = KEY_SPACE_NAME;
    private static final String STATE = "state";
    public static final String LONG_VALUE = "longValue";
    public static final String DATE_VALUE = "dateValue";
    public static final String STRING_VALUE = "stringValue";
    private static final String ATTACHMENTS = "attachments";
    private static final String PROPERTIES = "properties";
    private static final String CONTENT_ID = "CONTENT_ID";
    private static final String ID = "ID";
    private static final String[] TASK_FIELDS = {
        CONTENT_ID,
        ID,
        "CREATOR_USER_KEY",
        "COMPLETE_DATE",
        "CREATE_DATE",
        "UPDATE_DATE",
        "COMPLETE_USER_KEY",
        "GLOBAL_ID",
        "TASK_STATUS",
        "BODY",
        "CONTENT_VISIBLE",
        "SPACE_VISIBLE",
        "ASSIGNEE_USER_KEY",
        "DUE_DATE",
        "PAGE_TITLE",
        "SPACE_ID"
    };

    @Inject
    private Environment environment;

    @Inject
    private JobProgressManager progress;

    @Inject
    private Logger logger;

    @Inject
    private JobContext jobContext;

    private CancelableJobStatus jobStatus;

    private File directory;

    /**
     * Indicate if {@link #directory} is temporary (extracted from a source package).
     */
    private boolean temporaryDirectory;

    private File entities;

    private File descriptor;

    private File tree;

    private String workingDirectory;

    private ConfluenceCSVFile tasks;

    // Maps a space id to all the pages in this space
    private final Map<Long, List<Long>> pages = new LinkedHashMap<>();

    // Maps a space id to all the blog pages in this space
    private final Map<Long, List<Long>> blogPages = new LinkedHashMap<>();

    // Maps a page id to its direct non-blog children
    private final Map<Long, List<Long>> pageChildren = new LinkedHashMap<>();

    // List of parent pages that have not been seen
    private final Map<Long, Set<Long>> missingParents = new LinkedHashMap<>();

    // maps a space id to its home page
    private final Map<Long, Long> homePages = new LinkedHashMap<>();

    // maps a space id it to its orphan pages
    private final Map<Long, List<Long>> orphans = new LinkedHashMap<>();

    // maps a space key to its id
    private final Map<String, Long> casePreservingSpacesByKey = new HashMap<>();
    private final Map<String, Long> lowerSpacesByKey = new HashMap<>();

    // maps a space id to a lowercase title to page id mapping
    private final Map<Long, Map<String, Long>> pagesBySpaceAndLowerTitle = new HashMap<>();

    private String spaceKeyToImport;
    private long spaceIdToImport;

    /**
     * @return the content permission sets of the given page properties.
     * @param pageProperties the page of which to get the permission sets.
     * @since 9.47.0
     */
    public static List<Object> getContentPermissionSets(ConfluenceProperties pageProperties)
    {
        return pageProperties.getList(KEY_CONTENT_CONTENT_PERMISSION_SETS);
    }

    /**
     * @return the content permissions of the given permission set.
     * @param permissionSetProperties the permission set of which to get the permissions.
     * @since 9.47.0
     */
    public static List<Object> getContentPermissions(ConfluenceProperties permissionSetProperties)
    {
        return permissionSetProperties.getList(KEY_CONTENT_PERMISSION_SET_CONTENT_PERMISSIONS);
    }

    /**
     * @return the children of the given page.
     * @param pageId the page of which to get the children
     * @since 9.35.0
     */
    public List<Long> getPageChildren(Long pageId)
    {
        return pageChildren.getOrDefault(pageId, Collections.emptyList());
    }

    /**
     * @return the home page of the given space.
     * @param spaceId the space of which to get the home page
     * @since 9.35.0
     */
    public Long getHomePage(Long spaceId)
    {
        return homePages.get(spaceId);
    }

    /**
     * @return the orphans (pages which don't have a parent) of the given space
     * @param spaceId the space of which to get the orphans
     * @since 9.35.0
     */
    public List<Long> getOrphans(Long spaceId)
    {
        List<Long> spaceOrphans = this.orphans.getOrDefault(spaceId, Collections.emptyList());
        Collection<Long> spaceMissingParents = this.missingParents.get(spaceId);

        if (spaceMissingParents == null || spaceMissingParents.isEmpty()) {
            return spaceOrphans;
        }

        spaceOrphans = new ArrayList<>(spaceOrphans);
        for (Long missingParent : spaceMissingParents) {
            spaceOrphans.addAll(getPageChildren(missingParent));
        }
        return spaceOrphans;
    }

    /**
     * @return a page id from a space key and its title (this operation is case insensitive)
     * @param ciSpaceKey the space in which the page is supposed to be
     * @param ciPageTitle the title of the page
     * @since 9.35.0
     */
    public Long getPageId(String ciSpaceKey, String ciPageTitle)
    {
        if (StringUtils.isEmpty(ciSpaceKey) || StringUtils.isEmpty(ciPageTitle)) {
            return null;
        }

        Long spaceId = this.lowerSpacesByKey.get(ciSpaceKey.toLowerCase());
        if (spaceId == null) {
            return null;
        }

        Map<String, Long> pagesByTitle = this.pagesBySpaceAndLowerTitle.get(spaceId);
        if (pagesByTitle == null) {
            return null;
        }

        return pagesByTitle.get(ciPageTitle.toLowerCase());
    }

    /**
     * @return the comments of the given page
     * @param pageProperties the page of which to get the comments
     * @since 9.47.0
     */
    public List<Long> getPageComments(ConfluenceProperties pageProperties)
    {
        List<Long> l = getLongList(pageProperties, ConfluenceXMLPackage.KEY_PAGE_COMMENTS);
        return l == null ? new ArrayList<>() : l;
    }

    /**
     * @param source the source where to find the package to parse
     * @throws FilterException when any error happen during the reading of the package
     */
    public void setSource(InputSource source) throws FilterException
    {
        if (source instanceof FileInputSource) {
            fromFile(((FileInputSource) source).getFile());
        } else if (source instanceof URLInputSource
            && ((URLInputSource) source).getURL().getProtocol().equals("file")) {
            URI uri;
            try {
                uri = ((URLInputSource) source).getURL().toURI();
            } catch (Exception e) {
                throw new FilterException("The passed file URL is invalid", e);
            }
            fromFile(new File(uri));
        } else {
            try (source) {
                if (source instanceof InputStreamInputSource) {
                    fromStream((InputStreamInputSource) source);
                } else {
                    throw new FilterException(
                        String.format("Unsupported input source of type [%s]", source.getClass().getName()));
                }
            } catch (IOException e) {
                throw new FilterException(
                    String.format("Failed to read input source of type [%s]", source.getClass().getName()), e);
            }
        }

        this.entities = new File(this.directory, FILE_ENTITIES);
        this.descriptor = new File(this.directory, FILE_DESCRIPTOR);
        this.tasks = new ConfluenceCSVFile(null, this.directory, FILE_TASKS);
    }

    /**
     * @param workingDirectory the directory to use to extract the conflence package for processing (can be null)
     * @throws IOException when failing to access the package content
     * @throws FilterException when any error happen during the reading of the package
     * @since 9.60.0
     * @deprecated use ConfluenceXMLPackage#setWorkingDirectory() and then read() instead
     */
    @Deprecated(since = "9.94.0")
    public void read(String workingDirectory) throws IOException, FilterException
    {
        setWorkingDirectory(workingDirectory);
        read();
    }

    /**
     * @throws IOException when failing to access the package content
     * @throws FilterException when any error happen during the reading of the package
     * @since 9.94.0
     */
    public void read() throws IOException, FilterException
    {
        try {
            createTree();
        } catch (FilterException e) {
            throw e;
        } catch (Exception e) {
            throw new FilterException("Failed to analyze the package index", e);
        }
    }

    /**
     * @param source the source where to find the package to parse
     * @param workingDirectory the directory to use to extract the confluence package for processing (can be null)
     * @throws IOException when failing to access the package content
     * @throws FilterException when any error happen during the reading of the package
     * @since 9.37.0
     * @deprecated use #setSource(InputSource), #setWorkingDirectory(String) and #read()
     */
    @Deprecated (since = "9.94.0")
    public void read(InputSource source, String workingDirectory) throws IOException, FilterException
    {
        this.setSource(source);
        this.setWorkingDirectory(workingDirectory);
        this.read(workingDirectory);
    }

    /**
     * @param source the source where to find the package to parse
     * @throws IOException when failing to access the package content
     * @throws FilterException when any error happen during the reading of the package
     * @deprecated use #setSource(InputSource) and #read()
     */
    @Deprecated (since = "9.94.0")
    public void read(InputSource source) throws IOException, FilterException
    {
        setSource(source);
        read();
    }

    private void saveState() throws IllegalAccessException, IOException, FilterException
    {
        File state = new File(this.tree, STATE);
        if (!state.isDirectory() && !state.mkdir()) {
            throw new FilterException("Could not create the state directory");
        }
        Files.write(getExtractedPackageVersionPath(state), getVersion().getBytes());
        for (Field field : this.getClass().getDeclaredFields()) {
            if (isStateField(field)) {
                try (FileOutputStream fos = new FileOutputStream(new File(state, field.getName()));
                     ObjectOutputStream myObjectOutStream = new ObjectOutputStream(fos)) {
                    myObjectOutStream.writeObject(field.get(this));
                }
            }
        }
    }

    private void clearState()
    {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (isStateField(field)) {
                Object f;
                try {
                    f = field.get(this);
                } catch (IllegalAccessException e) {
                    logger.error("Unexpected error when clearing the state of field [{}]", field.getName(), e);
                    continue;
                }
                if (f instanceof Collection) {
                    ((Collection<?>) f).clear();
                } else if (f instanceof Map) {
                    ((Map<?, ?>) f).clear();
                }
            }
        }
    }

    private static boolean isStateField(Field field)
    {
        Class<?> type = field.getType();
        return !Modifier.isStatic(field.getModifiers())
            && (Map.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type));
    }

    private String getVersion()
    {
        String version = getClass().getPackage().getSpecificationVersion();
        return version == null ? "" : version;
    }

    private boolean restoreState(File tree)
    {
        if (!tree.exists()) {
            return false;
        }

        File state = new File(tree, STATE);
        if (!state.exists()) {
            return false;
        }

        checkExtractedPackageVersion(tree, state);

        for (Field field : this.getClass().getDeclaredFields()) {
            if (isStateField(field) && !restoreStateField(field, state)) {
                clearState();
                return false;
            }
        }

        this.tree = tree;

        return true;
    }

    private boolean restoreStateField(Field field, File state)
    {
        String name = field.getName();
        Object property;
        try (FileInputStream fis = new FileInputStream(new File(state, name));
             ObjectInputStream objectInput = new ObjectInputStream(fis)) {
            property = objectInput.readObject();
        } catch (IOException | ClassNotFoundException e) {
            logger.warn("Could not restore the package state: field [{}] is unreadable", name, e);
            return false;
        }
        try {
            Object f = field.get(this);
            if (f instanceof Collection && property instanceof Collection) {
                ((Collection) f).addAll((Collection) property);
            } else if (f instanceof Map && property instanceof Map) {
                ((Map) f).putAll((Map) property);
            } else {
                logger.warn("Could not restore the package state: wrong type for field [{}]", name);
                return false;
            }
        } catch (IllegalAccessException e) {
            logger.warn("Could not restore the Confluence package state", e);
            return false;
        }
        return true;
    }

    private void checkExtractedPackageVersion(File tree, File state)
    {
        logger.info("Restoring from extracted Confluence package found at [{}]", tree.getPath());
        try {
            String version = Files.readString(getExtractedPackageVersionPath(state)).trim();
            if (!getVersion().equals(version)) {
                this.logger.warn(
                    "The package was extracted by version [{}]. Current version is [{}]. "
                        + RESTORING_FROM_ANOTHER_VERSION_UNSUPPORTED_WARNING, version, getVersion());
            }
        } catch (IOException e) {
            this.logger.warn(
                "Could not determine the version of the extracted package. "
                + RESTORING_FROM_ANOTHER_VERSION_UNSUPPORTED_WARNING, e);
        }
    }

    private static Path getExtractedPackageVersionPath(File state)
    {
        return Paths.get(state.getPath() + "/version.txt");
    }

    /**
     * @param workingDirectoryPath the path to the working directory to restore the state from
     * @return whether restoring succeeded
     */
    public boolean restoreState(String workingDirectoryPath)
    {
        return restoreState(new File(workingDirectoryPath));
    }

    private void fromFile(File file) throws FilterException
    {
        if (file.isDirectory()) {
            this.directory = file;
        } else {
            try (FileInputStream stream = new FileInputStream(file)) {
                fromStream(stream);
            } catch (IOException e) {
                throw new FilterException(String.format("Failed to read Confluence package in file [%s]", file), e);
            }
        }
    }

    private void fromStream(InputStreamInputSource source) throws IOException
    {
        try (InputStream stream = source.getInputStream()) {
            fromStream(stream);
        }
    }

    private void fromStream(InputStream stream) throws IOException
    {
        // Get temporary folder
        this.directory =
            Files.createTempDirectory(this.environment.getTemporaryDirectory().toPath(), "confluencexml").toFile();
        this.temporaryDirectory = true;

        // Extract the zip
        ZipArchiveInputStream zais = new ZipArchiveInputStream(stream);
        for (ZipArchiveEntry zipEntry = zais.getNextZipEntry(); zipEntry != null; zipEntry = zais.getNextZipEntry()) {
            if (!zipEntry.isDirectory()) {
                String path = zipEntry.getName();
                File file = new File(this.directory, path);

                FileUtils.copyInputStreamToFile(CloseShieldInputStream.wrap(zais), file);
            }
        }
    }

    /**
     * @param properties the properties from where to extract the date
     * @param key the key associated with the date
     * @return the date associated with the passed key in the passed properties or null
     * @throws ParseException when failing to parse the date
     */
    public Date getDate(ConfluenceProperties properties, String key) throws ParseException
    {
        String str = properties.getString(key);

        if (str == null || str.isEmpty()) {
            return null;
        }

        return new SimpleDateFormat(DATE_FORMAT).parse(str);
    }

    /**
     * @param properties the properties from where to extract the list
     * @param key the key associated with the list
     * @return the list associated with the passed key in the passed properties or null
     */
    public List<Long> getLongList(ConfluenceProperties properties, String key)
    {
        return getLongList(properties, key, null);
    }

    /**
     * @param properties the properties from where to extract the list
     * @param key the key associated with the list
     * @param def the default value to return if no list is found
     * @return the list associated with the passed key in the passed properties or def
     */
    public List<Long> getLongList(ConfluenceProperties properties, String key, List<Long> def)
    {
        List<Object> list = properties.getList(key, null);

        if (list == null) {
            return def;
        }

        if (list.isEmpty() || list.get(0) instanceof Long) {
            return (List) list;
        }

        List<Long> integerList = new ArrayList<>(list.size());
        for (Object element : list) {
            integerList.add(Long.valueOf(element.toString()));
        }

        return integerList;
    }

    /**
     * @param properties the properties where to find the content identifier
     * @param key the key to find the content identifiers
     * @return the properties about the content
     * @throws ConfigurationException when failing to create the properties
     */
    public ConfluenceProperties getContentProperties(ConfluenceProperties properties, String key)
        throws ConfigurationException
    {
        List<Long> elements = getLongList(properties, key);

        if (elements == null) {
            if (KEY_CONTENTPROPERTIES.equals(key)) {
                // Content properties are not listed in content objects in CSV backups
                Long contentId = properties.getLong(KEY_ID, null);
                if (contentId != null) {
                    return getContentPropertiesForCSVBackup(contentId);
                }
            }

            return null;
        }

        // This is for XML backups
        ConfluenceProperties contentProperties = new ConfluenceProperties();
        for (Long element : elements) {
            ConfluenceProperties contentProperty = getContentProperties(-1, element, false);
            addContentProperty(element, contentProperty, contentProperties);
        }

        return contentProperties;
    }

    private void addContentProperty(Object propId, ConfluenceProperties p, ConfluenceProperties contentProperties)
    {
        String name = p == null ? null : p.getString(KEY_NAME);
        if (name == null) {
            logger.warn("ContentProperty [{}] was not found", propId);
            return;
        }

        Object value = p.getString(LONG_VALUE, null);
        if (Strings.isNullOrEmpty((String) value)) {
            value = p.getString(DATE_VALUE, null);
            if (Strings.isNullOrEmpty((String) value)) {
                value = p.getString(STRING_VALUE, null);
            }
        } else {
            value = p.getLong(LONG_VALUE, null);
        }

        contentProperties.setProperty(name, value);
    }

    private ConfluenceProperties getContentPropertiesForCSVBackup(long contentId) throws ConfigurationException
    {
        ConfluenceProperties gatheredProperties = new ConfluenceProperties();
        File contentPropertiesFolder = getContentPropertiesFolder(contentId);
        if (!contentPropertiesFolder.exists()) {
            return null;
        }

        File[] files = contentPropertiesFolder.listFiles();
        if (files == null) {
            return null;
        }

        for (File child : files) {
            ConfluenceProperties contentProperty = ConfluenceProperties.create(new File(child, PROPERTIES_FILENAME));
            addContentProperty(child.getName(), contentProperty, gatheredProperties);
        }

        return gatheredProperties;
    }

    /**
     * @param spaceId the identifier of the space
     * @return the value to use as name for the space
     * @throws ConfigurationException when failing to create the properties
     */
    public String getSpaceName(long spaceId) throws ConfigurationException
    {
        ConfluenceProperties spaceProperties = getSpaceProperties(spaceId);

        return spaceProperties.getString(KEY_SPACE_NAME);
    }

    /**
     * @param spaceProperties the properties containing information about the space
     * @return the value to use as name for the space
     */
    public static String getSpaceName(ConfluenceProperties spaceProperties)
    {
        String key = spaceProperties.getString(KEY_SPACE_NAME);

        return key != null ? key : spaceProperties.getString(KEY_SPACE_KEY);
    }

    /**
     * @param spaceId the identifier of the space
     * @return the value to use as key for the space
     * @throws ConfigurationException when failing to create the properties
     */
    public String getSpaceKey(long spaceId) throws ConfigurationException
    {
        ConfluenceProperties spaceProperties = getSpaceProperties(spaceId);

        return spaceProperties.getString(KEY_SPACE_KEY);
    }

    /**
     * @return whether the given space is archived (or false if the status cannot be determined)
     * @param spaceId the identifier of the space
     * @throws ConfigurationException when failing to create the properties
     * @since 9.31.0
     */
    public boolean isSpaceArchived(long spaceId) throws ConfigurationException
    {
        return SPACE_STATUS_ARCHIVED.equals(getSpaceStatus(spaceId));
    }

    /**
     * @return the status of the space
     * @param spaceId the identifier of the space
     * @throws ConfigurationException when failing to create the properties
     * @since 9.31.0
     */
    public String getSpaceStatus(long spaceId) throws ConfigurationException
    {
        ConfluenceProperties spaceProperties = getSpaceProperties(spaceId);

        return spaceProperties.getString(KEY_SPACE_STATUS);
    }

    /**
     * @param spaceProperties the properties containing information about the space
     * @return the value to use as key for the space
     */
    public static String getSpaceKey(ConfluenceProperties spaceProperties)
    {
        String key = spaceProperties.getString(KEY_SPACE_KEY);

        return key != null ? key : spaceProperties.getString(KEY_SPACE_NAME);
    }

    /**
     * @return a map of spaces where the key is the name of the space and the value is its id
     * @since 9.21.0
     * @deprecated since 9.83.0, because many uses of this method are erroneous: space keys are case-insensitive and
     *             these uses likely don't take this in account. They also modified the list of spaces, which is not
     *             supported anymore. Use {@link #getSpaceKeys(boolean)}, {@link #getSpaces()} and
     *             {@link #getSpaceId(String)} instead
     */
    @Deprecated (since = "9.83.0")
    public Map<String, Long> getSpacesByKey()
    {
        return casePreservingSpacesByKey;
    }

    /**
     * @return the id of the given space
     * @param spaceKey the space key (the operation is case-insensitive)
     * @since 9.83.0
     */
    public Long getSpaceId(String spaceKey)
    {
        if (spaceKey == null) {
            return null;
        }
        return lowerSpacesByKey.get(spaceKey.toLowerCase());
    }

    /**
     * @return the collection of spaces in this package
     * @since 9.83.0
     */
    public Collection<Long> getSpaces()
    {
        return Collections.unmodifiableCollection(lowerSpacesByKey.values());
    }

    /**
     * @param lowerCase whether the space keys should be returned lower case
     * @return the list of space keys
     */
    public Collection<String> getSpaceKeys(boolean lowerCase)
    {
        return new ArrayList<>((lowerCase ? lowerSpacesByKey : casePreservingSpacesByKey).keySet());
    }

    /**
     * @return a map of spaces with their pages
     */
    public Map<Long, List<Long>> getPages()
    {
        return this.pages;
    }

    /**
     * @return a map of blog spaces with their pages
     * @since 9.24.0
     */
    public Map<Long, List<Long>> getBlogPages()
    {
        return this.blogPages;
    }

    /**
     * Ignore extraneous spaces for Confluence space exports.
     * Works around <a href="https://jira.atlassian.com/browse/CONFSERVER-16575">CONFSERVER-16575</a>.
     */
    public void ignoreExtraneousSpaces()
    {
        spaceKeyToImport = getDescriptorField("spaceKey");
    }

    /**
     * @param workingDirectory the working directory to set
     * @since 9.94.0
     */
    public void setWorkingDirectory(String workingDirectory)
    {
        this.workingDirectory = workingDirectory;
    }

    private void createTree() throws IOException, FilterException, ConfluenceCanceledException
    {
        mkdirTree();
        getJobStatus();
        readObjects();
    }

    private void readXMLObjects(int steps) throws FilterException, ConfluenceCanceledException
    {
        try (CountingInputStream s = new CountingInputStream(new BufferedInputStream(new FileInputStream(entities)))) {
            XMLStreamReader xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(new WithoutControlCharactersReader(s));
            xmlReader.nextTag();

            long size = entities.length();
            boolean inStep = false;
            long stepSize = size / steps;
            long nextStepPos = stepSize;
            XMLConfluenceObjectReader objectReader = new XMLConfluenceObjectReader(xmlReader);
            for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
                if (!inStep) {
                    progress.startStep(this);
                    inStep = true;
                }
                String elementName = xmlReader.getLocalName();

                if (elementName.equals("object")) {
                    readXMLObject(objectReader);
                } else {
                    StAXUtils.skipElement(xmlReader);
                }
                long pos = s.getByteCount();
                if (pos >= nextStepPos) {
                    progress.endStep(this);
                    nextStepPos += stepSize;
                    inStep = false;
                }
            }

            if (inStep) {
                progress.endStep(this);
            }
        } catch (IOException | XMLStreamException | ConfigurationException e) {
            throw new FilterException(e);
        }
    }

    private void readCSVObjects(int steps) throws FilterException, ConfluenceCanceledException
    {
        try {
            readCSVTable("bodycontent", this::readBodyContentObject);
            readCSVTable("content", this::readContentObject);
            readCSVTable("content_label", this::readLabellingObject);
            readCSVTable("content_perm", this::readContentPermissionObject);
            readCSVTable("content_perm_set", this::readContentPermissionSetObject);
            readCSVTable("contentproperties", this::readContentPropertyObject);
            readCSVTable("label", this::readLabelObject);
            readCSVTable("pagetemplates", this::readPageTemplateObject);
            readCSVTable("spacepermissions", this::readSpacePermissionObject);
            readCSVTable("spaces", this::readSpaceObject);
            readCSVTable("user_mapping", this::readUserImplObject);

        } catch (IOException | ConfigurationException e) {
            throw new FilterException(e);
        }
    }

    private void readCSVTable(String basename, ConfluenceConsumer reader)
            throws ConfigurationException, ConfluenceCanceledException, FilterException, IOException
    {
        ConfluenceCSVFile.readRecords(this::getCancelledJob, this.directory, basename, reader);
    }

    private void readObjects() throws FilterException, ConfluenceCanceledException
    {
        int steps = 100;
        progress.pushLevelProgress(steps, this);
        if (isCSV()) {
            readCSVObjects(steps);
        } else {
            readXMLObjects(steps);
        }
        cleanUpUnwantedSpaces();
        try {
            saveState();
        } catch (IllegalAccessException | IOException e) {
            logger.warn("Unable to save the package state, restoring for later migrations won't work", e);
        }
        progress.popLevelProgress(this);
    }

    private void readContentPropertyObject(ConfluenceObjectReader objectReader)
            throws ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        Long id = asLong(objectReader.readObjectProperties(properties, ConfluenceObjectFields.CONTENTPROPERTIES));
        if (id != null) {
            // contentId is available in CSV exports but not XML exports.
            // When we don't know, we store in the content with id -1
            long contentId = properties.getLong("contentid", -1L);
            saveContentPropertyProperties(properties, contentId, id);
        }
    }

    private void readLabelObject(ConfluenceObjectReader objectReader) throws ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        Object id = objectReader.readObjectProperties(properties, ConfluenceObjectFields.LABEL);
        if (id != null) {
            saveObjectProperties(properties, id.toString());
        }
    }

    /**
     * @return the revisions of a content by id.
     * @param pageProperties The page for which to get the revisions
     * @param includeIgnored Whether to include deleted or draft pages
     * @param throwOnError Whether to throw if something wrong happens
     * @throws ConfigurationException if something wrong happens
     * @since 9.87.O
     */
    public Map<Long, ConfluenceProperties> getRevisionsById(ConfluenceProperties pageProperties,
        boolean includeIgnored, boolean throwOnError) throws ConfigurationException
    {
        // Note: revisions can appear several times in the list
        List<Long> revisionIds = getLongList(pageProperties, ConfluenceXMLPackage.KEY_PAGE_REVISIONS);
        Map<Long, ConfluenceProperties> revisionsById = new HashMap<>(revisionIds.size());
        for (Long revisionId : revisionIds) {
            ConfluenceProperties revisionProperties = null;
            try {
                revisionProperties = getPageProperties(revisionId, false);
                if (revisionProperties == null) {
                    this.logger.warn("Can't find page revision with id [{}]", revisionId);
                }
            } catch (ConfigurationException e) {
                if (throwOnError) {
                    throw e;
                }
                this.logger.error("Failed to get page revision with id [{}]", revisionId, e);
            }

            if (revisionProperties != null && (includeIgnored || !mustIgnoreContent(revisionProperties))) {
                revisionsById.put(revisionId, revisionProperties);
            }
        }
        return revisionsById;
    }

    private void mkdirTree() throws IOException
    {
        if (this.tree == null) {
            this.tree = workingDirectory == null
                ? Files.createTempDirectory(this.environment.getTemporaryDirectory().toPath(),
    "confluencexml-tree").toFile()
                : new File(workingDirectory);
        }

        if (!this.tree.isDirectory() && !this.tree.mkdir()) {
            throw new IOException("Could not create the working directory to extract the Confluence package");
        }
    }

    private void getJobStatus()
    {
        Job job = this.jobContext.getCurrentJob();
        if (job != null) {
            JobStatus status = job.getStatus();
            if (status instanceof CancelableJobStatus) {
                this.jobStatus = (CancelableJobStatus) status;
            }
        }
    }

    private void setClass(String className, ConfluenceProperties properties)
    {
        properties.setProperty(KEY_CLASS, className);
    }

    /**
     * @param properties the objet properties
     * @return the name of the object class represented by the passed properties
     * @since 9.78.0
     */
    public String getClass(ConfluenceProperties properties)
    {
        return (String) properties.getProperty(KEY_CLASS);
    }

    /**
     * @param properties the object properties
     * @return true if the passed properties are those of a page
     * @since 9.78.0
     */
    public boolean isPage(ConfluenceProperties properties)
    {
        return OBJECT_TYPE_PAGE.equals(getClass(properties));
    }

    /**
     * @param pageProperties the properties of a page
     * @return true of the passed properties are those of the home page of a space
     * @since 9.78.0
     */
    public boolean isHomePage(ConfluenceProperties pageProperties)
    {
        return pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_HOMEPAGE);
    }

    private void readXMLObject(XMLConfluenceObjectReader objectReader)
        throws XMLStreamException, ConfigurationException, FilterException, ConfluenceCanceledException
    {
        getCancelledJob();

        String type = objectReader.getNextClass();

        if (type != null) {
            switch (type) {
                case OBJECT_TYPE_PAGE:
                    readPageObject(objectReader, false);
                    break;
                case OBJECT_TYPE_SPACE:
                    readSpaceObject(objectReader);
                    break;
                case OBJECT_TYPE_SPACE_DESCRIPTION:
                    readSpaceDescriptionObject(objectReader);
                    break;
                case OBJECT_TYPE_INTERNAL_USER:
                    readInternalUserObject(objectReader);
                    break;
                case OBJECT_TYPE_CONFLUENCE_USER_IMPL:
                    readUserImplObject(objectReader);
                    break;
                case OBJECT_TYPE_INTERNAL_GROUP:
                    readGroupObject(objectReader);
                    break;
                case OBJECT_TYPE_HIBERNATE_MEMBERSHIP:
                    readMembershipObject(objectReader);
                    break;
                case OBJECT_TYPE_BODY_CONTENT:
                    readBodyContentObject(objectReader);
                    break;
                case OBJECT_TYPE_PAGE_TEMPLATE:
                    readPageTemplateObject(objectReader);
                    break;
                case OBJECT_TYPE_SPACE_PERMISSION:
                    readSpacePermissionObject(objectReader);
                    break;
                case OBJECT_TYPE_CONTENT_PERMISSION:
                    readContentPermissionObject(objectReader);
                    break;
                case OBJECT_TYPE_CONTENT_PERMISSION_SET:
                    readContentPermissionSetObject(objectReader);
                    break;
                case OBJECT_TYPE_ATTACHMENT:
                    readAttachmentObject(objectReader);
                    break;
                case OBJECT_TYPE_COMMENT:
                    readCommentObject(objectReader);
                    break;
                case OBJECT_TYPE_BLOG_POST:
                    readPageObject(objectReader, true);
                    break;
                case OBJECT_TYPE_LABELLING:
                    readLabellingObject(objectReader);
                    break;
                case OBJECT_TYPE_CONTENT_PROPERTY:
                    readContentPropertyObject(objectReader);
                    break;
                default:
                    readGenericObject(objectReader);
            }
        }
    }

    private void getCancelledJob() throws ConfluenceCanceledException
    {
        if (this.jobStatus != null && this.jobStatus.isCanceled()) {
            throw new ConfluenceCanceledException();
        }
    }

    private void readGenericObject(ConfluenceObjectReader objectReader)
        throws FilterException, ConfigurationException
    {
        ConfluenceProperties properties = new ConfluenceProperties();
        Object key = objectReader.readObjectProperties(properties, null);
        if (key != null) {
            saveObjectProperties(properties, key.toString());
        }
    }

    private void readAttachmentObject(ConfluenceObjectReader objectReader)
        throws FilterException, ConfigurationException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        Long attachmentId = asLong(objectReader.readObjectProperties(properties, ConfluenceObjectFields.CONTENT));
        if (attachmentId != null) {
            readAttachmentObject(properties, attachmentId);
        }
    }

    private void readAttachmentObject(ConfluenceProperties properties, long attachmentId) throws ConfigurationException
    {
        Long pageId = getAttachmentPageId(properties);

        if (pageId != null) {
            saveAttachmentProperties(properties, pageId, attachmentId);
        }
    }

    private void readCommentObject(ConfluenceObjectReader objectReader) throws FilterException, ConfigurationException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        Long commentId = asLong(objectReader.readObjectProperties(properties, ConfluenceObjectFields.CONTENT));
        if (commentId != null) {
            readCommentObject(properties, commentId);
            Long parentid = properties.getLong("parentid", null);
            Long pageid = properties.getLong("parentid", null);
            Long parentcommentid = properties.getLong("parentid", null);
            if (parentcommentid != null) {
                properties.addProperty(KEY_PAGE_PARENT, parentcommentid);
            } else if (parentid != null) {
                properties.addProperty(KEY_PAGE_PARENT, parentid);
            }
            if (pageid != null) {
                properties.addProperty(KEY_COMMENT_CONTAINERCONTENT, pageid);
            }
        }
    }

    private void readCommentObject(ConfluenceProperties properties, long commentId) throws ConfigurationException
    {
        saveObjectProperties(properties, Long.toString(commentId));

        saveInParent(properties, KEY_COMMENT_CONTAINERCONTENT, OBJECT_TYPE_PAGE, KEY_PAGE_COMMENTS, commentId);
    }

    private void saveInParent(ConfluenceProperties childProperties, String parentInChildField, String parentType,
        String childrenInParentField, long childId) throws ConfigurationException
    {
        Long parentId = childProperties.getLong(parentInChildField, null);
        if (parentId == null) {
            return;
        }
        saveInParent(parentId, parentType, childrenInParentField, childId);
    }

    private void saveInParent(long parentId, String parentType, String childrenInParentField, long childId)
        throws ConfigurationException
    {
        ConfluenceProperties parentProperties = getParentObjectByType(parentType, parentId, false);
        if (parentProperties == null) {
            return;
        }
        List<Long> childIds = getLongList(parentProperties, childrenInParentField);
        if (childIds == null) {
            childIds = new ArrayList<>();
        }
        if (!childIds.contains(childId)) {
            childIds.add(childId);
            parentProperties.setProperty(childrenInParentField, childIds);
            parentProperties.save();
        }
    }

    private ConfluenceProperties getParentObjectByType(String type, long id, boolean emptyBody)
            throws ConfigurationException
    {
        ConfluenceProperties properties = null;

        switch (type) {
            case OBJECT_TYPE_SPACE:
                properties = getSpaceProperties(id);
                break;
            case OBJECT_TYPE_SPACE_DESCRIPTION:
                properties = getSpaceDescriptorProperties(id, true);
                break;
            case OBJECT_TYPE_CONTENT_ENTITY_OBJECT:
                // Apparently, some Confluence versions don't specify the actual, concrete type of objects (anymore?)
                // in the BodyContent's content property, and gives "ContentEntityObject" instead. This is not very
                // convenient.
                properties = getContentEntityPropertiesForBodyContent(id);
                break;
            case OBJECT_TYPE_PAGE:
            case OBJECT_TYPE_BLOG_POST:
                properties = getPageProperties(id, true);
                break;
            case OBJECT_TYPE_CONTENT_PERMISSION_SET:
                properties = getContentPermissionSetProperties(id);
                break;
            case OBJECT_TYPE_COMMENT:
                properties = getCommentProperties(id, true);
                break;
            case OBJECT_TYPE_PAGE_TEMPLATE:
                properties = getSpacePageTemplateProperties(id, false);
                break;
            default:
                if (!emptyBody) {
                    // We don't complain about unexpected parent types for empty body that we can just discard
                    logger.error(
                        "Unexpected type [{}] for parent object id [{}]."
                        + "This is a bug in confluence-xml, please report.",
                        type, id);
                }
        }

        return properties;
    }

    private ConfluenceProperties getCommentProperties(long id, boolean create) throws ConfigurationException
    {
        return getObjectProperties(FOLDER_OBJECTS, Long.toString(id), create);
    }

    private ConfluenceProperties getBodyContentProperties(String objectId, boolean create) throws ConfigurationException
    {
        return getObjectProperties(FOLDER_BODY_CONTENTS, objectId, create, false);
    }

    /**
     * @return properties in which to store the body content of an object. Preferably, we want to store the body in
     * the object properties itself, if possible. If not (because we have not parsed it yet, we will store body content
     * properties in a dedicated properties object.
     * @param id the id of which the body content will be filled
     * @throws ConfigurationException if something wrong happens
     */
    private ConfluenceProperties getContentEntityPropertiesForBodyContent(long id) throws ConfigurationException
    {
        ConfluenceProperties properties = getPageProperties(id, false);

        if (properties == null) {
            properties = getSpaceDescriptorProperties(id, false);
        }

        if (properties == null) {
            properties = getCommentProperties(id, false);
        }

        if (properties == null) {
            properties = getSpacePageTemplateProperties(id, false);
        }

        if (properties == null) {
            properties = getBodyContentProperties(Long.toString(id), true);
        }

        return properties;
    }

    private Long getAttachmentPageId(ConfluenceProperties properties)
    {
        Long pageId = properties.getLong(KEY_ATTACHMENT_CONTAINERCONTENT, null);

        if (pageId == null) {
            pageId = properties.getLong(KEY_ATTACHMENT_CONTENT, null);
        }

        return pageId;
    }

    private boolean shouldIgnoreSpace(long spaceId)
    {
        if (spaceIdToImport == spaceId) {
            return false;
        }

        return spaceIdToImport != 0;
    }

    void cleanUpUnwantedSpaces() throws FilterException
    {
        if (StringUtils.isEmpty(spaceKeyToImport)) {
            return;
        }
        if (spaceIdToImport == 0) {
            throw new FilterException("The space with the key defined in the space export's exportDescriptor.properties"
                + " file was not found. Your export is likely corrupted. You can also try importing this package with"
                + " the 'Import extraneous spaces from the space export' parameter enabled.");

        }
        Predicate<Long> spaceIdNotToImport = spaceId -> spaceId != spaceIdToImport;
        pages.keySet().removeIf(spaceIdNotToImport);
        blogPages.keySet().removeIf(spaceIdNotToImport);
        homePages.keySet().removeIf(spaceIdNotToImport);
        orphans.keySet().removeIf(spaceIdNotToImport);
        pagesBySpaceAndLowerTitle.keySet().removeIf(spaceIdNotToImport);
        casePreservingSpacesByKey.keySet().removeIf(spaceKey -> !spaceKey.equals(spaceKeyToImport));
        lowerSpacesByKey.keySet().removeIf(spaceKey -> !spaceKey.equalsIgnoreCase(spaceKeyToImport));
    }

    private void readSpaceObject(ConfluenceObjectReader objectReader) throws FilterException, ConfigurationException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        setClass(OBJECT_TYPE_SPACE, properties);

        Long spaceId = asLong(objectReader.readObjectProperties(properties, ConfluenceObjectFields.SPACES));
        if (spaceId != null) {
            String spaceKey = properties.getString(KEY_SPACE_KEY);
            if (spaceKeyToImport != null) {
                if (spaceKeyToImport.equals(spaceKey)) {
                    spaceIdToImport = spaceId;
                    cleanUpUnwantedSpaces();
                } else {
                    return;
                }
            }

            if (spaceKey != null) {
                this.casePreservingSpacesByKey.put(spaceKey, spaceId);
                this.lowerSpacesByKey.put(spaceKey.toLowerCase(), spaceId);
            }

            saveSpaceProperties(properties, spaceId);
            maybeUpdateHomePage(properties, spaceId);
            this.pages.computeIfAbsent(spaceId, k -> new LinkedList<>());
        }
    }

    private void maybeUpdateHomePage(ConfluenceProperties properties, long spaceId) throws ConfigurationException
    {
        Long homePageId = properties.getLong(KEY_SPACE_HOMEPAGE, null);
        if (homePageId == null) {
            return;
        }
        Long formerHome = homePages.get(spaceId);
        if (!homePageId.equals(formerHome)) {
            ConfluenceProperties homePageProperties = getPageProperties(homePageId, true);
            homePageProperties.setProperty(KEY_PAGE_HOMEPAGE, true);
            savePageProperties(homePageProperties, homePageId);
            if (formerHome != null) {
                ConfluenceProperties formerHomePageProperties = getPageProperties(formerHome, false);
                if (formerHomePageProperties != null) {
                    formerHomePageProperties.clearProperty(KEY_PAGE_HOMEPAGE);
                    formerHomePageProperties.save();
                    orphans.computeIfAbsent(spaceId, k -> new ArrayList<>()).add(formerHome);
                }
            }
            setHomePage(spaceId, homePageId);
        }
    }

    private void readPageTemplateObject(ConfluenceObjectReader objectReader)
        throws ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        Long templateId = asLong(objectReader.readObjectProperties(properties, ConfluenceObjectFields.PAGETEMPLATES));

        Long spaceId = properties.getLong(KEY_PAGE_SPACE, null);
        if (templateId != null && spaceId != null && !shouldIgnoreSpace(spaceId)) {
            saveSpacePageTemplateProperties(properties, spaceId, (long) templateId);
            saveInParent(properties, KEY_PAGE_SPACE, OBJECT_TYPE_SPACE,
                KEY_SPACE_PAGE_TEMPLATES, templateId);
        }
    }

    private void readSpacePermissionObject(ConfluenceObjectReader objectReader)
        throws ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        Long permissionId = asLong(objectReader.readObjectProperties(properties, ConfluenceObjectFields.SPACEPERMISSIONS));
        if (permissionId != null && properties.getBoolean(KEY_ACTIVE, true)) {
            Long spaceId = properties.getLong(KEY_PAGE_SPACE, null);
            if (spaceId != null && !shouldIgnoreSpace(spaceId)) {
                saveSpacePermissionProperties(properties, spaceId, permissionId);
                saveInParent(properties, KEY_SPACE_PERMISSION_SPACE, OBJECT_TYPE_SPACE,
                        KEY_SPACE_PERMISSIONS, permissionId);
            }
        }
    }

    private void readContentPermissionObject(ConfluenceObjectReader objectReader)
        throws FilterException, ConfigurationException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        Long permissionId = asLong(objectReader.readObjectProperties(properties, ConfluenceObjectFields.CONTENT_PERM));
        if (permissionId != null) {
            Long contentPermissionSetId = properties.getLong(KEY_CONTENT_PERMISSION_OWNING_SET, null);
            if (contentPermissionSetId != null) {
                saveContentPermissionProperties(properties, contentPermissionSetId, permissionId);
                saveInParent(properties, KEY_CONTENT_PERMISSION_OWNING_SET, OBJECT_TYPE_CONTENT_PERMISSION_SET,
                        KEY_CONTENT_PERMISSION_SET_CONTENT_PERMISSIONS, permissionId);
            }
        }
    }

    private void readContentPermissionSetObject(ConfluenceObjectReader objectReader)
        throws FilterException, ConfigurationException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        Long permissionSetId = asLong(objectReader.readObjectProperties(properties, ConfluenceObjectFields.CONTENT_PERM_SET));
        if (permissionSetId != null) {
            saveContentPermissionSetProperties(properties, permissionSetId);

            Long owningContentId = properties.getLong(KEY_CONTENT_PERMISSION_SET_OWNING_CONTENT, null);
            if (owningContentId != null) {
                saveInParent(properties, KEY_CONTENT_PERMISSION_SET_OWNING_CONTENT,
                        OBJECT_TYPE_PAGE, KEY_CONTENT_CONTENT_PERMISSION_SETS, permissionSetId);
            }
        }
    }

    private void readBodyContentObject(ConfluenceObjectReader objectReader)
        throws ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();
        properties.disableListDelimiter();

        objectReader.readObjectProperties(properties, ConfluenceObjectFields.BODY_CONTENT);

        // We save properties of the body content object in the corresponding page object.
        Long parentId = properties.getLong(KEY_BODY_CONTENT_CONTENT, null);
        if (parentId != null) {
            String className = getBodyPropertyClass(properties);
            if (className == null) {
                // Nothing to lose at this point... should not happen.
                className = OBJECT_TYPE_PAGE;
            }
            String body = properties.getString(KEY_PAGE_BODY);
            ConfluenceProperties parent = getParentObjectByType(className, parentId, StringUtils.isEmpty(body));
            if (parent == null) {
                return;
            }

            // This property messes with code that finds parents of objects, so we remove it.
            // There is no way we need it, we already have the id of the content in the id property.
            properties.clearProperty(KEY_BODY_CONTENT_CONTENT);

            // We replace the id of the body content object with the page id, as it is this one which is correct for the
            // page property object in which we save the body content.
            // We could have cleared it and let this property be set when reading the page object, but we've seen cases
            // where the page object is missing from the Vonfluence export (!!).
            properties.setProperty(KEY_ID, parentId);

            parent.copy(properties);
            parent.save();
        }
    }

    private static String getBodyPropertyClass(ConfluenceProperties properties)
    {
        return properties.getAttributeClass(KEY_BODY_CONTENT_CONTENT);
    }

    private void readSpaceDescriptionObject(ConfluenceObjectReader objectReader)
            throws ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        setClass(OBJECT_TYPE_SPACE_DESCRIPTION, properties);
        Long id = asLong(objectReader.readObjectProperties(properties, ConfluenceObjectFields.CONTENT));
        if (id != null) {
            readSpaceDescriptionObject(properties, id);
        }
    }

    private void readSpaceDescriptionObject(ConfluenceProperties properties, long id) throws ConfigurationException
    {
        Long spaceId = properties.getLong(KEY_PAGE_SPACE, null);
        if (spaceId != null && shouldIgnoreSpace(spaceId)) {
            return;
        }

        saveSpaceDescriptionProperties(properties, id);
    }

    private static Long asLong(Object id)
    {
       if (id instanceof Long) {
           return (Long) id;
       }

       if (id instanceof String) {
           try {
               return Long.parseLong((String) id);
           } catch (NumberFormatException e) {
               return null;
           }
       }

       return null;
    }

    private void readContentObject(ConfluenceObjectReader objectReader)
        throws ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();
        Long id = asLong(objectReader.readObjectProperties(properties, ConfluenceObjectFields.CONTENT));
        if (id == null) {
            logger.error("id is unexpectedly null, ignoring this content");
            return;
        }

        boolean isBlog = false;
        String ct = properties.getString("contenttype");
        switch (ct) {
            case "BLOGPOST":
                isBlog = true;
                /* fall-through */
            case "PAGE":
                readPageObject(properties, id, isBlog);
                break;
            case "ATTACHMENT":
                readAttachmentObject(properties, id);
                break;
            case "SPACEDESCRIPTION":
                readSpaceDescriptionObject(properties, id);
                break;
            case "COMMENT":
                readCommentObject(properties, id);
                break;
            case "GLOBALDESCRIPTION":
            case "WHITEBOARD":
            case "DATABASE":
            case "USERINFO":
            case "FOLDER":
            case "CUSTOM":
                // ignore
                break;
            default:
                logger.warn("Unknown content type [{}]", ct);
        }
    }

    private void readPageObject(ConfluenceObjectReader objectReader, boolean isBlog)
            throws ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();
        Long id = asLong(objectReader.readObjectProperties(properties, ConfluenceObjectFields.CONTENT));
        if (id == null) {
            logger.error("Page id is unexpectedly null, ignoring this object");
            return;
        }
        readPageObject(properties, id, isBlog);
    }

    private void readPageObject(ConfluenceProperties properties, long pageId, boolean isBlog)
            throws ConfigurationException
    {
        setClass(OBJECT_TYPE_PAGE, properties);
        // NOTE: historical versions of pages don't necessarily have a space id.
        Long spaceId = properties.getLong(KEY_PAGE_SPACE, null);
        if (mustIgnoreContent(properties) || (spaceId != null && shouldIgnoreSpace(spaceId))) {
            return;
        }

        long originalVersionId = getAttachmentOriginalVersionId(properties, -1);
        if (originalVersionId == -1) {
            // Register only current pages (they will take care of handling their history)
            if (spaceId == null) {
                this.logger.error("Could not find space of page [{}]. Importing it may fail.", pageId);
                return;
            }
            registerPage(isBlog, properties, pageId, spaceId);
        } else {
            saveInParent(originalVersionId, OBJECT_TYPE_PAGE, KEY_PAGE_REVISIONS, pageId);
        }

        if (isBlog) {
            properties.setProperty(KEY_PAGE_BLOGPOST, true);
        }
        savePageProperties(properties, pageId);
    }

    private static boolean mustIgnoreContent(ConfluenceProperties properties)
    {
        // Skip deleted, archived or draft pages
        // Note that some draft pages don't have spaces and this causes issues.
        String contentStatus = properties.getString(ConfluenceXMLPackage.KEY_PAGE_CONTENT_STATUS);
        return contentStatus != null && (contentStatus.equals("deleted") || contentStatus.equals("draft"));
    }

    private void registerPage(boolean isBlog, ConfluenceProperties properties, long pageId, long spaceId)
    {
        Set<Long> missingParentsForSpace = missingParents.get(spaceId);
        if (missingParentsForSpace != null) {
            missingParentsForSpace.remove(pageId);
        }

        if (!isBlog) {
            handleHomePageAndOrphans(properties, spaceId, pageId);
        }
        (isBlog ? this.blogPages : this.pages).computeIfAbsent(spaceId, k -> new LinkedList<>()).add(pageId);
        String title = properties.getString(KEY_PAGE_TITLE, null);
        if (title != null) {
            // FIXME: we could probably reuse the already existing lowerTitle property, but I don't think we can rely on
            // it being present so we'd have to have a fallback anyway
            pagesBySpaceAndLowerTitle.computeIfAbsent(spaceId, k -> new HashMap<>()).put(title.toLowerCase(), pageId);
        }
    }

    private void handleHomePageAndOrphans(ConfluenceProperties properties, Long spaceId, long pageId)
    {
        Long parent = properties.getLong(KEY_PAGE_PARENT, null);
        if (parent == null) {
            Long homePage = homePages.get(spaceId);
            if (homePage == null || !homePage.equals(pageId)) {
                orphans.computeIfAbsent(spaceId, k -> new ArrayList<>()).add(pageId);
            }
        } else {
            pageChildren.computeIfAbsent(parent, k -> new ArrayList<>()).add(pageId);
            if (!this.pages.getOrDefault(spaceId, Collections.emptyList()).contains(parent)) {
                missingParents.computeIfAbsent(spaceId, k -> new LinkedHashSet<>()).add(parent);
            }
        }
    }

    private void setHomePage(Long spaceId, long pageId)
    {
        homePages.put(spaceId, pageId);
        Collection<Long> spaceOrphans = orphans.get(spaceId);
        if (spaceOrphans != null) {
            spaceOrphans.remove(pageId);
        }
    }

    private void readLabellingObject(ConfluenceObjectReader objectReader)
        throws FilterException, ConfigurationException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        Long labellingId = asLong(objectReader.readObjectProperties(properties, ConfluenceObjectFields.CONTENT_LABEL));
        if (labellingId != null) {
            // Since Confluence 8.0, the labellings are not part of the Page Object anymore (most probably because it can be
            // associated to a space or a PageTemplate object too).
            Long parentId = properties.getLong(KEY_LABELLING_CONTENT, null);
            String parentClass = null;
            if (parentId == null) {
                parentId = properties.getLong("pageTemplate", null);
                parentClass = OBJECT_TYPE_PAGE_TEMPLATE;
            }

            if (parentId != null) {
                if (parentClass == null) {
                    parentClass = getBodyPropertyClass(properties);
                }

                if (parentClass == null) {
                    // Assume it's a page by default
                    parentClass = OBJECT_TYPE_PAGE;
                } else if (OBJECT_TYPE_ATTACHMENT.equals(parentClass)) {
                    // when what is labelled is an attachment we skip saving the labelling object altogether.
                    // We don't currently support importing attachment labels.
                    return;
                }

                ConfluenceProperties parent = getParentObjectByType(parentClass, parentId, false);

                if (parent != null && !parent.getList(KEY_LABELLINGS).contains(labellingId)) {
                    parent.addProperty(KEY_LABELLINGS, labellingId);
                    parent.save();
                }
            }

            saveObjectProperties(properties, labellingId.toString());
        }
    }

    private void readInternalUserObject(ConfluenceObjectReader objectReader)
        throws ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        Object key = objectReader.readObjectProperties(properties, null);
        if (key != null) {
            saveObjectProperties(FOLDER_INTERNALUSER, properties, key.toString());
        }
    }

    private void readUserImplObject(ConfluenceObjectReader objectReader)
        throws ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        Object key = objectReader.readObjectProperties(properties, ConfluenceObjectFields.USER_MAPPING);
        if (key != null) {
            saveObjectProperties(FOLDER_USERIMPL, properties, key.toString());
        }
    }

    private void readGroupObject(ConfluenceObjectReader objectReader)
        throws ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        Object key = objectReader.readObjectProperties(properties, null);
        if (key != null) {
            saveObjectProperties(FOLDER_GROUP, properties, key.toString());
        }
    }

    private void readMembershipObject(ConfluenceObjectReader objectReader)
        throws ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        objectReader.readObjectProperties(properties, null);

        Long parentGroup = properties.getLong("parentGroup", null);

        if (parentGroup != null) {
            ConfluenceProperties groupProperties = getGroupProperties(parentGroup);

            Long userMember = properties.getLong("userMember", null);

            if (userMember != null) {
                List<Long> users =
                    new ArrayList<>(getLongList(groupProperties, KEY_GROUP_MEMBERUSERS, Collections.emptyList()));
                users.add(userMember);
                groupProperties.setProperty(KEY_GROUP_MEMBERUSERS, users);
            }

            Long groupMember = properties.getLong("groupMember", null);

            if (groupMember != null) {
                List<Long> groups = new ArrayList<>(
                    getLongList(groupProperties, KEY_GROUP_MEMBERGROUPS, Collections.emptyList()));
                groups.add(groupMember);
                groupProperties.setProperty(KEY_GROUP_MEMBERGROUPS, groups);
            }

            saveObjectProperties(FOLDER_GROUP, groupProperties, Long.toString(parentGroup));
        }
    }

    private String escapeWindowsFolderName(String folderName)
    {
        return FileSystem.getCurrent().toLegalFileName(folderName, '_');
    }

    private File getSpacesFolder()
    {
        return new File(this.tree, "spaces");
    }

    private File getContentPermissionSetsFolder()
    {
        return new File(this.tree, KEY_CONTENT_CONTENT_PERMISSION_SETS);
    }

    private File getSpaceFolder(long spaceId)
    {
        return new File(getSpacesFolder(), String.valueOf(spaceId));
    }

    private File getSpaceDescriptorFolder()
    {
        return new File(this.tree, "spacedescriptors");
    }

    private File getPagesFolder()
    {
        return new File(this.tree, "pages");
    }

    private File getContentsFolder()
    {
        return new File(this.tree, "contents");
    }

    private File getObjectsFolder(String folderName)
    {
        return new File(this.tree, escapeWindowsFolderName(folderName));
    }

    private File getObjectFolder(File folder, String objectId)
    {
        return new File(folder, escapeWindowsFolderName(objectId));
    }

    private File getInternalUserFolder()
    {
        return getObjectsFolder(FOLDER_INTERNALUSER);
    }

    private File getUserImplFolder()
    {
        return getObjectsFolder(FOLDER_USERIMPL);
    }

    private File getGroupsFolder()
    {
        return getObjectsFolder(FOLDER_GROUP);
    }

    private File getSpaceDescriptorFolder(long spaceDescriptorId)
    {
        return new File(getSpaceDescriptorFolder(), String.valueOf(spaceDescriptorId));
    }

    private File getPageFolder(long pageId)
    {
        return new File(getPagesFolder(), String.valueOf(pageId));
    }

    private File getContentFolder(long contentId)
    {
        return new File(getContentsFolder(), String.valueOf(contentId));
    }

    private File getObjectFolder(String folderName, String objectId)
    {
        return new File(getObjectsFolder(folderName), escapeWindowsFolderName(objectId));
    }

    private File getSpaceDescriptorPropertiesFile(long spaceDescriptorId)
    {
        File folder = getSpaceDescriptorFolder(spaceDescriptorId);

        return new File(folder, PROPERTIES_FILENAME);
    }

    private File getPagePropertiesFile(long pageId)
    {
        File folder = getPageFolder(pageId);

        return new File(folder, PROPERTIES_FILENAME);
    }

    private File getTaskPropertiesFile(long pageId, long taskId)
    {
        File folder = getPageFolder(pageId);

        return new File(new File(folder, "tasks"), String.valueOf(taskId));
    }

    private File getObjectPropertiesFile(File folder, String propertyId)
    {
        return new File(getObjectFolder(folder, propertyId), PROPERTIES_FILENAME);
    }

    private File getObjectPropertiesFile(String folderName, String propertyId)
    {
        File folder = getObjectFolder(folderName, propertyId);

        return new File(folder, PROPERTIES_FILENAME);
    }

    /**
     * @param pageId the identifier of the page where the attachments are located
     * @return the attachments located in the given page
     */
    public Collection<Long> getAttachments(long pageId)
    {
        File folder = getAttachmentsFolder(pageId);

        Collection<Long> attachments;
        if (folder.exists()) {
            String[] attachmentFolders = folder.list();

            attachments = new TreeSet<>();
            for (String attachmentIdString : attachmentFolders) {
                if (NumberUtils.isCreatable(attachmentIdString)) {
                    attachments.add(Long.valueOf(attachmentIdString));
                }
            }
        } else {
            attachments = Collections.emptyList();
        }

        return attachments;
    }

    private File getAttachmentsFolder(long pageId)
    {
        return new File(getPageFolder(pageId), ATTACHMENTS);
    }

    private File getSpacePermissionFolder(long spaceId)
    {
        return new File(getSpaceFolder(spaceId), FOLDER_SPACE_PERMISSIONS);
    }

    private File getContentPermissionSetFolder(long permissionSetId)
    {
        return new File(getContentPermissionSetsFolder(), String.valueOf(permissionSetId));
    }

    private File getContentPermissionFolder(long permissionSetId)
    {
        return new File(getContentPermissionSetFolder(permissionSetId), String.valueOf(permissionSetId));
    }

    private File getAttachmentFolder(long pageId, long attachmentId)
    {
        return new File(getAttachmentsFolder(pageId), String.valueOf(attachmentId));
    }

    private File getContentPropertiesFolder(long contentId)
    {
        return new File(getContentFolder(contentId), PROPERTIES);
    }

    private File getContentPropertiesFolder(long contentId, long propertyId)
    {
        return new File(getContentPropertiesFolder(contentId), String.valueOf(propertyId));
    }

    private File getSpacePageTemplateFolder(long templateId)
    {
        return new File(new File(this.tree, FOLDER_SPACE_PAGE_TEMPLATE), String.valueOf(templateId));
    }

    private File getSpacePermissionFolder(long spaceId, long permissionId)
    {
        return new File(getSpacePermissionFolder(spaceId), String.valueOf(permissionId));
    }

    private File getContentPermissionFolder(long permissionSetId, long permissionId)
    {
        return new File(getContentPermissionFolder(permissionSetId), String.valueOf(permissionId));
    }

    private File getAttachmentPropertiesFile(long pageId, long attachmentId)
    {
        File folder = getAttachmentFolder(pageId, attachmentId);

        return new File(folder, PROPERTIES_FILENAME);
    }

    private File getContentPropertiesFile(long contentId, long propertyId)
    {
        File folder = getContentPropertiesFolder(contentId, propertyId);

        return new File(folder, PROPERTIES_FILENAME);
    }

    private File getSpacePageTemplatePropertiesFile(long templateId)
    {
        File folder = getSpacePageTemplateFolder(templateId);

        return new File(folder, PROPERTIES_FILENAME);
    }


    private File getSpacePermissionPropertiesFile(long spaceId, long permissionId)
    {
        File folder = getSpacePermissionFolder(spaceId, permissionId);

        return new File(folder, PROPERTIES_FILENAME);
    }

    private File getContentPermissionSetPropertiesFile(long permissionSetId)
    {
        File folder = getContentPermissionSetFolder(permissionSetId);

        return new File(folder, PROPERTIES_FILENAME);
    }

    private File getContentPermissionPropertiesFile(long permissionSetId, long permissionId)
    {
        File folder = getContentPermissionFolder(permissionSetId, permissionId);

        return new File(folder, PROPERTIES_FILENAME);
    }

    private File getSpacePropertiesFile(long spaceId)
    {
        File folder = getSpaceFolder(spaceId);

        return new File(folder, PROPERTIES_FILENAME);
    }

    /**
     * @param spaceDescriptorId the identifier of the space descriptor
     * @param create true of the properties should be created when they don't exist
     * @return the properties containing information about the page
     * @throws ConfigurationException when failing to create the properties
     */
    public ConfluenceProperties getSpaceDescriptorProperties(long spaceDescriptorId, boolean create)
        throws ConfigurationException
    {
        File file = getSpaceDescriptorPropertiesFile(spaceDescriptorId);

        return getObjectProperties(Long.toString(spaceDescriptorId), file, create, true);
    }

    /**
     * @param pageId the identifier of the page
     * @param create true of the properties should be created when they don't exist
     * @return the properties containing information about the page
     * @throws ConfigurationException when failing to create the properties
     */
    public ConfluenceProperties getPageProperties(long pageId, boolean create) throws ConfigurationException
    {
        File file = getPagePropertiesFile(pageId);
        ConfluenceProperties props = getObjectProperties(Long.toString(pageId), file, create, true);
        if (!create && (props == null || props.getLong(ConfluenceXMLPackage.KEY_ID, null) == null)) {
            // Null ID can happen when the home page is missing. ConfluenceXMLPackage has set the homePage property
            // when parsing the space, but the page id and any other property is missing. This means the page
            // isn't actually there and we should not return it
            return null;
        }
        return props;
    }

    /**
     * @param pageId the identifier of the page
     * @param taskId the local identifier of the task
     * @param create true of the properties should be created when they don't exist
     * @return the properties containing information about the task
     * @throws ConfigurationException when failing to create the properties
     */
    private ConfluenceProperties getTaskProperties(long pageId, long taskId, boolean create)
            throws ConfigurationException
    {
        File file = getTaskPropertiesFile(pageId, taskId);
        return (create || file.exists()) ? ConfluenceProperties.create(file) : null;
    }

    /**
     * @param objectId the identifier of the object
     * @return the properties containing information about the object
     * @throws ConfigurationException when failing to create the properties
     */
    public ConfluenceProperties getObjectProperties(Long objectId) throws ConfigurationException
    {
        return getObjectProperties(FOLDER_OBJECTS, objectId);
    }

    /**
     * @param folder the folder where the object properties are stored
     * @param objectId the identifier of the object
     * @return the properties containing information about the object
     * @throws ConfigurationException when failing to create the properties
     */
    public ConfluenceProperties getObjectProperties(String folder, Long objectId) throws ConfigurationException
    {
        if (objectId == null) {
            return null;
        }

        return getObjectProperties(folder, objectId.toString(), true);
    }

    /**
     * @param folder the folder where the object properties are stored
     * @param objectId the identifier of the object
     * @param create true if the properties should be created when they don't exist
     * @return the properties containing information about the object
     * @throws ConfigurationException when failing to create the properties
     */
    public ConfluenceProperties getObjectProperties(String folder, String objectId, boolean create)
        throws ConfigurationException
    {
        return getObjectProperties(folder, objectId, create, true);
    }

    private ConfluenceProperties getObjectProperties(String folder, String objectId, boolean create,
        boolean lookForBody) throws ConfigurationException
    {
        if (objectId == null) {
            return null;
        }

        return getObjectProperties(objectId, getObjectPropertiesFile(folder, objectId), create, lookForBody);
    }

    private ConfluenceProperties getObjectProperties(String objectId, File propertiesFile, boolean create,
        boolean lookForBody) throws ConfigurationException
    {
        ConfluenceProperties confluenceProperties =
            (create || propertiesFile.exists()) ? ConfluenceProperties.create(propertiesFile) : null;
        if (confluenceProperties != null && lookForBody) {
            String body = confluenceProperties.getString(KEY_PAGE_BODY);
            if (body == null) {
                ConfluenceProperties bodyContentProperties = getBodyContentProperties(objectId, false);
                if (bodyContentProperties != null) {
                    confluenceProperties.copy(bodyContentProperties);
                }
            }
        }
        return confluenceProperties;
    }

    /**
     * @param userId the identifier of the user
     * @return the properties containing information about the user
     * @throws ConfigurationException when failing to create the properties
     */
    public ConfluenceProperties getInternalUserProperties(Long userId) throws ConfigurationException
    {
        return getObjectProperties(FOLDER_INTERNALUSER, userId);
    }

    /**
     * @param userKey the key of the user
     * @return the properties containing information about the user
     * @throws ConfigurationException when failing to create the properties
     */
    public ConfluenceProperties getUserImplProperties(String userKey) throws ConfigurationException
    {
        return getObjectProperties(FOLDER_USERIMPL, userKey, false);
    }

    /**
     * @param userIdOrKey the identifier or key of the user
     * @return the properties containing information about the user
     * @throws ConfigurationException when failing to create the properties
     */
    public ConfluenceProperties getUserProperties(String userIdOrKey) throws ConfigurationException
    {
        ConfluenceProperties properties = getUserImplProperties(userIdOrKey);

        if (properties == null && NumberUtils.isCreatable(userIdOrKey)) {
            properties = getInternalUserProperties(NumberUtils.createLong(userIdOrKey));
        }

        return properties;
    }

    /**
     * @param id the Confluence object id.
     * @return the list of ids of the object's ancestors, from the root to the object, excluded.
     * @throws ConfigurationException when something bad happens
     * @since 9.35.0
     */
    public List<Long> getAncestors(Long id) throws ConfigurationException
    {
        if (id == null) {
            return Collections.emptyList();
        }

        String idStr = Long.toString(id);

        Long parent = getObjectParent(idStr);
        if (parent == null) {
            return new ArrayList<>();
        }

        List<Long> ancestors = getAncestors(parent);
        if (ancestors != null) {
            ancestors.add(parent);
        }

        return ancestors;
    }

    private Long getObjectParent(String id) throws ConfigurationException
    {
        ConfluenceProperties properties = getConfluenceProperties(id);
        if (properties == null) {
            return null;
        }

        for (String parentProperty : PARENT_PROPERTIES) {
            long parentId = properties.getLong(parentProperty, -1L);
            if (parentId != -1) {
                return parentId;
            }
        }

        return null;
    }

    private ConfluenceProperties getConfluenceProperties(String objectId) throws ConfigurationException
    {
        File objectFolder = findObjectFolder(this.tree, objectId);
        if (objectFolder == null) {
            return null;
        }

        return getObjectProperties(objectId, getObjectPropertiesFile(objectFolder, objectId), false, true);
    }

    /**
     * @param folder the folder inside which to search.
     * @param id the Confluence object id.
     * @return the sub-folder of the object in this folder.
     * @since 9.35.0
     */
    private File findObjectFolder(File folder, String id)
    {
        if (!folder.isDirectory()) {
            return null;
        }

        String[] list = folder.list();
        if (list.length == 0) {
            return null;
        }

        char firstChar = list[0].charAt(0);
        if (firstChar >= '0' && firstChar <= '9') {
            // we assume the folder contains objects.
            for (String child : list) {
                if (id.equals(child)) {
                    return folder;
                }
            }
        }

        // the folder contains sub-folders
        for (String child : list) {
            File p = findObjectFolder(new File(folder, child), id);
            if (p != null) {
                return p;
            }
        }

        return null;
    }

    /**
     * @param key the user key
     * @param def the value to return if the user was not found
     * @since 9.26.0
     * @return the resolved user
     */
    public String resolveUserName(String key, String def)
    {
        try {
            ConfluenceProperties userProperties = getUserProperties(key);

            if (userProperties != null) {
                String userName = userProperties.getString(ConfluenceXMLPackage.KEY_USER_NAME);

                if (userName != null) {
                    return userName;
                }
            }
        } catch (ConfigurationException e) {
            this.logger.warn("Failed to retrieve properties of user with key [{}]: {}", key,
                ExceptionUtils.getRootCauseMessage(e));
        }

        return def;
    }

    /**
     * @return users stored with class InternalUser (with long id)
     */
    public Collection<Long> getInternalUsers()
    {
        File folder = getInternalUserFolder();

        if (folder.exists()) {
            String[] userFolders = folder.list();

            if (userFolders != null) {
                Collection<Long> users = new TreeSet<>();
                for (String userIdString : userFolders) {
                    if (NumberUtils.isCreatable(userIdString)) {
                        users.add(Long.valueOf(userIdString));
                    }
                }
                return users;
            }
        }

        return Collections.emptyList();
    }

    /**
     * @return users stored with class ConfluenceUserImpl (with String keys)
     */
    public Collection<String> getUsersImpl()
    {
        File folder = getUserImplFolder();

        if (folder.exists()) {
            String[] list = folder.list();
            if (list != null) {
                return new TreeSet<>(Arrays.asList(list));
            }
        }

        return Collections.emptyList();
    }

    /**
     * @return the groups found in the package
     */
    public Collection<Long> getGroups()
    {
        File folder = getGroupsFolder();

        if (folder.exists()) {
            String[] groupFolders = folder.list();
            if (groupFolders != null) {
                Collection<Long> groups = new TreeSet<>();
                for (String groupIdString : groupFolders) {
                    if (NumberUtils.isCreatable(groupIdString)) {
                        groups.add(Long.valueOf(groupIdString));
                    }
                }
                return groups;
            }
        }

        return Collections.emptyList();
    }

    /**
     * @param groupId the identifier of the group
     * @return the properties containing information about the group
     * @throws ConfigurationException when failing to create the properties
     */
    public ConfluenceProperties getGroupProperties(Long groupId) throws ConfigurationException
    {
        return getObjectProperties(FOLDER_GROUP, groupId);
    }

    /**
     * @param pageId the identifier of the page where the attachment is located
     * @param attachmentId the identifier of the attachment
     * @return the properties containing information about the attachment
     * @throws ConfigurationException when failing to create the properties
     */
    public ConfluenceProperties getAttachmentProperties(long pageId, long attachmentId) throws ConfigurationException
    {
        File file = getAttachmentPropertiesFile(pageId, attachmentId);

        return ConfluenceProperties.create(file);
    }

    /**
     * @param contentId the identifier of the content related to the property
     * @param propertyId the identifier of the property
     * @return the properties containing information about the content property
     * @throws ConfigurationException when failing to create the properties
     */
    public ConfluenceProperties getContentProperties(long contentId, long propertyId, boolean create)
            throws ConfigurationException
    {
        File file = getContentPropertiesFile(contentId, propertyId);
        if (!create && !file.exists()) {
            file = getContentPropertiesFile(-1, propertyId);
        }

        return ConfluenceProperties.create(file);
    }

    /**
     * @param ignored the identifier of the space
     * @param templateId the identifier of the page template
     * @param create whether to create the properties if they don't exist yet
     * @return the properties containing information about the page template
     * @throws ConfigurationException when failing to create the properties
     * @since 9.86.0
     */
    public ConfluenceProperties getSpacePageTemplateProperties(long ignored, long templateId, boolean create)
        throws ConfigurationException
    {
        return getSpacePageTemplateProperties(templateId, create);
    }

    /**
     * @param templateId the identifier of the page template
     * @param create whether to create the properties if they don't exist yet
     * @return the properties containing information about the page template
     * @throws ConfigurationException when failing to create the properties
     * @since 9.79.0
     */
    public ConfluenceProperties getSpacePageTemplateProperties(long templateId, boolean create)
        throws ConfigurationException
    {
        File file = getSpacePageTemplatePropertiesFile(templateId);

        return getObjectProperties(Long.toString(templateId), file, create, true);
    }

    /**
     * @param spaceId the identifier of the space
     * @param permissionId the identifier of the permission
     * @return the properties containing information about the space permission
     * @throws ConfigurationException when failing to create the properties
     * @since 9.24.0
     */
    public ConfluenceProperties getSpacePermissionProperties(long spaceId, long permissionId)
        throws ConfigurationException
    {
        File file = getSpacePermissionPropertiesFile(spaceId, permissionId);

        return ConfluenceProperties.create(file);
    }

    /**
     * @param permissionSetId the identifier of the permission set
     * @param permissionId the identifier of the permission
     * @return the properties containing information about the permission
     * @throws ConfigurationException when failing to create the properties
     * @since 9.24.0
     */
    public ConfluenceProperties getContentPermissionProperties(long permissionSetId, long permissionId)
            throws ConfigurationException
    {
        File file = getContentPermissionPropertiesFile(permissionSetId, permissionId);

        return ConfluenceProperties.create(file);
    }

    /**
     * @param permissionSetId the identifier of the permission set
     * @return the properties containing information about the permission set
     * @throws ConfigurationException when failing to create the properties
     */
    public ConfluenceProperties getContentPermissionSetProperties(long permissionSetId)
            throws ConfigurationException
    {
        File file = getContentPermissionSetPropertiesFile(permissionSetId);

        return ConfluenceProperties.create(file);
    }

    /**
     * @param spaceId the identifier of the space
     * @return the properties containing information about the space
     * @throws ConfigurationException when failing to create the properties
     */
    public ConfluenceProperties getSpaceProperties(long spaceId) throws ConfigurationException
    {
        File file = getSpacePropertiesFile(spaceId);

        return ConfluenceProperties.create(file);
    }

    private void saveSpaceDescriptionProperties(ConfluenceProperties properties, long spaceDescriptorId)
        throws ConfigurationException
    {
        ConfluenceProperties fileProperties = getSpaceDescriptorProperties(spaceDescriptorId, true);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void savePageProperties(ConfluenceProperties properties, long pageId) throws ConfigurationException
    {
        ConfluenceProperties fileProperties = getPageProperties(pageId, true);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void saveObjectProperties(ConfluenceProperties properties, String objectId) throws ConfigurationException
    {
        saveObjectProperties(FOLDER_OBJECTS, properties, objectId);
    }

    private void saveObjectProperties(String folder, ConfluenceProperties properties, String objectKey)
        throws ConfigurationException
    {
        ConfluenceProperties fileProperties = getObjectProperties(folder, objectKey, true);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void saveAttachmentProperties(ConfluenceProperties properties, long pageId, long attachmentId)
        throws ConfigurationException
    {
        ConfluenceProperties fileProperties = getAttachmentProperties(pageId, attachmentId);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void saveContentPropertyProperties(ConfluenceProperties properties, long contentId, long propertyId)
        throws ConfigurationException
    {
        ConfluenceProperties fileProperties = getContentProperties(contentId, propertyId, true);

        fileProperties.copy(properties);

        fileProperties.save();
    }


    private void saveSpacePageTemplateProperties(ConfluenceProperties properties, long spaceId, long templateId)
        throws ConfigurationException
    {
        ConfluenceProperties fileProperties = getSpacePageTemplateProperties(spaceId, templateId, true);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void saveSpacePermissionProperties(ConfluenceProperties properties, long spaceId, long permissionId)
        throws ConfigurationException
    {
        ConfluenceProperties fileProperties = getSpacePermissionProperties(spaceId, permissionId);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void saveContentPermissionSetProperties(ConfluenceProperties properties, long permissionSetId)
            throws ConfigurationException
    {
        ConfluenceProperties fileProperties = getContentPermissionSetProperties(permissionSetId);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void saveContentPermissionProperties(ConfluenceProperties properties,
        long contentPermissionSetId, long permissionId) throws ConfigurationException
    {
        ConfluenceProperties fileProperties = getContentPermissionProperties(contentPermissionSetId, permissionId);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void saveSpaceProperties(ConfluenceProperties properties, long spaceId) throws ConfigurationException
    {
        ConfluenceProperties fileProperties = getSpaceProperties(spaceId);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    /**
     * @return the main file of the Confluence package
     */
    public File getEntities()
    {
        return this.entities;
    }

    /**
     * @return the file containing information about the Confluence instance
     */
    public File getDescriptor()
    {
        return this.descriptor;
    }

    /**
     * @return the value of the given field in the descriptor file, null if not found
     * @param field the field to get.
     * NOTE: Results are not cached, please don't repeatedly call.
     * @since 9.50.0
     */
    public String getDescriptorField(String field)
    {
        if (this.descriptor == null) {
            return null;
        }

        try (Scanner scanner = new Scanner(this.descriptor)) {
            String lineStart = field + '=';
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith(lineStart)) {
                    return line.substring(lineStart.length()).trim();
                }
            }
        } catch (FileNotFoundException e) {
            // ignore
        }
        return null;
    }

    /**
     * @return the Confluence instance type declared in the descriptor file (for XML backups),
     *         or "cloud" if the backup is a CSV backup, or null if this can't be determined.
     */
    public String getConfluenceInstanceType()
    {
        return isCSV() ? "cloud" : getDescriptorField("source");
    }

    /**
     * @return whether the backup package is actually a CSV backup (from Confluence Cloud)
     * We currently determine this by trying to find the spaces.csv.gz file.
     */
    public boolean isCSV()
    {
        return ConfluenceCSVFile.exists(this.directory, "spaces");
    }

    /**
     * Read the tasks.
     *
     * @throws FilterException if there's an issue reading the tasks
     *
     * NOTE: Calling this method is not mandatory. It is only needed if you want the tasks to be read at some specific
     *       time (e.g. to have logs in consistant order).
     *       the tasks are currently found in the AO_BAF3AA_AOINLINE_TASK.csv file. This method could evolve to return
     *       tasks from other places as well in the future if we find out other places listing tasks)
     */
    public void readTasks() throws FilterException
    {
        if (this.tasks == null) {
            return;
        }

        if (!this.tasks.exists()) {
            this.tasks = null;
            return;
        }

        try {
            mkdirTree();
            logger.info("Reading tasks...");
            this.tasks.readRecords(this::readTask);
            logger.info("Finished reading tasks.");
            this.tasks = null;
        } catch (ConfluenceCanceledException | ConfigurationException | IOException e) {
            throw new FilterException("Failed to read tasks", e);
        }
    }

    private void readTask(ConfluenceObjectReader reader)
    {
        ConfluenceCSVFile r = (ConfluenceCSVFile) reader;
        try {
            long contentId = Long.parseLong(r.get(CONTENT_ID));
            long id = Long.parseLong(r.get(ID));
            ConfluenceProperties taskProperties = getTaskProperties(contentId, id, true);
            if (taskProperties == null) {
                logger.error("Task properties are unexpectedly null. This should not happen.");
                return;
            }
            for (String field : TASK_FIELDS) {
                taskProperties.setProperty(field, r.get(field));
            }
            taskProperties.save();
        } catch (NumberFormatException e) {
            logger.error("Failed to parse task [{}], some details may be missing when importing it", r);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the requested task from the AO_BAF3AA_AOINLINE_TASK.csv file or possibly from elsewhere in the future
     * @param pageId the id of the page in which the task is
     * @param taskId the local id of the task
     * @throws FilterException if something wrong happens while reading the task properties file
     */
    public ConfluenceTask getTask(long pageId, int taskId) throws FilterException
    {
        try {
            readTasks();
            ConfluenceProperties taskProperties = getTaskProperties(pageId, taskId, false);
            if (taskProperties == null) {
                return null;
            }

            return new PropertiesConfluenceTask(taskProperties);
        } catch (ConfigurationException e) {
            throw new FilterException("Failed to read task", e);
        }
    }

    /**
     * @param stablePageId the stable identifier of the page were the attachment is located
     * @param stableAttachmentId the identifier of the attachment
     * @param version the version of the attachment
     * @return the file containing the attachment content
     * @throws FileNotFoundException when failing to find the attachment content file
     * @deprecated
     */
    @Deprecated(since = "9.96.0")
    public File getAttachmentFile(long stablePageId, long stableAttachmentId, long version) throws FileNotFoundException
    {
        try {
            ConfluenceProperties attachmentProperties = getAttachmentProperties(stablePageId, stableAttachmentId);
            return getAttachmentFile(stablePageId, attachmentProperties);
        } catch (ConfigurationException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    // Attachments are sometimes found at:
    //  - attachments/<stablePageId>/<stableAttachmentId>/<version> (xml backups)
    //  - attachments/<stablePageId>/<attachmentId>/<version> (CSV space backups)
    private File getAttachmentFileUsingAttachmentId(long stablePageId, long attachmentId, long stableAttachmentId,
        long version) throws FileNotFoundException
    {
        File attachmentsFolder = new File(this.directory, ATTACHMENTS);
        File attachmentsPageFolder = new File(attachmentsFolder, String.valueOf(stablePageId));

        File attachmentFolder = new File(attachmentsPageFolder, String.valueOf(attachmentId));
        File file = new File(attachmentFolder, String.valueOf(version));
        if (file.exists()) {
            return file;
        }

        File stableAttachmentFolder = new File(attachmentsPageFolder, String.valueOf(stableAttachmentId));
        file = new File(stableAttachmentFolder, String.valueOf(version));
        if (file.exists()) {
            return file;
        }

        throw new FileNotFoundException(file.getAbsolutePath());
    }

    public File getAttachmentFile(long stablePageId, ConfluenceProperties attachmentProperties)
            throws FileNotFoundException
    {
        try {
            ConfluenceProperties contentProperties = getContentProperties(attachmentProperties, KEY_CONTENTPROPERTIES);
            if (contentProperties != null) {
                return getAttachmentFile(stablePageId, attachmentProperties, contentProperties);
            }
        } catch (ConfigurationException e) {
            throw new FileNotFoundException(e.getMessage());
        }

        throw new FileNotFoundException();
    }

    public File getAttachmentFile(long stablePageId, ConfluenceProperties attachmentProperties,
            ConfluenceProperties contentProperties) throws FileNotFoundException
    {
        String filename = contentProperties.getString("FILESTORE_ID", "");
        if (StringUtils.isNotEmpty(filename)) {
            File attachmentsFolder = new File(this.directory, ATTACHMENTS);
            File file = new File(attachmentsFolder, filename);
            if (file.exists()) {
                return file;
            }
        }

        long attachmentId = attachmentProperties.getLong(KEY_ID, -1);
        long stableAttachmentId = getAttachmentOriginalVersionId(attachmentProperties, -1);
        if (stableAttachmentId == -1) {
            stableAttachmentId = attachmentId;
            if (stableAttachmentId == -1) {
                throw new FileNotFoundException("Could not determine the ID of the attachment object");
            }
        }

        long version = getAttachementVersion(attachmentProperties);
        return getAttachmentFileUsingAttachmentId(stablePageId, attachmentId, stableAttachmentId, version);
    }

    /**
     * Free any temporary resource used by the package.
     * 
     * @throws IOException when failing to close the package
     */
    @Override
    public void close() throws IOException
    {
        logger.info("Closing the Confluence package.");
        if (this.tree != null) {
            FileUtils.deleteDirectory(this.tree);
        }

        if (this.temporaryDirectory && this.directory.exists()) {
            FileUtils.deleteDirectory(this.directory);
        }
        logger.info("Closed the Confluence package.");
    }

    /**
     * Free any temporary resource used by the package.
     * @param async whether this should be done asynchronously in a separate thread, so it doesn't block the current
     *              main operation.
     * @throws IOException when something bad happens
     */
    public void close(boolean async) throws IOException
    {
        if (async) {
            CompletableFuture.runAsync(() -> {
                try {
                    close();
                } catch (IOException e) {
                    logger.error("Something went wrong while closing the Confluence package", e);
                }
            });
        } else {
            close();
        }
    }

    /**
     * @param attachmentProperties the properties containing attachment information
     * @return the name of the attachment
     */
    public String getAttachmentName(ConfluenceProperties attachmentProperties)
    {
        String attachmentName = attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_TITLE, null);
        if (attachmentName == null) {
            attachmentName = attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_NAME);
        }

        return attachmentName;
    }

    /**
     * @param attachmentProperties the properties containing attachment information
     * @return the version of the attachment
     */
    public Long getAttachementVersion(ConfluenceProperties attachmentProperties)
    {
        Long version = attachmentProperties.getLong(ConfluenceXMLPackage.KEY_ATTACHMENT_VERSION, null);
        if (version == null) {
            version = attachmentProperties.getLong(ConfluenceXMLPackage.KEY_ATTACHMENT_ATTACHMENTVERSION, null);
        }

        return version;
    }

    /**
     * @param attachmentProperties the properties containing attachment information
     * @param def the default value to return in can of error
     * @return the identifier of the attachment original version
     */
    public long getAttachmentOriginalVersionId(ConfluenceProperties attachmentProperties, long def)
    {
        Long originalRevisionId =
            attachmentProperties.getLong(ConfluenceXMLPackage.KEY_ATTACHMENT_ORIGINALVERSIONID, null);
        return originalRevisionId != null ? originalRevisionId
            : attachmentProperties.getLong(ConfluenceXMLPackage.KEY_ATTACHMENT_ORIGINALVERSION, def);
    }

    /**
     * @param labellingProperties the properties containing the tag information
     * @return the name of the tag
     */
    public String getTagName(ConfluenceProperties labellingProperties)
    {
        Long tagId = labellingProperties.getLong(ConfluenceXMLPackage.KEY_LABELLING_LABEL, null);
        if (tagId == null) {
            return null;
        }
        String tagName = tagId.toString();

        try {
            ConfluenceProperties labelProperties = getObjectProperties(tagId);
            tagName = labelProperties.getString(ConfluenceXMLPackage.KEY_LABEL_NAME);
        } catch (NumberFormatException | ConfigurationException e) {
            logger.error("Unable to get tag name, using id [{}] instead.", tagId, e);
        }

        return tagName;
    }

    /**
     * @param commentId the identifier of the comment
     * @return the content of the comment
     */
    public String getCommentText(Long commentId)
    {
        String commentText = commentId.toString();
        try {
            ConfluenceProperties commentProperties = getObjectProperties(commentId);
            if (commentProperties == null) {
                logger.warn("Unable to get comment text for comment [{}], using id instead.", commentId);
            } else {
                commentText = getCommentText(commentProperties);
            }
        } catch (ConfigurationException e) {
            logger.error("Failed to get comment text for comment [{}], using id instead.", commentId, e);
        }

        return commentText;
    }

    /**
     * @param commentProperties the comment properties
     * @return the content of the comment
     * @since 9.47.0
     */
    public String getCommentText(ConfluenceProperties commentProperties)
    {
        return commentProperties.getString(KEY_PAGE_BODY);
    }

    /**
     * @param commentId the identifier of the comment
     * @return the type of the comment content
     */
    public Integer getCommentBodyType(Long commentId)
    {
        int bodyType = -1;
        try {
            ConfluenceProperties commentProperties = getObjectProperties(commentId);
            if (commentProperties == null) {
                logger.warn("Unable to get comment body type for comment [{}].", commentId);
            } else {
                bodyType = getCommentBodyType(commentProperties);
            }
        } catch (ConfigurationException e) {
            logger.error("Failed to get comment body type for comment [{}].", commentId, e);
        }

        return bodyType;
    }

    /**
     * @param commentProperties the comment properties
     * @return the content of the comment
     * @since 9.47.0
     */
    public int getCommentBodyType(ConfluenceProperties commentProperties)
    {
        return commentProperties.getInt(KEY_PAGE_BODY_TYPE);
    }

    /**
     * @param properties the properties to parse
     * @param key the key
     * @param def the default value in case of error
     * @return the long value corresponding to the key or default
     * @deprecated since 9.96.0
     */
    @Deprecated(since = "9.96.0")
    public static Long getLong(ConfluenceProperties properties, String key, Long def)
    {
        return properties.getLong(key, def);
    }
}
