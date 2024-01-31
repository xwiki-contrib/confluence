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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.confluence.filter.internal.WithoutControlCharactersReaders;
import org.xwiki.environment.Environment;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.FileInputSource;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.InputStreamInputSource;
import org.xwiki.filter.input.URLInputSource;
import org.xwiki.job.event.status.JobProgressManager;
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
     * The property key to access the space permissions.
     * @since 9.24.0
     */
    public static final String KEY_SPACE_PERMISSIONS = "permissions";

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
     * The property key to access the page lebellings.
     */
    public static final String KEY_PAGE_LABELLINGS = "labellings";

    /**
     * The property key to access the page comments.
     */
    public static final String KEY_PAGE_COMMENTS = "comments";

    /**
     * Old property to indicate attachment name.
     *
     * @see #KEY_ATTACHMENT_TITLE
     */
    public static final String KEY_ATTACHMENT_NAME = "fileName";

    /**
     * The property key to access the attachment title.
     */
    public static final String KEY_ATTACHMENT_TITLE = "title";

    /**
     * Old field containing attachment page id.
     *
     * @see #KEY_ATTACHMENT_CONTAINERCONTENT
     */
    public static final String KEY_ATTACHMENT_CONTENT = "content";

    /**
     * The property key to access the attachment content key.
     */
    public static final String KEY_ATTACHMENT_CONTAINERCONTENT = "containerContent";

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
     * The property key to access the attachment content properties.
     */
    public static final String KEY_ATTACHMENT_CONTENTPROPERTIES = "contentProperties";

    /**
     * The property key to access the attachment content status.
     */
    public static final String KEY_ATTACHMENT_CONTENTSTATUS = "contentStatus";

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
     * The property key to access the attachment creation author.
     */
    public static final String KEY_ATTACHMENT_CREATION_AUTHOR = "creatorName";

    /**
     * The property key to access the attachment creation date.
     */
    public static final String KEY_ATTACHMENT_CREATION_DATE = "creationDate";

    /**
     * The property key to access the attachment revision author.
     */
    public static final String KEY_ATTACHMENT_REVISION_AUTHOR = "lastModifierName";

    /**
     * The property key to access the attachment revision date.
     */
    public static final String KEY_ATTACHMENT_REVISION_DATE = "lastModificationDate";

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
    public static final String KEY_ATTACHMENT_VERSION = "version";

    /**
     * Old property to indicate attachment original revision.
     *
     * @see #KEY_ATTACHMENT_ORIGINALVERSIONID
     */
    public static final String KEY_ATTACHMENT_ORIGINALVERSION = "originalVersion";

    /**
     * The property key to access the attachment original version.
     */
    public static final String KEY_ATTACHMENT_ORIGINALVERSIONID = "originalVersionId";

    /**
     * The property key to access the attachment DTO.
     */
    public static final String KEY_ATTACHMENT_DTO = "imageDetailsDTO";

    /**
     * The property key to access the label name.
     */
    public static final String KEY_LABEL_NAME = "name";

    /**
     * The property key to access the label id.
     */
    public static final String KEY_LABELLING_LABEL = "label";

    /**
     * The property key to access the group name.
     */
    public static final String KEY_GROUP_NAME = "name";

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
    public static final String KEY_GROUP_DESCRIPTION = "description";

    /**
     * The property key to access the group members.
     */
    public static final String KEY_GROUP_MEMBERUSERS = "memberusers";

    /**
     * The property key to access the group members.
     */
    public static final String KEY_GROUP_MEMBERGROUPS = "membergroups";

    /**
     * The property key to access the user name.
     */
    public static final String KEY_USER_NAME = "name";

    /**
     * The property key to access the user active status.
     */
    public static final String KEY_USER_ACTIVE = "active";

    /**
     * The property key to access the user creation date.
     */
    public static final String KEY_USER_CREATION_DATE = "createdDate";

    /**
     * The property key to access the user revision date.
     */
    public static final String KEY_USER_REVISION_DATE = "updatedDate";

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
     * The property key to access the blog post page.
     *
     * @since 9.24.0
     */
    public static final String KEY_PAGE_BLOGPOST = "blogpost";

    /**
     * The date format in a Confluence package (2012-03-07 17:16:48.158).
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * Pattern to find the end of "intentionally damaged" CDATA end sections. Confluence does this to nest CDATA
     * sections inside CDATA sections. Interestingly it does not care if there is a &gt; after the ]].
     */
    private static final Pattern FIND_BROKEN_CDATA_PATTERN = Pattern.compile("]] ");

    /**
     * Replacement to repair the CDATA.
     */
    private static final String REPAIRED_CDATA_END = "]]";

    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    private static final String FOLDER_INTERNALUSER = "internalusers";

    private static final String FOLDER_USERIMPL = "userimpls";

    private static final String FOLDER_GROUP = "groups";

    private static final String PROPERTIES_FILENAME = "properties.properties";

    private static final String SPACE_STATUS_ARCHIVED = "ARCHIVED";

    private static final Collection<String> SUPPORTED_OBJECTS = new HashSet<>(Arrays.asList(
        "Attachment",
        "BlogPost",
        "BodyContent",
        "Comment",
        "ContentPermission",
        "ContentPermissionSet",
        "ContentProperty",
        "InternalGroup",
        "InternalUser",
        "Label",
        "Labelling",
        "Page",
        "Space",
        "SpaceDescription",
        "SpacePermission"
    ));

    @Inject
    private Environment environment;

    @Inject
    private JobProgressManager progress;

    @Inject
    private Logger logger;

    private File directory;

    /**
     * Indicate if {@link #directory} is temporary (extracted from a source package).
     */
    private boolean temporaryDirectory;

    private File entities;

    private File descriptor;

    private File tree;

    private Map<Long, List<Long>> pages = new LinkedHashMap<>();

    private Map<Long, List<Long>> blogPages = new LinkedHashMap<>();

    private Map<String, Long> spacesByKey = new HashMap<>();

    /**
     * @param source the source where to find the package to parse
     * @throws IOException when failing to access the package content
     * @throws FilterException when any error happen during the reading of the package
     */
    public void read(InputSource source) throws IOException, FilterException
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
            try {
                if (source instanceof InputStreamInputSource) {
                    fromStream((InputStreamInputSource) source);
                } else {
                    throw new FilterException(
                        String.format("Unsupported input source of type [%s]", source.getClass().getName()));
                }
            } finally {
                source.close();
            }
        }

        this.entities = new File(this.directory, FILE_ENTITIES);
        this.descriptor = new File(this.directory, FILE_DESCRIPTOR);

        // Initialize

        try {
            createTree();
        } catch (Exception e) {
            throw new FilterException("Failed to analyze the package index", e);
        }
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

        DateFormat format = new SimpleDateFormat(DATE_FORMAT);

        return str != null ? format.parse(str) : null;
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
            return null;
        }

        ConfluenceProperties contentProperties = new ConfluenceProperties();
        for (Long element : elements) {
            ConfluenceProperties contentProperty = getObjectProperties(element);
            if (contentProperty != null) {
                String name = contentProperty.getString("name");
                if (name == null) {
                    logger.warn("ContentProperty [{}] was not found", element);
                    continue;
                }

                Object value = contentProperty.getString("longValue", null);
                if (Strings.isNullOrEmpty((String) value)) {
                    value = contentProperty.getString("dateValue", null);
                    if (Strings.isNullOrEmpty((String) value)) {
                        value = contentProperty.getString("stringValue", null);
                    } else {
                        // TODO: dateValue
                    }
                } else {
                    value = contentProperty.getLong("longValue", null);
                }

                contentProperties.setProperty(name, value);
            }
        }

        return contentProperties;

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
     */
    public Map<String, Long> getSpacesByKey()
    {
        return spacesByKey;
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

    private void createTree()
        throws XMLStreamException, FactoryConfigurationError, IOException, ConfigurationException, FilterException
    {
        if (this.temporaryDirectory) {
            this.tree = new File(this.directory, "tree");
        } else {
            this.tree = Files
                .createTempDirectory(this.environment.getTemporaryDirectory().toPath(), "confluencexml-tree").toFile();
        }
        this.tree.mkdir();

        try (CountingInputStream s = new CountingInputStream(new BufferedInputStream(new FileInputStream(entities)))) {
            XMLStreamReader xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(new WithoutControlCharactersReaders(s));

            xmlReader.nextTag();

            long size = entities.length();
            int steps = 100;
            progress.pushLevelProgress(steps, this);
            boolean inStep = false;
            long stepSize = size / steps;
            long nextStepPos = stepSize;
            for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
                if (!inStep) {
                    progress.startStep(this);
                    inStep = true;
                }
                String elementName = xmlReader.getLocalName();

                if (elementName.equals("object")) {
                    readObject(xmlReader);
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
            progress.popLevelProgress(this);
        }
    }

    private void readObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        String type = xmlReader.getAttributeValue(null, "class");

        if (type != null) {
            if (type.equals("Page")) {
                readPageObject(xmlReader);
            } else if (type.equals("Space")) {
                readSpaceObject(xmlReader);
            } else if (type.equals("InternalUser")) {
                readInternalUserObject(xmlReader);
            } else if (type.equals("ConfluenceUserImpl")) {
                readUserImplObject(xmlReader);
            } else if (type.equals("InternalGroup")) {
                readGroupObject(xmlReader);
            } else if (type.equals("HibernateMembership")) {
                readMembershipObject(xmlReader);
            } else if (type.equals("BodyContent")) {
                readBodyContentObject(xmlReader);
            } else if (type.equals("SpaceDescription")) {
                readSpaceDescriptionObject(xmlReader);
            } else if (type.equals("SpacePermission")) {
                readSpacePermissionObject(xmlReader);
            } else if (type.equals("ContentPermission")) {
                readContentPermissionObject(xmlReader);
            } else if (type.equals("ContentPermissionSet")) {
                readContentPermissionSetObject(xmlReader);
            } else if (type.equals("Attachment")) {
                readAttachmentObject(xmlReader);
            } else if (type.equals("BlogPost")) {
                readBlogPostObject(xmlReader);
            } else {
                ConfluenceProperties properties = new ConfluenceProperties();

                long id = readObjectProperties(xmlReader, properties);

                saveObjectProperties(properties, id);
            }
        }
    }

    private long readObjectProperties(XMLStreamReader xmlReader, ConfluenceProperties properties)
        throws XMLStreamException, FilterException
    {
        long id = -1;

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            if (elementName.equals("id")) {
                String idName = xmlReader.getAttributeValue(null, "name");

                if (idName != null && idName.equals("id")) {
                    id = Long.parseLong(xmlReader.getElementText());

                    properties.setProperty("id", id);
                } else {
                    StAXUtils.skipElement(xmlReader);
                }
            } else if (elementName.equals("collection")) {
                String propertyName = xmlReader.getAttributeValue(null, "name");

                properties.setProperty(propertyName, readListProperty(xmlReader));
            } else if (elementName.equals("property")) {
                String propertyName = xmlReader.getAttributeValue(null, "name");

                properties.setProperty(propertyName, readProperty(xmlReader));
            } else {
                StAXUtils.skipElement(xmlReader);
            }
        }

        return id;
    }

    private String readImplObjectProperties(XMLStreamReader xmlReader, ConfluenceProperties properties)
        throws XMLStreamException, FilterException
    {
        String id = "-1";

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            if (elementName.equals("id")) {
                String idName = xmlReader.getAttributeValue(null, "name");

                if (idName != null && idName.equals("key")) {
                    id = fixCData(xmlReader.getElementText());

                    properties.setProperty("id", id);
                } else {
                    StAXUtils.skipElement(xmlReader);
                }
            } else if (elementName.equals("collection")) {
                String propertyName = xmlReader.getAttributeValue(null, "name");

                properties.setProperty(propertyName, readListProperty(xmlReader));
            } else if (elementName.equals("property")) {
                String propertyName = xmlReader.getAttributeValue(null, "name");

                properties.setProperty(propertyName, readProperty(xmlReader));
            } else {
                StAXUtils.skipElement(xmlReader);
            }
        }

        return id;
    }

    private void readAttachmentObject(XMLStreamReader xmlReader)
        throws XMLStreamException, FilterException, ConfigurationException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        long attachmentId = readObjectProperties(xmlReader, properties);

        Long pageId = getAttachmentPageId(properties);

        if (pageId != null) {
            saveAttachmentProperties(properties, pageId, attachmentId);
        }
    }

    private Long getAttachmentPageId(ConfluenceProperties properties)
    {
        Long pageId = getLong(properties, KEY_ATTACHMENT_CONTAINERCONTENT, null);

        if (pageId == null) {
            pageId = properties.getLong(KEY_ATTACHMENT_CONTENT, null);
        }

        return pageId;
    }

    private void readSpaceObject(XMLStreamReader xmlReader)
        throws XMLStreamException, FilterException, ConfigurationException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        long spaceId = readObjectProperties(xmlReader, properties);

        saveSpaceProperties(properties, spaceId);

        // Register space by id
        this.pages.computeIfAbsent(spaceId, k -> new LinkedList<Long>());

        // Register space by key
        String spaceKey = properties.getString("key");
        if (spaceKey != null) {
            this.spacesByKey.put(spaceKey, spaceId);
        }
    }

    private void readSpaceDescriptionObject(XMLStreamReader xmlReader)
        throws XMLStreamException, FilterException, ConfigurationException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        long descriptionId = readObjectProperties(xmlReader, properties);

        properties.setProperty(KEY_PAGE_HOMEPAGE, true);

        savePageProperties(properties, descriptionId);
    }

    private void readSpacePermissionObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        long permissionId = readObjectProperties(xmlReader, properties);

        Long spaceId = properties.getLong("space", null);
        if (spaceId != null) {
            saveSpacePermissionProperties(properties, spaceId, permissionId);
        }
    }

    private void readContentPermissionObject(XMLStreamReader xmlReader)
        throws XMLStreamException, FilterException, ConfigurationException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        long permissionId = readObjectProperties(xmlReader, properties);

        Long contentPermissionSetId = properties.getLong("owningSet", null);
        if (contentPermissionSetId != null) {
            saveContentPermissionProperties(properties, contentPermissionSetId, permissionId);
        }
    }

    private void readContentPermissionSetObject(XMLStreamReader xmlReader)
        throws XMLStreamException, FilterException, ConfigurationException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        long permissionId = readObjectProperties(xmlReader, properties);

        saveContentPermissionSetProperties(properties, permissionId);
    }

    private void readBodyContentObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();
        properties.disableListDelimiter();

        readObjectProperties(xmlReader, properties);

        Long pageId = properties.getLong("content", null);
        if (pageId != null) {
            savePageProperties(properties, pageId);
        }
    }

    private void readPageObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        long pageId = readObjectProperties(xmlReader, properties);

        savePageProperties(properties, pageId);

        // Register only current pages (they will take care of handling their history)
        Long originalVersion = (Long) properties.getProperty("originalVersion");
        if (originalVersion == null) {
            Long spaceId = properties.getLong("space", null);
            List<Long> spacePages = this.pages.computeIfAbsent(spaceId, k -> new LinkedList<>());
            spacePages.add(pageId);
        }
    }

    private void readBlogPostObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        properties.setProperty(KEY_PAGE_BLOGPOST, true);

        long pageId = readObjectProperties(xmlReader, properties);

        savePageProperties(properties, pageId);

        // Register only current pages (they will take care of handling there history)
        Long originalVersion = (Long) properties.getProperty("originalVersion");
        if (originalVersion == null) {
            Long spaceId = properties.getLong("space", null);
            this.blogPages.computeIfAbsent(spaceId, k -> new LinkedList<>()).add(pageId);
        }
    }

    private void readInternalUserObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        long pageId = readObjectProperties(xmlReader, properties);

        saveObjectProperties(FOLDER_INTERNALUSER, properties, pageId);
    }

    private void readUserImplObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        String pageId = readImplObjectProperties(xmlReader, properties);

        saveObjectProperties(FOLDER_USERIMPL, properties, pageId);
    }

    private void readGroupObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        long pageId = readObjectProperties(xmlReader, properties);

        saveObjectProperties(FOLDER_GROUP, properties, pageId);
    }

    private void readMembershipObject(XMLStreamReader xmlReader)
        throws ConfigurationException, XMLStreamException, FilterException
    {
        ConfluenceProperties properties = new ConfluenceProperties();

        readObjectProperties(xmlReader, properties);

        Long parentGroup = properties.getLong("parentGroup", null);

        if (parentGroup != null) {
            ConfluenceProperties groupProperties = getGroupProperties(parentGroup);

            Long userMember = properties.getLong("userMember", null);

            if (userMember != null) {
                List<Long> users =
                    new ArrayList<>(getLongList(groupProperties, KEY_GROUP_MEMBERUSERS, Collections.<Long>emptyList()));
                users.add(userMember);
                groupProperties.setProperty(KEY_GROUP_MEMBERUSERS, users);
            }

            Long groupMember = properties.getLong("groupMember", null);

            if (groupMember != null) {
                List<Long> groups = new ArrayList<>(
                    getLongList(groupProperties, KEY_GROUP_MEMBERGROUPS, Collections.<Long>emptyList()));
                groups.add(groupMember);
                groupProperties.setProperty(KEY_GROUP_MEMBERGROUPS, groups);
            }

            saveObjectProperties(FOLDER_GROUP, groupProperties, parentGroup);
        }
    }

    private Object readProperty(XMLStreamReader xmlReader) throws XMLStreamException, FilterException
    {
        String propertyClass = xmlReader.getAttributeValue(null, "class");

        Object result = null;
        if (propertyClass == null) {
            try {
                return fixCData(xmlReader.getElementText());
            } catch (XMLStreamException e) {
                // Probably an empty element
            }
        } else if (propertyClass.equals("java.util.List") || propertyClass.equals("java.util.Collection")) {
            return readListProperty(xmlReader);
        } else if (propertyClass.equals("java.util.Set")) {
            return readSetProperty(xmlReader);
        } else if (SUPPORTED_OBJECTS.contains(propertyClass)) {
            return readObjectReference(xmlReader);
        } else if (propertyClass.equals("ConfluenceUserImpl")) {
            return readImplObjectReference(xmlReader);
        }

        StAXUtils.skipElement(xmlReader);

        return result;
    }

    /**
     * To protect content with cdata section inside cdata elements confluence adds a single space after two
     * consecutive curly braces. we need to undo this patch as otherwise the content parser will complain about invalid
     * content. Strictly speaking this needs only to be done for string valued properties.
     */
    private String fixCData(String elementText)
    {
        if (elementText == null) {
            return elementText;
        }
        return FIND_BROKEN_CDATA_PATTERN.matcher(elementText).replaceAll(REPAIRED_CDATA_END);
    }

    private Long readObjectReference(XMLStreamReader xmlReader) throws FilterException, XMLStreamException
    {
        xmlReader.nextTag();

        if (!xmlReader.getLocalName().equals("id")) {
            throw new FilterException(
                String.format("Was expecting id element but found [%s]", xmlReader.getLocalName()));
        }

        Long id = Long.valueOf(xmlReader.getElementText());

        xmlReader.nextTag();

        return id;
    }

    private String readImplObjectReference(XMLStreamReader xmlReader) throws FilterException, XMLStreamException
    {
        xmlReader.nextTag();

        if (!xmlReader.getLocalName().equals("id")) {
            throw new FilterException(
                String.format("Was expecting id element but found [%s]", xmlReader.getLocalName()));
        }

        String key = fixCData(xmlReader.getElementText());

        xmlReader.nextTag();

        return key;
    }

    private List<Object> readListProperty(XMLStreamReader xmlReader) throws XMLStreamException, FilterException
    {
        List<Object> list = new ArrayList<>();

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            list.add(readProperty(xmlReader));
        }

        return list;
    }

    private Set<Object> readSetProperty(XMLStreamReader xmlReader) throws XMLStreamException, FilterException
    {
        Set<Object> set = new LinkedHashSet<>();

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            set.add(readProperty(xmlReader));
        }

        return set;
    }

    private File getSpacesFolder()
    {
        return new File(this.tree, "spaces");
    }

    private File getContentPermissionSetsFolder()
    {
        return new File(this.tree, "contentPermissionSets");
    }

    private File getSpaceFolder(long spaceId)
    {
        return new File(getSpacesFolder(), String.valueOf(spaceId));
    }

    private File getPagesFolder()
    {
        return new File(this.tree, "pages");
    }

    private File getObjectsFolder(String folderName)
    {
        return new File(this.tree, folderName);
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

    private File getPageFolder(long pageId)
    {
        return new File(getPagesFolder(), String.valueOf(pageId));
    }

    private File getObjectFolder(String folderName, long objectId)
    {
        return new File(getObjectsFolder(folderName), String.valueOf(objectId));
    }

    private File getObjectFolder(String folderName, String objectId)
    {
        return new File(getObjectsFolder(folderName), objectId);
    }

    private File getPagePropertiesFile(long pageId)
    {
        File folder = getPageFolder(pageId);

        return new File(folder, PROPERTIES_FILENAME);
    }

    private File getObjectPropertiesFile(String folderName, long propertyId)
    {
        File folder = getObjectFolder(folderName, propertyId);

        return new File(folder, PROPERTIES_FILENAME);
    }

    private File getObjectPropertiesFile(String folderName, String propertyId)
    {
        File folder = getObjectFolder(folderName, propertyId);

        return new File(folder, PROPERTIES_FILENAME);
    }

    /**
     * @param pageId the identifier of the page where the attachments are located
     * @return the attachments located in the passed page
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
        return new File(getPageFolder(pageId), "attachments");
    }

    private File getSpacePermissionFolder(long spaceId)
    {
        return new File(getSpaceFolder(spaceId), "permissions");
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
     * @param pageId the identifier of the page
     * @param create true of the properties should be created when they don't exist
     * @return the properties containing information about the page
     * @throws ConfigurationException when failing to create the properties
     */
    public ConfluenceProperties getPageProperties(long pageId, boolean create) throws ConfigurationException
    {
        File file = getPagePropertiesFile(pageId);

        return create || file.exists() ? ConfluenceProperties.create(file) : null;
    }

    /**
     * @param objectId the identifier of the object
     * @return the properties containing information about the object
     * @throws ConfigurationException when failing to create the properties
     */
    public ConfluenceProperties getObjectProperties(Long objectId) throws ConfigurationException
    {
        return getObjectProperties("objects", objectId);
    }

    /**
     * @param folder the folder where the object properties are stored
     * @param objectId the identifier of the object
     * @return the properties containing information about the object
     * @throws ConfigurationException when failing to create the properties
     */
    public ConfluenceProperties getObjectProperties(String folder, Long objectId) throws ConfigurationException
    {
        long id;
        if (objectId != null) {
            id = objectId;
        } else {
            return null;
        }

        File file = getObjectPropertiesFile(folder, id);

        return ConfluenceProperties.create(file);
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
        if (objectId == null) {
            return null;
        }

        File file = getObjectPropertiesFile(folder, objectId);

        return create || file.exists() ? ConfluenceProperties.create(file) : null;
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

        Collection<Long> users;
        if (folder.exists()) {
            String[] userFolders = folder.list();

            users = new TreeSet<>();
            for (String userIdString : userFolders) {
                if (NumberUtils.isCreatable(userIdString)) {
                    users.add(Long.valueOf(userIdString));
                }
            }
        } else {
            users = Collections.emptyList();
        }

        return users;
    }

    /**
     * @return users stored with class ConfluenceUserImpl (with String keys)
     */
    public Collection<String> getUsersImpl()
    {
        File folder = getUserImplFolder();

        Collection<String> users;
        if (folder.exists()) {
            String[] userFolders = folder.list();

            users = new TreeSet<>();
            for (String userIdString : userFolders) {
                users.add(userIdString);
            }
        } else {
            users = Collections.emptyList();
        }

        return users;
    }

    /**
     * @return the groups found in the package
     */
    public Collection<Long> getGroups()
    {
        File folder = getGroupsFolder();

        Collection<Long> groups;
        if (folder.exists()) {
            String[] groupFolders = folder.list();

            groups = new TreeSet<>();
            for (String groupIdString : groupFolders) {
                if (NumberUtils.isCreatable(groupIdString)) {
                    groups.add(Long.valueOf(groupIdString));
                }
            }
        } else {
            groups = Collections.emptyList();
        }

        return groups;
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

    private void savePageProperties(ConfluenceProperties properties, long pageId) throws ConfigurationException
    {
        ConfluenceProperties fileProperties = getPageProperties(pageId, true);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void saveObjectProperties(ConfluenceProperties properties, long objectId) throws ConfigurationException
    {
        saveObjectProperties("objects", properties, objectId);
    }

    private void saveObjectProperties(String folder, ConfluenceProperties properties, long objectId)
        throws ConfigurationException
    {
        ConfluenceProperties fileProperties = getObjectProperties(folder, objectId);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void saveObjectProperties(String folder, ConfluenceProperties properties, String objectId)
        throws ConfigurationException
    {
        ConfluenceProperties fileProperties = getObjectProperties(folder, objectId, true);

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
     * @param pageId the identifier of the page were the attachment is located
     * @param attachmentId the identifier of the attachment
     * @param version the version of the attachment
     * @return the file containing the attachment content
     * @throws FileNotFoundException when failing to find the attachment content file
     */
    public File getAttachmentFile(long pageId, long attachmentId, long version) throws FileNotFoundException
    {
        File attachmentsFolder = new File(this.directory, "attachments");
        File attachmentsPageFolder = new File(attachmentsFolder, String.valueOf(pageId));
        File attachmentFolder = new File(attachmentsPageFolder, String.valueOf(attachmentId));

        // In old version the file name is the version
        File file = new File(attachmentFolder, String.valueOf(version));

        if (file.exists()) {
            return file;
        }

        // In recent version the name is always 1
        file = new File(attachmentFolder, "1");

        if (file.exists()) {
            return file;
        }

        throw new FileNotFoundException(file.getAbsolutePath());
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
     * @param async whether this should be done asynchronously in a separate thread so it doesn't block the current main
     *              operation.
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
        Long version = getLong(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_VERSION, null);
        if (version == null) {
            version = getLong(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_ATTACHMENTVERSION, null);
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
            getLong(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_ORIGINALVERSIONID, null);
        return originalRevisionId != null ? originalRevisionId
            : getLong(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_ORIGINALVERSION, def);
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
            // BodyContent objects are stored in page properties under the content id
            ConfluenceProperties commentContent = getPageProperties(commentId, false);
            if (commentContent == null) {
                logger.warn("Unable to get comment text for comment [{}], using id instead.", commentId);
            } else {
                commentText = commentContent.getString(KEY_PAGE_BODY);
            }
        } catch (ConfigurationException e) {
            logger.error("Unable to get comment text for comment [{}], using id instead.", commentId, e);
        }

        return commentText;
    }

    /**
     * @param commentId the identifier of the comment
     * @return the type of the comment content
     */
    public Integer getCommentBodyType(Long commentId)
    {
        Integer bodyType = -1;
        try {
            ConfluenceProperties commentContent = getPageProperties(commentId, false);
            if (commentContent == null) {
                logger.warn("Unable to get comment body type for comment [{}].", commentId);
            } else {
                bodyType = commentContent.getInt(KEY_PAGE_BODY_TYPE);
            }
        } catch (ConfigurationException e) {
            logger.error("Unable to get comment body type for comment [{}].", commentId, e);
        }

        return bodyType;
    }

    /**
     * @param properties the properties to parse
     * @param key the key
     * @param def the default value in case of error
     * @return the long value corresponding to the key or default
     */
    public static Long getLong(ConfluenceProperties properties, String key, Long def)
    {
        try {
            return properties.getLong(key, def);
        } catch (Exception e) {
            // Usually mean the field does not have the expected format

            return def;
        }
    }
}
