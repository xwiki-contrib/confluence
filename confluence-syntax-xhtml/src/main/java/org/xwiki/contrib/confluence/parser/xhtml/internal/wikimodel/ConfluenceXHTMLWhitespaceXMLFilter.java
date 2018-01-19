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
package org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xwiki.rendering.wikimodel.xhtml.filter.XHTMLWhitespaceXMLFilter;

/**
 * Override XHTMLWhitespaceXMLFilter to take into account special Confluence elements.
 * 
 * @version $Id$
 */
public class ConfluenceXHTMLWhitespaceXMLFilter extends XHTMLWhitespaceXMLFilter
{
    /**
     * Visible elements like images count in the inline text to clean white spaces.
     */
    private static final Set<String> EMPTYVISIBLE_ELEMENTS =
        new HashSet<>(Arrays.asList("ri:page", "ri:space", "ri:user"));

    private LexicalHandler lexicalHandler;

    /**
     * @param reader the XML reader
     */
    public ConfluenceXHTMLWhitespaceXMLFilter(XMLReader reader)
    {
        super(reader);
    }

    @Override
    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException
    {
        // We save the lexical handler so that we can use it in the
        // implementation of the LexicalHandler interface methods.
        if (SAX_LEXICAL_HANDLER_PROPERTY.equals(name)) {
            this.lexicalHandler = (LexicalHandler) value;
        } else {
            super.setProperty(name, value);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
        if (EMPTYVISIBLE_ELEMENTS.contains(qName)) {
            startEmptyVisibleElement();

            getContentHandler().startElement(uri, localName, qName, atts);
        } else {
            super.startElement(uri, localName, qName, atts);

        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        if (EMPTYVISIBLE_ELEMENTS.contains(qName)) {
            endEmptyVisibleElement();

            getContentHandler().endElement(uri, localName, qName);
        } else {
            super.endElement(uri, localName, qName);
        }
    }

    @Override
    public void startCDATA() throws SAXException
    {
        startEmptyVisibleElement();

        this.lexicalHandler.startCDATA();
    }

    @Override
    public void endCDATA() throws SAXException
    {
        endEmptyVisibleElement();

        this.lexicalHandler.endCDATA();
    }
}
