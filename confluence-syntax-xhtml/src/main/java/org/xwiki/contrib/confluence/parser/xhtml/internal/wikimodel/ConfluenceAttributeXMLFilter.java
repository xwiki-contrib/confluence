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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xwiki.rendering.wikimodel.xhtml.filter.DefaultXMLFilter;

import java.util.Map;

/**
 * Convert some parameters.
 * @since 9.60.0
 * @version $Id$
 */
public class ConfluenceAttributeXMLFilter extends DefaultXMLFilter
{
    private static final String DATA_HIGHLIGHT_COLOUR = "data-highlight-colour";
    private static final String STYLE = "style";
    private static final String BACKGROUND_COLOR = "background-color";
    private static final String CDATA = "CDATA";

    private static final Map<String, String> CONVERTED_COLORS = Map.of(
        // observed
        "blue", "#deebff",
        "green", "#e3fcef",
        "yellow", "#fffae6",
        // guessed
        "white", "#ffffff",
        "teal", "#e6fcff",
        "red", "#ffebe6",
        "purple", "#eae6ff"
    );

    /**
     * Constructor.
     * @param reader the reader
     */
    public ConfluenceAttributeXMLFilter(XMLReader reader)
    {
        super(reader);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
        super.startElement(uri, localName, qName, convert(atts));
    }

    private Attributes convert(Attributes atts)
    {
        return cleanupRowspanColspan(handleHighlightColour(atts));
    }

    private static Attributes handleHighlightColour(Attributes atts)
    {
        String highlightColor = atts.getValue(DATA_HIGHLIGHT_COLOUR);
        if (highlightColor != null && !highlightColor.isEmpty()) {
            highlightColor = CONVERTED_COLORS.getOrDefault(highlightColor, highlightColor);
            AttributesImpl convertedAtts = new AttributesImpl(atts);

            convertedAtts.removeAttribute(convertedAtts.getIndex(DATA_HIGHLIGHT_COLOUR));
            int styleIndex = convertedAtts.getIndex(STYLE);
            String style = styleIndex == -1 ? "" : convertedAtts.getValue(styleIndex);
            if (!style.contains(BACKGROUND_COLOR)) {
                // there's already a background-color, let's not override it.
                String sep = (style.isEmpty() || style.endsWith(";")) ? " " : "; ";
                style += sep + BACKGROUND_COLOR + ": " + highlightColor;
                if (styleIndex == -1) {
                    convertedAtts.addAttribute("", "", STYLE, CDATA, style.trim());
                } else {
                    convertedAtts.setAttribute(styleIndex, "", "", STYLE, CDATA, style.trim());
                }
            }

            return convertedAtts;
        }

        return atts;
    }

    private Attributes cleanupRowspanColspan(Attributes atts)
    {
        return removeIfOne("rowspan", removeIfOne("colspan", atts));
    }

    private Attributes removeIfOne(String attributeName, Attributes atts)
    {
        String val = atts.getValue(attributeName);
        if ("1".equals(val)) {
            AttributesImpl convertedAtts = atts instanceof AttributesImpl
                ? (AttributesImpl) atts
                : new AttributesImpl(atts);
            convertedAtts.removeAttribute(atts.getIndex(attributeName));
            return convertedAtts;
        }
        return atts;
    }
}
