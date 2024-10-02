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

import org.junit.jupiter.api.Test;
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
    public void testBasicTypes()
    {
        assertEquals(new ResourceReference("simple page", ResourceType.DOCUMENT),
            parser.parse("simple page"));
        assertEquals(new ResourceReference("space.page", ResourceType.DOCUMENT), parser.parse("space:page"));
        assertEquals(new ResourceReference("space", ResourceType.SPACE), parser.parse("space:"));
        assertEquals(new ResourceReference("filename", ResourceType.ATTACHMENT), parser.parse("^filename"));
        // could this be of type ResourceType.PAGE_ATTACHMENT instead?
        // FIXME: instead should have baseReferences instead
        assertEquals(new ResourceReference("page@filename", ResourceType.ATTACHMENT),
            parser.parse("page^filename"));
        assertEquals(new ResourceReference("space.page@filename", ResourceType.ATTACHMENT),
            parser.parse("space:page^filename"));
        assertEquals(new ResourceReference("username", ResourceType.USER), parser.parse("~username"));
        assertEquals(new ResourceReference("https://extensions.xwiki.org/", ResourceType.URL),
            parser.parse("https://extensions.xwiki.org/"));
        // files containing an '^' are forbidden in confluence
        // the integration tests expect an URL instead of unknown?
        ResourceReference unparseable = new ResourceReference("broken^file^refernce", ResourceType.URL);
        unparseable.setTyped(false);
        assertEquals(unparseable, parser.parse("broken^file^refernce"));
    }

    @Test
    public void testDotEscape()
    {
        assertEquals(new ResourceReference("space.dot\\.name", ResourceType.DOCUMENT),
            parser.parse("space:dot.name"));
        assertEquals(new ResourceReference("dot\\.name", ResourceType.DOCUMENT), parser.parse("dot.name"));
    }

    @Test
    public void testEscapePage()
    {
        assertEquals("a\\.b", ConfluenceResourceReferenceParser.escapePage("a.b"));
        assertEquals("a\\.b\\.c", ConfluenceResourceReferenceParser.escapePage("a.b.c"));
        assertEquals("a\\:b\\.c", ConfluenceResourceReferenceParser.escapePage("a:b.c"));
        assertEquals("a\\\\b", ConfluenceResourceReferenceParser.escapePage("a\\b"));
    }

    @Test
    public void testEscapeAttachment()
    {
        assertEquals("a\\@b", ConfluenceResourceReferenceParser.escapeAttachment("a@b"));
    }
}
