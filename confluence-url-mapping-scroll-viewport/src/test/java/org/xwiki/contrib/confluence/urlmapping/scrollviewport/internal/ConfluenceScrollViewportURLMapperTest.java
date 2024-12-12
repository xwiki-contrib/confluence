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
package org.xwiki.contrib.confluence.urlmapping.scrollviewport.internal;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageIdResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceScrollViewportSpacePrefixResolver;
import org.xwiki.contrib.confluence.urlmapping.internal.ConfluenceURLMappingPrefixHandler;
import org.xwiki.contrib.urlmapping.URLMappingResult;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.query.QueryParameter;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.contrib.confluence.urlmapping.internal.UrlMappingTestTools.assertFailedConversion;

@ComponentTest
@ComponentList({
    ConfluenceScrollViewportFlatURLMapper.class,
    ConfluenceScrollViewportHierarchicalURLMapper.class,
})
public class ConfluenceScrollViewportURLMapperTest
{
    private static final String MY_SPACE = "MySpace";

    private static final String WEB_HOME = "WebHome";

    private static final String XWIKI = "xwiki";

    private static final String MIGRATION_ROOT = "MigrationRoot";

    private static final String GET = "get";

    private static final DocumentReference MY_DOC_REF = new DocumentReference(
        XWIKI,
        List.of(MIGRATION_ROOT, MY_SPACE, "MyDoc"),
        WEB_HOME
    );

    private static final EntityResourceReference DOC_RR = new EntityResourceReference(
        MY_DOC_REF, EntityResourceAction.VIEW);

    @InjectMockComponents
    private ConfluenceURLMappingPrefixHandler handler;

    @MockComponent
    private ConfluencePageIdResolver confluencePageIdResolver;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private Query mockQuery;

    @MockComponent
    private ConfluenceScrollViewportSpacePrefixResolver spacePrefixResolver;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @BeforeComponent
    void setup() throws Exception
    {
        QueryParameter queryParameter = mock(QueryParameter.class);
        when(confluencePageIdResolver.getDocumentById(42)).thenReturn(MY_DOC_REF);
        when(queryManager.createQuery(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.<String>execute()).thenReturn(Collections.emptyList());
        when(mockQuery.setWiki(anyString())).thenReturn(mockQuery);
        when(mockQuery.bindValue(anyString(), anyString())).thenReturn(mockQuery);
        when(mockQuery.bindValue(anyString())).thenReturn(queryParameter);
        when(queryParameter.anyChars()).thenReturn(queryParameter);
        when(queryParameter.query()).thenReturn(mockQuery);
        when(queryParameter.literal(anyString())).thenReturn(queryParameter);
        when(spacePrefixResolver.getSpaceAndPrefixForUrl("prefix/a/myspace/mydoc/webhome")).thenReturn(
            new AbstractMap.SimpleImmutableEntry<>("prefix/a", "mySpace"));
        when(wikiDescriptorManager.getAllIds()).thenReturn(Arrays.asList("xwiki", "sub"));
        when(resolver.resolve(anyString(), any())).thenAnswer(i -> {
            List<String> spaceList = new ArrayList<String>(Arrays.asList(((String) i.getArgument(0)).split("\\.")));
            String pageName = spaceList.remove(spaceList.size() - 1);
            return new DocumentReference(((WikiReference) i.getArgument(1)).getName(), spaceList, pageName);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "prefix/a/1/page-ho-42.html?param=thatwedontcareabout",
        "prefix/a/1/page-ho-42.html"
    })
    void convertViewportFlatURL(String path)
    {
        URLMappingResult converted = handler.convert(path, GET, null);
        assertEquals(DOC_RR, converted.getResourceReference());
        assertEquals("", converted.getURL());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "prefix/a/myspace/mydoc/webhome?param=thatwedontcareabout",
        "prefix/a/myspace/mydoc/webhome"
    })
    void convertViewportHierarchicalURL(String path) throws Exception
    {
        when(mockQuery.<String>execute()).thenReturn(Collections.singletonList("MigrationRoot.MySpace.MyDoc.WebHome"));
        URLMappingResult converted = handler.convert(path, GET, null);
        assertEquals(DOC_RR, converted.getResourceReference());
        assertEquals("", converted.getURL());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "pages/viewpages.action?pageId=42",
        "pages/pages/viewpages.action?pageId=42",
        "download/attachment/42/hello+world.txt",
        "display/display/MySpace/My+Doc?param=thatwedontcareabout",
        "displays/MySpace/My+Doc?param=thatwedontcareabout"
    })
    void dontConvertWrongURL(String path)
    {
        URLMappingResult converted = handler.convert(path, GET, null);
        assertFailedConversion(converted);
    }
}
