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
package org.xwiki.contrib.confluence.parser.confluence.internal.wikimodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

@ComponentList({ 
    WikiModel.class
})
@ComponentTest
public class ConfluenceResourceReferenceParserTest
{
    @InjectMockComponents
    private ConfluenceResourceReferenceParser parser;

    @Test
    void testBasicTypes()
    {
        ((Logger) LoggerFactory.getLogger(ConfluenceResourceReferenceParser.class)).setLevel(Level.ERROR);
        assertRef(null, "simple page", null,  ResourceType.DOCUMENT, parser.parse("simple page"));
        assertRef("space", "page", null, ResourceType.DOCUMENT, parser.parse("space:page"));
        assertRef("space", null, null, ResourceType.SPACE, parser.parse("space:"));
        assertRef(null, null, "filename", ResourceType.ATTACHMENT, parser.parse("^filename"));
        assertRef(null, "page", "filename", ResourceType.ATTACHMENT, parser.parse("page^filename"));
        assertRef("space", "page", "filename", ResourceType.ATTACHMENT, parser.parse("space:page^filename"));
        assertEquals(new ResourceReference("username", ResourceType.USER), parser.parse("~username"));
        assertEquals(new ResourceReference("https://extensions.xwiki.org/", ResourceType.URL),
            parser.parse("https://extensions.xwiki.org/"));
        // files containing an '^' are forbidden in confluence
        ResourceReference unparseable = new ResourceReference("broken^file^reference", ResourceType.UNKNOWN);
        unparseable.setTyped(false);
        assertEquals(unparseable, parser.parse("broken^file^reference"));
    }

    private void assertRef(String space, String page, String filename, ResourceType type, ResourceReference ref)
    {
        ConfluenceResourceReference cref = (ConfluenceResourceReference) ref;
        assertEquals(space, cref.getSpaceKey());
        assertEquals(page, cref.getPageTitle());
        assertEquals(filename, cref.getFilename());
        assertEquals(type, ref.getType());
    }
}
