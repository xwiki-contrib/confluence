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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageIdResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import javax.inject.Provider;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
@ComponentList({
    PageClassConfluenceResolver.class
})
class ConfluenceResolversTest
{
    private static final EntityReference MY_FALLBACK_SPACE = new EntityReference("MyFallback", EntityType.SPACE);
    private static final EntityReference MY_FALLBACK_HOME = new LocalDocumentReference("WebHome", MY_FALLBACK_SPACE);

    private static final String MY_SPACE = "MySpace";
    private static final DocumentReference MY_DOC_REF = new DocumentReference(
        "xwiki",
        List.of("MigrationRoot", MY_SPACE, "MyDoc"),
        "WebHome"
    );

    @InjectMockComponents (role = ConfluencePageIdResolver.class)
    private DefaultConfluencePageResolver confluencePageResolver;

    @InjectMockComponents
    private DefaultConfluenceSpaceResolver confluenceSpaceResolver;

    @MockComponent
    Provider<XWikiContext> xcontextProvider;

    @MockComponent
    QueryManager queryManager;

    @BeforeComponent
    void setup() throws Exception
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        XWiki wiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(wiki);
        XWikiDocument fallbackHome = mock(XWikiDocument.class);
        XWikiDocument notFoundHome = mock(XWikiDocument.class);
        when(fallbackHome.isNew()).thenReturn(false);
        when(notFoundHome.isNew()).thenReturn(true);
        when(wiki.getDocument((EntityReference) any(), refEq(xcontext))).thenAnswer(invocationOnMock -> {
            EntityReference docRef = invocationOnMock.getArgument(0);
            if (docRef.equals(MY_FALLBACK_HOME)) {
                return fallbackHome;
            }

            if (docRef instanceof DocumentReference) {
                return new XWikiDocument((DocumentReference) docRef);
            }

            if (docRef instanceof LocalDocumentReference) {
                return new XWikiDocument((new DocumentReference(docRef.appendParent(new WikiReference("xwiki")))));
            }

            return new XWikiDocument(new DocumentReference(docRef));
        });
    }

    @BeforeEach
    void beforeEach() throws QueryException
    {
        Query query = mock(Query.class);
        when(queryManager.createQuery(anyString(), anyString())).thenReturn(query);
        AtomicLong id = new AtomicLong(0);
        AtomicReference<String> space = new AtomicReference<>("");
        AtomicReference<String> title = new AtomicReference<>("");
        when(query.bindValue(eq("id"), anyLong())).thenAnswer(invocationOnMock -> {
            id.set(invocationOnMock.getArgument(1));
            return query;
        });

        when(query.bindValue(eq("space"), anyString())).then(invocationOnMock -> {
            space.set(invocationOnMock.getArgument(1));
            return query;
        });

        when(query.bindValue(eq("title"), anyString())).then(invocationOnMock -> {
            title.set(invocationOnMock.getArgument(1));
            return query;
        });

        when(query.setLimit(anyInt())).thenReturn(query);

        when(query.execute()).thenAnswer(invocationOnMock -> {
            if (id.get() == 42) {
                return List.of(new XWikiDocument(MY_DOC_REF));
            }

            if ("MySpace".equals(space.get())) {
                switch (title.get()) {
                    case "My Doc":
                        return List.of(new XWikiDocument(MY_DOC_REF));
                    case "":
                        return List.of(
                            new XWikiDocument(new DocumentReference(
                                "WebHome",
                                new SpaceReference(MY_DOC_REF.getParent().getParent()))));
                    default:
                        // ignore
                }
            }
            return List.of();
        });
    }

    @Test
    void testGetDocumentById() throws ConfluenceResolverException
    {
        assertEquals(MY_DOC_REF, confluencePageResolver.getDocumentById(42));
    }

    @Test
    void testGetDocumentByIdNotFound() throws Exception
    {
        assertNull(confluencePageResolver.getDocumentById(1337));
    }


    @Test
    void testGetDocumentByTitle() throws ConfluenceResolverException
    {
        assertEquals(MY_DOC_REF, confluencePageResolver.getDocumentByTitle(MY_SPACE, "My Doc"));
    }

    @Test
    void testGetDocumentByTitleNotFound() throws ConfluenceResolverException
    {
        assertNull(confluencePageResolver.getDocumentByTitle(MY_SPACE, "Not Found Doc"));
    }

    @Test
    void testGetSpaceByKeyUsingConfluencePageClass() throws ConfluenceResolverException
    {
        assertEquals(
            new EntityReference(MY_SPACE, EntityType.SPACE,
                new EntityReference("MigrationRoot", EntityType.SPACE, new WikiReference("xwiki"))),
            confluenceSpaceResolver.getSpaceByKey(MY_SPACE));
    }

    @Test
    void testGetSpaceByKeyUsingFallback() throws ConfluenceResolverException
    {
        assertEquals(
            MY_FALLBACK_SPACE,
            confluenceSpaceResolver.getSpaceByKey("MyFallback"));
    }

    @Test
    void testGetSpaceByKeyNotFound() throws Exception
    {
        assertNull(confluenceSpaceResolver.getSpaceByKey("NotFoundSpace"));
    }
}