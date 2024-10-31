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
import org.xwiki.contrib.confluence.resolvers.ConfluenceSpaceKeyResolver;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
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
import org.xwiki.test.page.PageComponentList;

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
    PageClassConfluenceResolver.class,
    LocalStringEntityReferenceSerializer.class
})
@PageComponentList
class ConfluenceResolversTest
{
    private static final String MY_SPACE = "MySpace";
    private static final String MY_DOC = "My Doc";
    private static final String XWIKI = "xwiki";
    private static final String MIGRATION_ROOT = "MigrationRoot";
    private static final String MY_FALLBACK = "MyFallback";
    private static final String WEB_HOME = "WebHome";

    private static final String VALUE = "value";
    private static final String SPACE = "space";

    private static final String NOT_FOUND_DOC = "Not Found Doc";

    private static final String NOT_FOUND_SPACE = "NotFoundSpace";

    private static final EntityReference MY_FALLBACK_SPACE = new EntityReference(MY_FALLBACK, EntityType.SPACE);
    private static final EntityReference MY_FALLBACK_HOME = new LocalDocumentReference(WEB_HOME, MY_FALLBACK_SPACE);

    private static final DocumentReference MY_DOC_REF = new DocumentReference(
        XWIKI,
        List.of(MIGRATION_ROOT, MY_SPACE, "MyDoc"),
        WEB_HOME
    );

    private static final DocumentReference MY_SPACE_REF = new DocumentReference(
        XWIKI,
        List.of(MIGRATION_ROOT, MY_SPACE),
        WEB_HOME
    );

    @InjectMockComponents (role = ConfluencePageIdResolver.class)
    private DefaultConfluencePageResolver confluencePageResolver;

    @InjectMockComponents (role = ConfluenceSpaceKeyResolver.class)
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
                return new XWikiDocument((new DocumentReference(docRef.appendParent(new WikiReference(XWIKI)))));
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
        AtomicReference<String> value = new AtomicReference<>("");
        when(query.bindValue(eq(VALUE), anyLong())).thenAnswer(invocationOnMock -> {
            id.set(invocationOnMock.getArgument(1));
            return query;
        });

        when(query.bindValue(eq(SPACE), anyString())).thenAnswer(invocationOnMock -> {
            space.set(invocationOnMock.getArgument(1));
            return query;
        });

        when(query.bindValue(eq(VALUE), anyString())).thenAnswer(invocationOnMock -> {
            value.set(invocationOnMock.getArgument(1));
            return query;
        });

        when(query.setLimit(anyInt())).thenReturn(query);

        when(query.execute()).thenAnswer(invocationOnMock -> {
            if (id.get() == 42) {
                return List.of(new XWikiDocument(MY_DOC_REF));
            }

            if (space.get().isEmpty() && (value.get().equals(MY_SPACE))) {
                return List.of(new XWikiDocument(MY_SPACE_REF));
            }

            if (space.get().equals(MY_SPACE)) {
                if (value.get().equals(MY_DOC)) {
                    return List.of(new XWikiDocument(MY_DOC_REF));
                }

                if (value.get().equals(NOT_FOUND_DOC)) {
                    return List.of();
                }

                return List.of(
                    new XWikiDocument(new DocumentReference(
                        WEB_HOME,
                        new SpaceReference(MY_DOC_REF.getParent().getParent()))));
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
        assertEquals(MY_DOC_REF, confluencePageResolver.getDocumentByTitle(MY_SPACE, MY_DOC));
    }

    @Test
    void testGetDocumentByTitleNotFound() throws ConfluenceResolverException
    {
        assertNull(confluencePageResolver.getDocumentByTitle(MY_SPACE, NOT_FOUND_DOC));
    }

    @Test
    void testGetSpaceByKeyUsingConfluencePageClass() throws ConfluenceResolverException
    {
        assertEquals(
            MY_SPACE_REF.getParent(),
            confluenceSpaceResolver.getSpaceByKey(MY_SPACE));
    }

    @Test
    void testGetSpaceByKeyUsingFallback() throws ConfluenceResolverException
    {
        assertEquals(
            MY_FALLBACK_SPACE,
            confluenceSpaceResolver.getSpaceByKey(MY_FALLBACK));
    }

    @Test
    void testGetSpaceByKeyNotFound() throws Exception
    {
        assertNull(confluenceSpaceResolver.getSpaceByKey(NOT_FOUND_SPACE));
    }
}