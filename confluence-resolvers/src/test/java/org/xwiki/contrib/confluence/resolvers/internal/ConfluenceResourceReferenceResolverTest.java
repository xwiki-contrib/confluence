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
package org.xwiki.contrib.confluence.resolvers.internal;

import com.xpn.xwiki.test.reference.ReferenceComponentList;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageIdResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageTitleResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.contrib.confluence.resolvers.ConfluenceSpaceKeyResolver;
import org.xwiki.contrib.confluence.resolvers.resource.internal.DefaultConfluenceResourceReferenceResolver;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.SpaceResourceReference;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.xwiki.contrib.confluence.resolvers.resource.ConfluenceResourceReferenceType.*;

@ComponentTest
@ReferenceComponentList
class ConfluenceResourceReferenceResolverTest
{
    private static final String DEMO = "DEMO";
    private static final EntityReference DEMO_SPACE = new EntityReference(
        DEMO, EntityType.SPACE, new WikiReference("xwiki"));
    public static final EntityReference FORTYTWODOCREF = new EntityReference("WebHome", EntityType.DOCUMENT,
        new EntityReference("Test", EntityType.SPACE, DEMO_SPACE));
    private static final String XWIKI_DEMO = "xwiki:DEMO";
    private static final String MYSECTION = "mysection";
    private static final DocumentResourceReference DEMO_WEBHOME_REF =
        new DocumentResourceReference("xwiki:DEMO.WebHome");
    private static final String FORTYTWOREF = "xwiki:DEMO.Test.WebHome";
    
    @InjectMockComponents
    private DefaultConfluenceResourceReferenceResolver rrResolver;

    @MockComponent
    private ConfluenceSpaceKeyResolver spaceKeyResolver;

    @MockComponent
    private ConfluencePageIdResolver pageIdResolver;

    @MockComponent
    private ConfluencePageTitleResolver pageTitleResolver;

    private EntityReferenceSerializer<String> referenceSerializer;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @BeforeComponent
    void setup() throws ConfluenceResolverException
    {
        when(spaceKeyResolver.getSpaceByKey(DEMO)).thenReturn(DEMO_SPACE);
        when(pageIdResolver.getDocumentById(42)).thenReturn(FORTYTWODOCREF);
        when(pageTitleResolver.getDocumentByTitle(DEMO, "Hello @ \\World")).thenReturn(FORTYTWODOCREF);
    }

    @AfterComponent
    void setupComponent() throws ComponentLookupException
    {
        referenceSerializer = componentManager.getInstance(
            new DefaultParameterizedType(null, EntityReferenceSerializer.class, String.class));
    }

    @Test
    void space() throws ConfluenceResolverException
    {
        assertEquals(new SpaceResourceReference(XWIKI_DEMO), rrResolver.resolve(CONFLUENCE_SPACE, DEMO));
    }

    @Test
    void spaceAnchor() throws ConfluenceResolverException
    {
        SpaceResourceReference expected = new SpaceResourceReference(XWIKI_DEMO);
        expected.setAnchor(MYSECTION);
        assertEquals(expected, rrResolver.resolve(CONFLUENCE_SPACE, "DEMO#mysection"));
    }

    @Test
    void spaceHome() throws ConfluenceResolverException
    {
        assertEquals(DEMO_WEBHOME_REF, rrResolver.resolve(CONFLUENCE_PAGE, "spaceHome:DEMO"));
        assertEquals(DEMO_WEBHOME_REF, rrResolver.resolve(CONFLUENCE_PAGE, "spaceHome:DEMO#"));
    }

    @Test
    void spaceHomeAnchor() throws ConfluenceResolverException
    {
        DocumentResourceReference expected = DEMO_WEBHOME_REF;
        expected.setAnchor(MYSECTION);
        assertEquals(expected, rrResolver.resolve(CONFLUENCE_PAGE, "spaceHome:DEMO#mysection"));
    }

    @Test
    void spaceHomeFile() throws ConfluenceResolverException
    {
        AttachmentResourceReference expected = new AttachmentResourceReference("xwiki:DEMO.WebHome@file.csv");
        assertEquals(expected, rrResolver.resolve(CONFLUENCE_ATTACH, "spaceHome:DEMO@file.csv"));
        assertEquals(expected, rrResolver.resolve(CONFLUENCE_ATTACH, "spaceHome:DEMO@file.csv#"));
    }

    @Test
    void spaceHomeFileAnchor() throws ConfluenceResolverException
    {
        AttachmentResourceReference expected = new AttachmentResourceReference("xwiki:DEMO.WebHome@file.txt");
        expected.setAnchor(MYSECTION);
        assertEquals(expected, rrResolver.resolve(CONFLUENCE_ATTACH, "spaceHome:DEMO@file.txt#mysection"));
    }

    @Test
    void pageId() throws ConfluenceResolverException
    {
        DocumentResourceReference expected = new DocumentResourceReference(FORTYTWOREF);
        assertEquals(expected, rrResolver.resolve(CONFLUENCE_PAGE, "id:42"));
        assertEquals(expected, rrResolver.resolve(CONFLUENCE_PAGE, "id:42#"));
    }

    @Test
    void pageIdAnchor() throws ConfluenceResolverException
    {
        DocumentResourceReference expected = new DocumentResourceReference(FORTYTWOREF);
        expected.setAnchor(MYSECTION);
        assertEquals(expected, rrResolver.resolve(CONFLUENCE_PAGE, "id:42#mysection"));
    }

    @Test
    void pageIdFile() throws ConfluenceResolverException
    {
        AttachmentResourceReference expected = new AttachmentResourceReference("xwiki:DEMO.Test.WebHome@file.txt");
        assertEquals(expected, rrResolver.resolve(CONFLUENCE_ATTACH, "id:42@file.txt"));
        assertEquals(expected, rrResolver.resolve(CONFLUENCE_ATTACH, "id:42@file.txt#"));
    }

    @Test
    void pageIdFileAnchor() throws ConfluenceResolverException
    {
        AttachmentResourceReference expected = new AttachmentResourceReference(
            referenceSerializer.serialize(new EntityReference("fi\\le.csv", EntityType.ATTACHMENT, FORTYTWODOCREF)));
        expected.setAnchor(MYSECTION);
        assertEquals(expected, rrResolver.resolve(CONFLUENCE_ATTACH, "id:42@fi\\\\le.csv#mysection wordtobeignored"));
    }

    @Test
    void pageTitle() throws ConfluenceResolverException
    {
        DocumentResourceReference expected = new DocumentResourceReference(FORTYTWOREF);
        assertEquals(expected, rrResolver.resolve(CONFLUENCE_PAGE, "page:DEMO.Hello \\@ \\\\World"));
        assertEquals(expected, rrResolver.resolve(CONFLUENCE_PAGE, "page:DEMO.Hello \\@ \\\\World#"));
    }

    @Test
    void pageTitleAnchor() throws ConfluenceResolverException
    {
        DocumentResourceReference expected = new DocumentResourceReference(FORTYTWOREF);
        expected.setAnchor(MYSECTION);
        assertEquals(expected, rrResolver.resolve(CONFLUENCE_PAGE, "page:DEMO.Hello \\@ \\\\World#mysection"));
    }

    @Test
    void pageTitleFile() throws ConfluenceResolverException
    {
        AttachmentResourceReference expected = new AttachmentResourceReference("xwiki:DEMO.Test.WebHome@fi#le.txt");
        assertEquals(expected, rrResolver.resolve(CONFLUENCE_ATTACH, "page:DEMO.Hello \\@ \\\\World@fi\\#le.txt#"));
    }

    @Test
    void pageTitleFileAnchor() throws ConfluenceResolverException
    {
        AttachmentResourceReference expected = new AttachmentResourceReference(
            referenceSerializer.serialize(new EntityReference("fi\\le.txt", EntityType.ATTACHMENT, FORTYTWODOCREF)));
        expected.setAnchor(MYSECTION);
        assertEquals(expected, rrResolver.resolve(CONFLUENCE_ATTACH, "page:DEMO.Hello \\@ \\\\World@fi\\\\le.txt#mysection"));
    }

    @Test
    void notFound() throws ConfluenceResolverException
    {
        assertNull(rrResolver.resolve(CONFLUENCE_PAGE, "page:NOTFOUND.Not Found"));
        assertNull(rrResolver.resolve(CONFLUENCE_PAGE, "page:NOTFOUND.Not Found#anchor"));
        assertNull(rrResolver.resolve(CONFLUENCE_ATTACH, "page:NOTFOUND.Not Found@file"));
        assertNull(rrResolver.resolve(CONFLUENCE_ATTACH, "page:NOTFOUND.Not Found@file#anchor"));
        assertNull(rrResolver.resolve(CONFLUENCE_PAGE, "spaceHome:NOTFOUND"));
        assertNull(rrResolver.resolve(CONFLUENCE_PAGE, "spaceHome:NOTFOUND#anchor"));
        assertNull(rrResolver.resolve(CONFLUENCE_ATTACH, "spaceHome:NOTFOUND@file"));
        assertNull(rrResolver.resolve(CONFLUENCE_ATTACH, "spaceHome:NOTFOUND@file#anchor"));
        assertNull(rrResolver.resolve(CONFLUENCE_PAGE, "id:4242"));
        assertNull(rrResolver.resolve(CONFLUENCE_PAGE, "id:4242#anchor"));
        assertNull(rrResolver.resolve(CONFLUENCE_ATTACH, "id:4242@file"));
        assertNull(rrResolver.resolve(CONFLUENCE_ATTACH, "id:4242@file#anchor"));
        assertNull(rrResolver.resolve(CONFLUENCE_SPACE, "space:NOTFOUND"));
        assertNull(rrResolver.resolve(CONFLUENCE_SPACE, "space:NOTFOUND#anchor"));
    }

    @Test
    void notCorrect() throws ConfluenceResolverException
    {
        // unexpected prefix
        assertNull(rrResolver.resolve(CONFLUENCE_PAGE, "unexpectedprefix:blablabla"));

        // missing attachment
        assertNull(rrResolver.resolve(CONFLUENCE_ATTACH, "pageid:42"));
        assertNull(rrResolver.resolve(CONFLUENCE_ATTACH, "pageid:42@"));

        // dangling escaping slash
        assertNull(rrResolver.resolve(CONFLUENCE_PAGE, "page:DEMO.Hello\\"));
        assertNull(rrResolver.resolve(CONFLUENCE_ATTACH, "pageid:42@file.csv\\"));
    }

    @Test
    void pageTitleEmptySpace() throws ConfluenceResolverException
    {
        assertNull(rrResolver.resolve(CONFLUENCE_PAGE, "page:Hello \\. World"));
    }

    @Test
    void testGetType()
    {
        assertEquals(CONFLUENCE_PAGE, rrResolver.getType("confluencePage:id:42"));
        assertEquals(CONFLUENCE_ATTACH, rrResolver.getType("confluenceAttach:id:42@file.png"));
        assertEquals(CONFLUENCE_SPACE, rrResolver.getType("confluenceSpace:MySpace"));
    }
}
