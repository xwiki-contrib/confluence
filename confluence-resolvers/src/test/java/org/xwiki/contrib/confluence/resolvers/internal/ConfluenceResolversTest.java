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
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageIdResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.contrib.confluence.resolvers.ConfluenceSpaceKeyResolver;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.PageComponentList;

import javax.inject.Provider;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final String NOT_FOUND_DOC = "Not Found Doc";

    private static final String NOT_FOUND_SPACE = "NotFoundSpace";

    private static final EntityReference MY_FALLBACK_SPACE = new EntityReference(MY_FALLBACK, EntityType.SPACE);
    private static final EntityReference MY_FALLBACK_HOME = new LocalDocumentReference(WEB_HOME, MY_FALLBACK_SPACE);
    private static final EntityReference MY_FALLBACK_REF = new EntityReference(WEB_HOME, EntityType.DOCUMENT,
        new EntityReference("RandomDoc", EntityType.SPACE, MY_FALLBACK_SPACE));

    private static final DocumentReference MY_DOC_REF = new DocumentReference(
        XWIKI,
        List.of(MIGRATION_ROOT, MY_SPACE, MY_DOC),
        WEB_HOME
    );

    private static final DocumentReference MY_SPACE_REF = new DocumentReference(
        XWIKI,
        List.of(MIGRATION_ROOT, MY_SPACE),
        WEB_HOME
    );
    private static final String FAKE_SOLR_DOC_KEY = "_fakeDoc";

    @InjectMockComponents (role = ConfluencePageIdResolver.class)
    private DefaultConfluencePageResolver confluencePageResolver;

    @InjectMockComponents (role = ConfluenceSpaceKeyResolver.class)
    private DefaultConfluenceSpaceResolver confluenceSpaceResolver;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private DocumentReferenceResolver<SolrDocument> solrDocumentDocumentReferenceResolver;

    @MockComponent
    private SolrUtils solrUtils;

    @BeforeComponent
    void setup() throws Exception
    {
        when(solrDocumentDocumentReferenceResolver.resolve(any())).thenAnswer(i ->
            new DocumentReference(((Map<String, DocumentReference>) i.getArgument(0)).get(FAKE_SOLR_DOC_KEY)));
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
        when(solrUtils.toFilterQueryString(any())).thenAnswer(i -> i.getArgument(0) instanceof String
            ? '"' + ((String) i.getArgument(0)) + '"'
            : i.getArgument(0).toString());
    }

    private SolrDocument fakeSolrDocument(DocumentReference ref, String spaceKey, String title)
    {
        return new SolrDocument(Map.of(
            FAKE_SOLR_DOC_KEY, ref,
            "fullname", ref.toString().replace("Document ", ""),
            "property.Confluence.Code.ConfluencePageClass.title_string", title,
            "property.Confluence.Code.ConfluencePageClass.space_string", spaceKey
        ));
    }

    @BeforeEach
    void beforeEach() throws QueryException
    {
        Query query = mock(Query.class);
        AtomicLong id = new AtomicLong(0);
        AtomicReference<String> space = new AtomicReference<>("");
        AtomicReference<String> title = new AtomicReference<>("");
        AtomicReference<String> queryType = new AtomicReference<>("");
        AtomicReference<String> hqlResult = new AtomicReference<>("");
        when(queryManager.createQuery(anyString(), anyString())).thenReturn(query);
        when(query.bindValue(anyString(), anyString())).thenAnswer(i -> {
            if (i.getArgument(0).equals("fullname")
                && i.getArgument(1).equals(MY_DOC_REF.toString().replace("xwiki:", ""))) {
                // This is the getSpace query
                hqlResult.set(MY_SPACE);
            }
            return query;
        });
        when(queryManager.createQuery(anyString(), anyString())).thenAnswer(i -> {
            queryType.set(i.getArgument(1));
            return query;
        });
        when(query.bindValue(eq("fq"), anyString())).thenAnswer(invocationOnMock -> {
            String queryString = invocationOnMock.getArgument(1);
            Pattern idPattern = Pattern.compile(".*property.Confluence.Code.ConfluencePageClass.id:(\\d+).*");
            Matcher matcher = idPattern.matcher(queryString);
            if (matcher.matches()) {
                id.set(Long.parseLong(matcher.group(1)));
            } else {
                id.set(-1);
            }

            Pattern spacePattern = Pattern.compile(".*property.Confluence.Code.ConfluencePageClass.space_string:"
                + "\"([^\"]+)\".*");
            matcher = spacePattern.matcher(queryString);
            if (matcher.matches()) {
                space.set(matcher.group(1));
            } else {
                space.set("");
            }

            Pattern titlePattern = Pattern.compile(".*property.Confluence.Code.ConfluencePageClass.title_string:"
                + "\"([^\"]+)\".*");
            matcher = titlePattern.matcher(queryString);
            if (matcher.matches()) {
                title.set(matcher.group(1));
            } else {
                title.set("");
            }

            return query;
        });
        when(query.setLimit(anyInt())).thenReturn(query);
        when(query.setWiki(anyString())).thenReturn(query);

        when(query.execute()).thenAnswer(invocationOnMock -> {
            if (queryType.get().equals(Query.HQL)) {
                return Collections.singletonList(hqlResult.get());
            }
            SolrDocumentList res = new SolrDocumentList();
            if (id.get() == 42) {
                res.add(fakeSolrDocument(MY_DOC_REF, MY_SPACE, MY_DOC));
            } else if (space.get().equals(MY_SPACE)) {
                if (title.get().isEmpty()) {
                    res.add(fakeSolrDocument(MY_DOC_REF, MY_SPACE, MY_DOC));
                    res.add(fakeSolrDocument(
                        MY_SPACE_REF,
                        MY_SPACE,
                        "Space home"));
                } else if (title.get().equals(MY_DOC)) {
                    res.add(fakeSolrDocument(MY_DOC_REF, MY_SPACE, MY_DOC));
                } else if (!title.get().equals(NOT_FOUND_DOC)) {
                    res.add(fakeSolrDocument(
                        new DocumentReference(
                            WEB_HOME,
                            new SpaceReference(MY_DOC_REF.getParent().getParent())
                        ),
                        MY_SPACE,
                        title.get()));
                }
            }

            QueryResponse r = mock(QueryResponse.class);
            when(r.getResults()).thenReturn(res);
            return Collections.singletonList(r);
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
    void testGetSpaceKeyUsingConfluencePageClass() throws ConfluenceResolverException
    {
        assertEquals(MY_SPACE, confluenceSpaceResolver.getSpaceKey(MY_DOC_REF));
    }

    @Test
    void testGetSpace() throws ConfluenceResolverException
    {
        assertEquals(
            MY_SPACE_REF.getParent(),
            confluenceSpaceResolver.getSpace(MY_DOC_REF));
    }

    @Test
    void testGetSpaceByKeyUsingFallback() throws ConfluenceResolverException
    {
        assertEquals(
            MY_FALLBACK_SPACE,
            confluenceSpaceResolver.getSpaceByKey(MY_FALLBACK));
    }

    @Test
    void testGetSpaceKeyUsingFallback() throws ConfluenceResolverException
    {
        assertEquals(
            MY_FALLBACK,
            confluenceSpaceResolver.getSpaceKey(MY_FALLBACK_REF));
    }

    @Test
    void testGetSpaceUsingFallback() throws ConfluenceResolverException
    {
        assertEquals(
            MY_FALLBACK_SPACE,
            confluenceSpaceResolver.getSpace(MY_FALLBACK_REF));
    }

    @Test
    void testGetSpaceByKeyNotFound() throws Exception
    {
        assertNull(confluenceSpaceResolver.getSpaceByKey(NOT_FOUND_SPACE));
    }
}