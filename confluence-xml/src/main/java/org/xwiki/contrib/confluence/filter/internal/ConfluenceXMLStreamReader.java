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
import org.apache.commons.io.input.BoundedInputStream;
import org.xwiki.contrib.confluence.filter.input.ConfluenceProperties;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceCanceledException;
import org.xwiki.filter.FilterException;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.xml.stax.StAXUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_ID;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_PAGE_POSITION;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_CLASS;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_SPACE_NAME;

/**
 * An XML backup is a zip file containing an entities.xml file which is basically a hibernate database dump, an export
 * descriptor, possibly a CSV file listing tasks appearing in contents, as well as an attachment folder.
 * This class parses the entities.xml file. Historically, the code doing this lived in the ConfluenceXMLPackage class.
 * @since 9.96.0
 * @version $Id$
 */
public final class ConfluenceXMLStreamReader implements ConfluenceRecordReader
{
    static final String KEY_NAME = KEY_SPACE_NAME;

    private static final String COLLECTION = "collection";
    private static final String PROPERTY = "property";
    private static final String ATTRIBUTE_CLASS = KEY_CLASS;

    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    /**
     * Pattern to find the end of "intentionally damaged" CDATA end sections. Confluence does this to nest CDATA
     * sections inside CDATA sections. Interestingly it does not care if there is a &gt; after the ]].
     */
    private static final Pattern FIND_BROKEN_CDATA_PATTERN = Pattern.compile("]] ");

    /**
     * Replacement to repair the CDATA.
     */
    private static final String REPAIRED_CDATA_END = "]]";

    private final XMLStreamReader xmlReader;
    private String currentType;

    private ConfluenceXMLStreamReader(XMLStreamReader xmlReader)
    {
        this.xmlReader = xmlReader;
    }

    /**
     * Reads objects in the entities.xml file and calls the provided consumer for each of them.
     * @param progress the progress manager to use to indicate progress
     * @param entities the entities.xml file
     * @param consumer the callback function that should be called for each record
     * @throws FilterException if something wrong happens
     * @throws ConfluenceCanceledException if the execution shall be interrupted
     */
    public static void readRecords(JobProgressManager progress, File entities, ConfluenceConsumer consumer)
            throws FilterException, ConfluenceCanceledException
    {
        try (BoundedInputStream s = new BoundedInputStream(new BufferedInputStream(new FileInputStream(entities)))) {
            XMLStreamReader xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(new WithoutControlCharactersReader(s));
            new ConfluenceXMLStreamReader(xmlReader).readRecords(progress, s, entities, consumer);
        } catch (IOException | XMLStreamException | ConfigurationException e) {
            throw new FilterException(e);
        } finally {
            progress.popLevelProgress(ConfluenceXMLStreamReader.class);
        }
    }

    private void readRecords(JobProgressManager progress, BoundedInputStream s, File entities,
        ConfluenceConsumer consumer)
            throws FilterException, ConfluenceCanceledException, XMLStreamException, ConfigurationException
    {
        int steps = 100;
        progress.pushLevelProgress(steps, ConfluenceXMLStreamReader.class);
        xmlReader.nextTag();

        long size = entities.length();
        boolean inStep = false;
        long stepSize = size / steps;
        long nextStepPos = stepSize;

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            if (!inStep) {
                progress.startStep(ConfluenceXMLStreamReader.class);
                inStep = true;
            }
            String elementName = xmlReader.getLocalName();

            if ("object".equals(elementName)) {
                currentType = xmlReader.getAttributeValue(null, ATTRIBUTE_CLASS);
                consumer.accept(this);
            } else {
                StAXUtils.skipElement(xmlReader);
            }
            long pos = s.getCount();
            if (pos >= nextStepPos) {
                progress.endStep(ConfluenceXMLStreamReader.class);
                nextStepPos += stepSize;
                inStep = false;
            }
        }

        if (inStep) {
            progress.endStep(ConfluenceXMLStreamReader.class);
        }
    }

    @Override
    public String getType()
    {
        return currentType;
    }

    @Override
    public Object readRecord(ConfluenceProperties properties)
        throws FilterException
    {
        Object id = "-1";

        try {
            for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
                String localName = xmlReader.getLocalName();
                if (KEY_ID.equals(localName)) {
                    id = getId(properties);
                } else if (COLLECTION.equals(localName) || PROPERTY.equals(localName)) {
                    String attributeName = xmlReader.getAttributeValue(null, KEY_NAME);
                    String className = xmlReader.getAttributeValue(null, ATTRIBUTE_CLASS);
                    properties.setAttributeClass(attributeName, className);
                    if (COLLECTION.equals(localName)) {
                        properties.setProperty(attributeName, readListProperty(xmlReader));
                    } else {
                        properties.setProperty(attributeName, readProperty(xmlReader));
                    }
                } else if (KEY_PAGE_POSITION.equals(localName)) {
                    properties.setProperty(KEY_PAGE_POSITION, xmlReader.getElementText());
                } else {
                    StAXUtils.skipElement(xmlReader);
                }
            }
        } catch (XMLStreamException e) {
            throw new FilterException(e);
        }

        return id;
    }

    private Object getId(ConfluenceProperties properties) throws XMLStreamException
    {
        String nameAttribute = xmlReader.getAttributeValue(null, "name");
        String idStr = fixCDataAndNL(xmlReader.getElementText());
        Object id = idStr;
        if ("id".equals(nameAttribute)) {
            try {
                id = Long.parseLong(idStr);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        properties.setProperty(KEY_ID, id);
        return id;
    }

    private Object readProperty(XMLStreamReader xmlReader) throws XMLStreamException, FilterException
    {
        Object res = null;
        String propertyClass = xmlReader.getAttributeValue(null, ATTRIBUTE_CLASS);

        if (propertyClass == null) {
            try {
                res = fixCDataAndNL(xmlReader.getElementText());
            } catch (XMLStreamException e) {
                // Probably an empty element
            }
        } else if (propertyClass.equals("java.util.List") || propertyClass.equals("java.util.Collection")) {
            res = readListProperty(xmlReader);
        } else if (propertyClass.equals("java.util.Set")) {
            res = readSetProperty(xmlReader);
        } else {
            res = readObjectReference(xmlReader);
        }

        if (res == null) {
            StAXUtils.skipElement(xmlReader);
        }

        return res;
    }

    /**
     * To protect content with cdata section inside cdata elements confluence adds a single space after two
     * consecutive curly braces. we need to undo this patch as otherwise the content parser will complain about invalid
     * content. Strictly speaking this needs only to be done for string valued properties.
     * What's more, Confluence may export LS characters that don't mix well with ConfluenceProperties, so we replace
     * them with regular new lines.
     */
    private String fixCDataAndNL(String elementText)
    {
        return elementText == null
            ? null
            : FIND_BROKEN_CDATA_PATTERN.matcher(elementText).replaceAll(REPAIRED_CDATA_END)
                .replace('\u2028', '\n')
                .replace('\u2029', '\n');
    }

    private Object readObjectReference(XMLStreamReader xmlReader) throws FilterException, XMLStreamException
    {
        xmlReader.nextTag();
        checkIdElement(xmlReader);
        String nameAttribute = xmlReader.getAttributeValue(null, KEY_NAME);
        Object id = fixCDataAndNL(xmlReader.getElementText());
        if (KEY_ID.equals(nameAttribute)) {
            id = Long.valueOf((String) id);
        }
        xmlReader.nextTag();
        return id;
    }

    private static void checkIdElement(XMLStreamReader xmlReader) throws FilterException
    {
        if (!xmlReader.getLocalName().equals(KEY_ID)) {
            throw new FilterException(
                String.format("Was expecting id element but found [%s]", xmlReader.getLocalName()));
        }
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
}
