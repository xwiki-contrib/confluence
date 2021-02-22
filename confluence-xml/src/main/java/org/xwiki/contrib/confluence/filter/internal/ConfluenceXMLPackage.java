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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.FileInputSource;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.InputStreamInputSource;
import org.xwiki.filter.input.URLInputSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.xml.stax.StAXUtils;

import com.google.common.base.Strings;

/**
 * @version $Id$
 * @since 9.0
 */
public class ConfluenceXMLPackage
{
    public static final String FILE_ENTITIES = "entities.xml";

    public static final String FILE_DESCRIPTOR = "exportDescriptor.properties";

    public static final String KEY_SPACE_NAME = "name";

    public static final String KEY_SPACE_KEY = "key";

    public static final String KEY_SPACE_DESCRIPTION = "description";

    public static final String KEY_PAGE_HOMEPAGE = "homepage";

    public static final String KEY_PAGE_PARENT = "parent";

    public static final String KEY_PAGE_SPACE = "space";

    public static final String KEY_PAGE_TITLE = "title";

    public static final String KEY_PAGE_CONTENTS = "bodyContents";

    public static final String KEY_PAGE_CREATION_AUTHOR = "creatorName";

    public static final String KEY_PAGE_CREATION_AUTHOR_KEY = "creator";

    public static final String KEY_PAGE_CREATION_DATE = "creationDate";

    public static final String KEY_PAGE_REVISION = "version";

    public static final String KEY_PAGE_REVISION_AUTHOR_KEY = "lastModifier";

    public static final String KEY_PAGE_REVISION_AUTHOR = "lastModifierName";

    public static final String KEY_PAGE_REVISION_DATE = "lastModificationDate";

    public static final String KEY_PAGE_REVISION_COMMENT = "versionComment";

    public static final String KEY_PAGE_REVISIONS = "historicalVersions";

    public static final String KEY_PAGE_CONTENT_STATUS = "contentStatus";

    public static final String KEY_PAGE_BODY = "body";

    public static final String KEY_PAGE_BODY_TYPE = "bodyType";

    public static final String KEY_PAGE_LABELLINGS = "labellings";

    public static final String KEY_PAGE_COMMENTS = "comments";

    /**
     * Old property to indicate attachment name.
     * 
     * @see #KEY_ATTACHMENT_TITLE
     */
    public static final String KEY_ATTACHMENT_NAME = "fileName";

    public static final String KEY_ATTACHMENT_TITLE = "title";

    /**
     * Old field containing attachment page id.
     * 
     * @see #KEY_ATTACHMENT_CONTAINERCONTENT
     */
    public static final String KEY_ATTACHMENT_CONTENT = "content";

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

    public static final String KEY_ATTACHMENT_CONTENTPROPERTIES = "contentProperties";

    public static final String KEY_ATTACHMENT_CONTENTSTATUS = "contentStatus";

    public static final String KEY_ATTACHMENT_CONTENT_MINOR_EDIT = "MINOR_EDIT";

    public static final String KEY_ATTACHMENT_CONTENT_FILESIZE = "FILESIZE";

    public static final String KEY_ATTACHMENT_CONTENT_MEDIA_TYPE = "MEDIA_TYPE";

    public static final String KEY_ATTACHMENT_CREATION_AUTHOR = "creatorName";

    public static final String KEY_ATTACHMENT_CREATION_DATE = "creationDate";

    public static final String KEY_ATTACHMENT_REVISION_AUTHOR = "lastModifierName";

    public static final String KEY_ATTACHMENT_REVISION_DATE = "lastModificationDate";

    public static final String KEY_ATTACHMENT_REVISION_COMMENT = "comment";

    /**
     * Old property to indicate attachment revision.
     * 
     * @see #KEY_ATTACHMENT_VERSION
     */
    public static final String KEY_ATTACHMENT_ATTACHMENTVERSION = "attachmentVersion";

    public static final String KEY_ATTACHMENT_VERSION = "version";

    /**
     * Old property to indicate attachment original revision.
     * 
     * @see #KEY_ATTACHMENT_ORIGINALVERSIONID
     */
    public static final String KEY_ATTACHMENT_ORIGINALVERSION = "originalVersion";

    public static final String KEY_ATTACHMENT_ORIGINALVERSIONID = "originalVersionId";

    public static final String KEY_ATTACHMENT_DTO = "imageDetailsDTO";

    public static final String KEY_LABEL_NAME = "name";

    public static final String KEY_LABELLING_LABEL = "label";

    public static final String KEY_GROUP_NAME = "name";

    public static final String KEY_GROUP_ACTIVE = "active";

    public static final String KEY_GROUP_LOCAL = "local";

    public static final String KEY_GROUP_CREATION_DATE = "createdDate";

    public static final String KEY_GROUP_REVISION_DATE = "updatedDate";

    public static final String KEY_GROUP_DESCRIPTION = "description";

    public static final String KEY_GROUP_MEMBERUSERS = "memberusers";

    public static final String KEY_GROUP_MEMBERGROUPS = "membergroups";

    public static final String KEY_USER_NAME = "name";

    public static final String KEY_USER_ACTIVE = "active";

    public static final String KEY_USER_CREATION_DATE = "createdDate";

    public static final String KEY_USER_REVISION_DATE = "updatedDate";

    public static final String KEY_USER_FIRSTNAME = "firstName";

    public static final String KEY_USER_LASTNAME = "lastName";

    public static final String KEY_USER_DISPLAYNAME = "displayName";

    public static final String KEY_USER_EMAIL = "emailAddress";

    public static final String KEY_USER_PASSWORD = "credential";

    /**
     * 2012-03-07 17:16:48.158
     */
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * pattern to find the end of "intentionally damaged" CDATA end sections. Confluence does this to nest CDATA
     * sections inside CDATA sections. Interestingly it does not care if there is a &gt; after the ]].
     */
    private static final Pattern FIND_BROKEN_CDATA_PATTERN = Pattern.compile("]] ");

    /**
     * replacement to repair the CDATA
     */
    private static final String REPAIRED_CDATA_END = "]]";

    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    private static final String FOLDER_INTERNALUSER = "internalusers";

    private static final String FOLDER_USERIMPL = "userimpls";

    private static final String FOLDER_GROUP = "groups";

    protected static final Logger LOGGER = LoggerFactory.getLogger(ConfluenceXMLPackage.class);

    private File directory;

    private File entities;

    private File descriptor;

    private boolean temporaryDirectory;

    private File tree;

    private Map<Long, List<Long>> pages = new LinkedHashMap<>();

    private Map<String, Long> spacesByKey = new HashMap<>();

    public ConfluenceXMLPackage(InputSource source) throws IOException, FilterException, XMLStreamException,
        FactoryConfigurationError, ConfigurationException, URISyntaxException
    {
        if (source instanceof FileInputSource) {
            fromFile(((FileInputSource) source).getFile());
        } else if (source instanceof URLInputSource
            && ((URLInputSource) source).getURL().getProtocol().equals("file")) {
            fromFile(new File(((URLInputSource) source).getURL().toURI()));
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

        createTree();
    }

    private void fromFile(File file) throws XMLStreamException
    {
        if (file.isDirectory()) {
            this.directory = file;
        } else {
            try (FileInputStream stream = new FileInputStream(file)) {
                fromStream(stream);
            } catch (IOException e) {
                throw new XMLStreamException(String.format("Failed to read Confluence package in file [%s]", file), e);
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
        this.directory = File.createTempFile("confluencexml", "");
        this.directory.delete();
        this.directory.mkdir();
        this.temporaryDirectory = false;

        // Extract the zip
        ZipArchiveInputStream zais = new ZipArchiveInputStream(stream);
        for (ZipArchiveEntry zipEntry = zais.getNextZipEntry(); zipEntry != null; zipEntry = zais.getNextZipEntry()) {
            if (!zipEntry.isDirectory()) {
                String path = zipEntry.getName();
                File file = new File(this.directory, path);

                FileUtils.copyInputStreamToFile(new CloseShieldInputStream(zais), file);
            }
        }
    }

    private PropertiesConfiguration newProperties()
    {
        PropertiesConfiguration properties = new PropertiesConfiguration();

        properties.setDelimiterParsingDisabled(true);

        return properties;
    }

    public Date getDate(PropertiesConfiguration properties, String key) throws ParseException
    {
        String str = properties.getString(key);

        return str != null ? DATE_FORMAT.parse(str) : null;
    }

    public List<Long> getLongList(PropertiesConfiguration properties, String key)
    {
        return getLongList(properties, key, null);
    }

    public List<Long> getLongList(PropertiesConfiguration properties, String key, List<Long> def)
    {
        List<Object> list = properties.getList(key, null);

        if (list == null) {
            return def;
        }

        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        if (list.get(0) instanceof Long) {
            return (List) list;
        }

        List<Long> integerList = new ArrayList<>(list.size());
        for (Object element : list) {
            integerList.add(Long.valueOf(element.toString()));
        }

        return integerList;
    }

    public PropertiesConfiguration getContentProperties(PropertiesConfiguration properties, String key)
        throws ConfigurationException
    {
        List<Long> elements = getLongList(properties, key);

        if (elements == null) {
            return null;
        }

        PropertiesConfiguration contentProperties = new PropertiesConfiguration();
        for (Long element : elements) {
            PropertiesConfiguration contentProperty = getObjectProperties(element);
            if (contentProperty != null) {
                String name = contentProperty.getString("name");

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

    public EntityReference getReferenceFromId(PropertiesConfiguration currentProperties, String key)
        throws ConfigurationException, FilterException
    {
        Long pageId = currentProperties.getLong(key, null);
        if (pageId != null) {
            PropertiesConfiguration pageProperties = getPageProperties(pageId, true);

            long spaceId = pageProperties.getLong(KEY_PAGE_SPACE);
            String pageTitle = pageProperties.getString(KEY_PAGE_TITLE);

            if (StringUtils.isNotEmpty(pageTitle)) {
                long currentSpaceId = currentProperties.getLong(KEY_PAGE_SPACE);

                EntityReference spaceReference = null;
                if (spaceId != currentSpaceId) {
                    String spaceName = getSpaceKey(currentSpaceId);
                    if (spaceName != null) {
                        spaceReference = new EntityReference(spaceName, EntityType.SPACE);
                    }
                }

                return new EntityReference(pageTitle, EntityType.DOCUMENT, spaceReference);
            } else {
                throw new FilterException("Cannot create a reference to the page with id [" + pageId
                    + "] because it does not have any title");
            }
        }

        return null;
    }

    public String getSpaceName(long spaceId) throws ConfigurationException
    {
        PropertiesConfiguration spaceProperties = getSpaceProperties(spaceId);

        return spaceProperties.getString(KEY_SPACE_NAME);
    }

    public static String getSpaceName(PropertiesConfiguration spaceProperties)
    {
        String key = spaceProperties.getString(KEY_SPACE_NAME);

        return key != null ? key : spaceProperties.getString(KEY_SPACE_KEY);
    }

    public String getSpaceKey(long spaceId) throws ConfigurationException
    {
        PropertiesConfiguration spaceProperties = getSpaceProperties(spaceId);

        return spaceProperties.getString(KEY_SPACE_KEY);
    }

    public static String getSpaceKey(PropertiesConfiguration spaceProperties)
    {
        String key = spaceProperties.getString(KEY_SPACE_KEY);

        return key != null ? key : spaceProperties.getString(KEY_SPACE_NAME);
    }

    public Map<Long, List<Long>> getPages()
    {
        return this.pages;
    }

    private void createTree()
        throws XMLStreamException, FactoryConfigurationError, IOException, ConfigurationException, FilterException
    {
        if (this.temporaryDirectory) {
            this.tree = new File(this.directory, "tree");
        } else {
            this.tree = File.createTempFile("confluencexml-tree", "");
            this.tree.delete();
        }
        this.tree.mkdir();

        try (InputStream stream = new FileInputStream(getEntities())) {
            XMLStreamReader xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(stream);

            xmlReader.nextTag();

            for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
                String elementName = xmlReader.getLocalName();

                if (elementName.equals("object")) {
                    readObject(xmlReader);
                } else {
                    StAXUtils.skipElement(xmlReader);
                }
            }
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
            } else if (type.equals("Attachment")) {
                readAttachmentObject(xmlReader);
            } else {
                PropertiesConfiguration properties = newProperties();

                long id = readObjectProperties(xmlReader, properties);

                // Save page
                saveObjectProperties(properties, id);
            }
        }
    }

    private long readObjectProperties(XMLStreamReader xmlReader, PropertiesConfiguration properties)
        throws XMLStreamException, FilterException
    {
        long id = -1;

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            if (elementName.equals("id")) {
                String idName = xmlReader.getAttributeValue(null, "name");

                if (idName != null && idName.equals("id")) {
                    id = Long.valueOf(xmlReader.getElementText());

                    properties.setProperty("id", id);
                } else {
                    StAXUtils.skipElement(xmlReader);
                }
            } else if (elementName.equals("property") || elementName.equals("collection")) {
                String propertyName = xmlReader.getAttributeValue(null, "name");

                properties.setProperty(propertyName, readProperty(xmlReader));
            } else {
                StAXUtils.skipElement(xmlReader);
            }
        }

        return id;
    }

    private String readImplObjectProperties(XMLStreamReader xmlReader, PropertiesConfiguration properties)
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
            } else if (elementName.equals("property") || elementName.equals("collection")) {
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
        PropertiesConfiguration properties = newProperties();

        long attachmentId = readObjectProperties(xmlReader, properties);

        Long pageId = getAttachmentPageId(properties);

        if (pageId != null) {
            // Save attachment
            saveAttachmentProperties(properties, pageId, attachmentId);
        }
    }

    private Long getAttachmentPageId(PropertiesConfiguration properties)
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
        PropertiesConfiguration properties = newProperties();

        long spaceId = readObjectProperties(xmlReader, properties);

        // Save page
        saveSpaceProperties(properties, spaceId);

        // Register space by id
        List<Long> spacePages = this.pages.get(spaceId);
        if (spacePages == null) {
            spacePages = new LinkedList<>();
            this.pages.put(spaceId, spacePages);
        }

        // Register space by key
        String spaceKey = properties.getString("key");
        if (spaceKey != null) {
            this.spacesByKey.put(spaceKey, spaceId);
        }
    }

    private void readSpaceDescriptionObject(XMLStreamReader xmlReader)
        throws XMLStreamException, FilterException, ConfigurationException
    {
        PropertiesConfiguration properties = newProperties();

        long descriptionId = readObjectProperties(xmlReader, properties);

        properties.setProperty(KEY_PAGE_HOMEPAGE, true);

        // Save page
        savePageProperties(properties, descriptionId);
    }

    private void readSpacePermissionObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        PropertiesConfiguration properties = newProperties();

        long permissionId = readObjectProperties(xmlReader, properties);

        Long spaceId = properties.getLong("space", null);
        if (spaceId != null) {
            // Save attachment
            saveSpacePermissionsProperties(properties, spaceId, permissionId);
        }
    }

    private void readBodyContentObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        PropertiesConfiguration properties = newProperties();
        properties.setDelimiterParsingDisabled(true);

        readObjectProperties(xmlReader, properties);

        Long pageId = properties.getLong("content", null);
        if (pageId != null) {
            savePageProperties(properties, pageId);
        }
    }

    private void readPageObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        PropertiesConfiguration properties = newProperties();

        long pageId = readObjectProperties(xmlReader, properties);

        // Save page
        savePageProperties(properties, pageId);

        // Register only current pages (they will take care of handling there history)
        Long originalVersion = (Long) properties.getProperty("originalVersion");
        if (originalVersion == null) {
            Long spaceId = properties.getLong("space", null);
            List<Long> spacePages = this.pages.get(spaceId);
            if (spacePages == null) {
                spacePages = new LinkedList<>();
                this.pages.put(spaceId, spacePages);
            }
            spacePages.add(pageId);
        }
    }

    private void readInternalUserObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        PropertiesConfiguration properties = newProperties();

        long pageId = readObjectProperties(xmlReader, properties);

        // Save page
        saveObjectProperties(FOLDER_INTERNALUSER, properties, pageId);
    }

    private void readUserImplObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        PropertiesConfiguration properties = newProperties();

        String pageId = readImplObjectProperties(xmlReader, properties);

        // Save page
        saveObjectProperties(FOLDER_USERIMPL, properties, pageId);
    }

    private void readGroupObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        PropertiesConfiguration properties = newProperties();

        long pageId = readObjectProperties(xmlReader, properties);

        // Save page
        saveObjectProperties(FOLDER_GROUP, properties, pageId);
    }

    private void readMembershipObject(XMLStreamReader xmlReader)
        throws ConfigurationException, XMLStreamException, FilterException
    {
        PropertiesConfiguration properties = newProperties();

        readObjectProperties(xmlReader, properties);

        Long parentGroup = properties.getLong("parentGroup", null);

        if (parentGroup != null) {
            PropertiesConfiguration groupProperties = getGroupProperties(parentGroup);

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

        if (propertyClass == null) {
            return fixCData(xmlReader.getElementText());
        } else if (propertyClass.equals("java.util.List") || propertyClass.equals("java.util.Collection")) {
            return readListProperty(xmlReader);
        } else if (propertyClass.equals("java.util.Set")) {
            return readSetProperty(xmlReader);
        } else if (propertyClass.equals("Page") || propertyClass.equals("Space") || propertyClass.equals("BodyContent")
            || propertyClass.equals("Attachment") || propertyClass.equals("SpaceDescription")
            || propertyClass.equals("Labelling") || propertyClass.equals("Label")
            || propertyClass.equals("SpacePermission") || propertyClass.equals("InternalGroup")
            || propertyClass.equals("InternalUser") || propertyClass.equals("Comment")
            || propertyClass.equals("ContentProperty")) {
            return readObjectReference(xmlReader);
        } else if (propertyClass.equals("ConfluenceUserImpl")) {
            return readImplObjectReference(xmlReader);
        } else {
            StAXUtils.skipElement(xmlReader);
        }

        return null;
    }

    /**
     * to protect content with cdata section inside of cdata elements confluence adds a single space after two
     * consecutive curly braces. we need to undo this patch as otherwise the content parser will complain about invalid
     * content. strictly speaking this needs only to be done for string valued properties
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

    private File getIternalUserFolder()
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

        return new File(folder, "properties.properties");
    }

    private File getObjectPropertiesFile(String folderName, long propertyId)
    {
        File folder = getObjectFolder(folderName, propertyId);

        return new File(folder, "properties.properties");
    }

    private File getObjectPropertiesFile(String folderName, String propertyId)
    {
        File folder = getObjectFolder(folderName, propertyId);

        return new File(folder, "properties.properties");
    }

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

    private File getSpacePermissionsFolder(long spaceId)
    {
        return new File(getSpaceFolder(spaceId), "permissions");
    }

    private File getAttachmentFolder(long pageId, long attachmentId)
    {
        return new File(getAttachmentsFolder(pageId), String.valueOf(attachmentId));
    }

    private File getSpacePermissionFolder(long spaceId, long permissionId)
    {
        return new File(getSpacePermissionsFolder(spaceId), String.valueOf(permissionId));
    }

    private File getAttachmentPropertiesFile(long pageId, long attachmentId)
    {
        File folder = getAttachmentFolder(pageId, attachmentId);

        return new File(folder, "properties.properties");
    }

    private File getSpacePermissionPropertiesFile(long spaceId, long permissionId)
    {
        File folder = getSpacePermissionFolder(spaceId, permissionId);

        return new File(folder, "properties.properties");
    }

    private File getSpacePropertiesFile(long spaceId)
    {
        File folder = getSpaceFolder(spaceId);

        return new File(folder, "properties.properties");
    }

    public PropertiesConfiguration getPageProperties(long pageId, boolean create) throws ConfigurationException
    {
        File file = getPagePropertiesFile(pageId);

        return create || file != null && file.exists() ? new PropertiesConfiguration(file) : null;
    }

    public PropertiesConfiguration getObjectProperties(Long objectId) throws ConfigurationException
    {
        return getObjectProperties("objects", objectId);
    }

    public PropertiesConfiguration getObjectProperties(String folder, Long objectId) throws ConfigurationException
    {
        long id;
        if (objectId != null) {
            id = objectId;
        } else {
            return null;
        }

        File file = getObjectPropertiesFile(folder, id);

        return new PropertiesConfiguration(file);
    }

    public PropertiesConfiguration getObjectProperties(String folder, String objectId, boolean create)
        throws ConfigurationException
    {
        if (objectId == null) {
            return null;
        }

        File file = getObjectPropertiesFile(folder, objectId);

        return create || file.exists() ? new PropertiesConfiguration(file) : null;
    }

    public PropertiesConfiguration getInternalUserProperties(Long userId) throws ConfigurationException
    {
        return getObjectProperties(FOLDER_INTERNALUSER, userId);
    }

    public PropertiesConfiguration getUserImplProperties(String userKey) throws ConfigurationException
    {
        return getObjectProperties(FOLDER_USERIMPL, userKey, false);
    }

    public PropertiesConfiguration getUserProperties(String userIdOrKey) throws ConfigurationException
    {
        PropertiesConfiguration properties = getUserImplProperties(userIdOrKey);

        if (properties == null && NumberUtils.isCreatable(userIdOrKey)) {
            properties = getInternalUserProperties(NumberUtils.createLong(userIdOrKey));
        }

        return properties;
    }

    /**
     * @return users stored with class InternalUser (with long id)
     */
    public Collection<Long> getInternalUsers()
    {
        File folder = getIternalUserFolder();

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

    public PropertiesConfiguration getGroupProperties(Long groupId) throws ConfigurationException
    {
        return getObjectProperties(FOLDER_GROUP, groupId);
    }

    public PropertiesConfiguration getAttachmentProperties(long pageId, long attachmentId) throws ConfigurationException
    {
        File file = getAttachmentPropertiesFile(pageId, attachmentId);

        return new PropertiesConfiguration(file);
    }

    public PropertiesConfiguration getSpacePermissionProperties(long spaceId, long permissionId)
        throws ConfigurationException
    {
        File file = getSpacePermissionPropertiesFile(spaceId, permissionId);

        return new PropertiesConfiguration(file);
    }

    public PropertiesConfiguration getSpaceProperties(long spaceId) throws ConfigurationException
    {
        File file = getSpacePropertiesFile(spaceId);

        return new PropertiesConfiguration(file);
    }

    private void savePageProperties(PropertiesConfiguration properties, long pageId) throws ConfigurationException
    {
        PropertiesConfiguration fileProperties = getPageProperties(pageId, true);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void saveObjectProperties(PropertiesConfiguration properties, long objectId) throws ConfigurationException
    {
        saveObjectProperties("objects", properties, objectId);
    }

    private void saveObjectProperties(String folder, PropertiesConfiguration properties, long objectId)
        throws ConfigurationException
    {
        PropertiesConfiguration fileProperties = getObjectProperties(folder, objectId);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void saveObjectProperties(String folder, PropertiesConfiguration properties, String objectId)
        throws ConfigurationException
    {
        PropertiesConfiguration fileProperties = getObjectProperties(folder, objectId, true);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void saveAttachmentProperties(PropertiesConfiguration properties, long pageId, long attachmentId)
        throws ConfigurationException
    {
        PropertiesConfiguration fileProperties = getAttachmentProperties(pageId, attachmentId);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void saveSpacePermissionsProperties(PropertiesConfiguration properties, long spaceId, long permissionId)
        throws ConfigurationException
    {
        PropertiesConfiguration fileProperties = getSpacePermissionProperties(spaceId, permissionId);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void saveSpaceProperties(PropertiesConfiguration properties, long spaceId) throws ConfigurationException
    {
        PropertiesConfiguration fileProperties = getSpaceProperties(spaceId);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    public File getEntities()
    {
        return this.entities;
    }

    public File getDescriptor()
    {
        return this.descriptor;
    }

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

    public void close() throws IOException
    {
        if (this.tree != null) {
            FileUtils.deleteDirectory(this.tree);
        }

        if (this.temporaryDirectory && this.directory.exists()) {
            FileUtils.deleteDirectory(this.directory);
        }
    }

    public String getAttachmentName(PropertiesConfiguration attachmentProperties)
    {
        String attachmentName = attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_TITLE, null);
        if (attachmentName == null) {
            attachmentName = attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_NAME);
        }

        return attachmentName;
    }

    public Long getAttachementVersion(PropertiesConfiguration attachmentProperties)
    {
        Long version = getLong(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_VERSION, null);
        if (version == null) {
            version = getLong(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_ATTACHMENTVERSION, null);
        }

        return version;
    }

    public long getAttachmentOriginalVersionId(PropertiesConfiguration attachmentProperties, long def)
    {
        Long originalRevisionId =
            getLong(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_ORIGINALVERSIONID, null);
        return originalRevisionId != null ? originalRevisionId
            : getLong(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_ORIGINALVERSION, def);
    }

    public String getTagName(PropertiesConfiguration labellingProperties)
    {
        Long tagId = labellingProperties.getLong(ConfluenceXMLPackage.KEY_LABELLING_LABEL, null);
        String tagName = tagId.toString();

        try {
            PropertiesConfiguration labelProperties = getObjectProperties(tagId);
            tagName = labelProperties.getString(ConfluenceXMLPackage.KEY_LABEL_NAME);
        } catch (NumberFormatException | ConfigurationException e) {
            LOGGER.warn("Unable to get tag name, using id instead.");
        }

        return tagName;
    }

    public String getCommentText(PropertiesConfiguration commentProperties, Long commentId)
    {
        String commentText = commentId.toString();
        try {
            // BodyContent objects are stored in page properties under the content id
            PropertiesConfiguration commentContent = getPageProperties(commentId, false);
            commentText = commentContent.getString("body");
        } catch (ConfigurationException e) {
            LOGGER.warn("Unable to get comment text, using id instead.");
        }

        return commentText;
    }

    public Integer getCommentBodyType(PropertiesConfiguration commentProperties, Long commentId)
    {
        Integer bodyType = -1;
        try {
            PropertiesConfiguration commentContent = getPageProperties(commentId, false);
            bodyType = commentContent.getInt("bodyType");
        } catch (ConfigurationException e) {
            LOGGER.warn("Unable to get comment body type.");
        }

        return bodyType;
    }

    public Long getLong(PropertiesConfiguration properties, String key, Long def)
    {
        try {
            return properties.getLong(key, def);
        } catch (Exception e) {
            // Usually mean the field does not have the expected format

            return def;
        }
    }
}
