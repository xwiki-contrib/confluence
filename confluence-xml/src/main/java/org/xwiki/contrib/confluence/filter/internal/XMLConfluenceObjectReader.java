package org.xwiki.contrib.confluence.filter.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.xwiki.contrib.confluence.filter.input.ConfluenceProperties;
import org.xwiki.filter.FilterException;
import org.xwiki.xml.stax.StAXUtils;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_ID;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_PAGE_POSITION;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_CLASS;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_SPACE_NAME;

public class XMLConfluenceObjectReader implements ConfluenceObjectReader
{
    private static final String COLLECTION = "collection";
    private static final String PROPERTY = "property";
    private static final String ATTRIBUTE_CLASS = KEY_CLASS;
    static final String KEY_NAME = KEY_SPACE_NAME;

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

    public XMLConfluenceObjectReader(XMLStreamReader xmlReader)
    {
        this.xmlReader = xmlReader;
    }

    public Object readObjectProperties(ConfluenceProperties properties, ConfluenceObjectFields fields)
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

    public String getNextClass()
    {
        return xmlReader.getAttributeValue(null, ATTRIBUTE_CLASS);
    }
}
