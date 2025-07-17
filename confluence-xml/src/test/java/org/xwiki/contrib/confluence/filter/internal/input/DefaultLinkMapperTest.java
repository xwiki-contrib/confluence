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
package org.xwiki.contrib.confluence.filter.internal.input;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageIdResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageTitleResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceSpaceKeyResolver;
import org.xwiki.environment.Environment;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.DefaultFileInputSource;
import org.xwiki.job.JobContext;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.validation.EntityNameValidation;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.XWikiTempDirUtil;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@ReferenceComponentList
@ComponentList({
    DefaultConfluenceInputContext.class,
    ConfluenceXMLPackage.class,
    ConfluenceConverter.class
})
@ComponentTest
class DefaultLinkMapperTest
{
    @InjectMockComponents
    private DefaultLinkMapper linkMapper;

    @MockComponent
    private Environment environment;

    @MockComponent
    private EntityNameValidationManager validationManager;

    @MockComponent
    private EntityNameValidation validation;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private JobProgressManager jobProgressManager;

    @MockComponent
    private JobContext jobContext;

    @MockComponent
    private XWikiContext xcontext;

    @InjectMockComponents
    private DefaultStringEntityReferenceResolver resolver;

    @MockComponent
    private ConfluencePageTitleResolver pageTitleResolver;

    @MockComponent
    private ConfluencePageIdResolver pageIdResolver;

    @MockComponent
    private ConfluenceSpaceKeyResolver spaceKeyResolver;

    private MockitoComponentManager componentManager;

    @BeforeComponent
    void before(MockitoComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }
    @BeforeEach
    void setup()
    {
        when(environment.getTemporaryDirectory()).thenReturn(XWikiTempDirUtil.createTemporaryDirectory());
        when(validationManager.getEntityReferenceNameStrategy()).thenReturn(validation);
        when(validation.transform(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0));
        when(validation.transform("spacetovalidate")).thenReturn("validatedspace");
        when(validation.transform("pagetovalidate")).thenReturn("validatedpage");
        XWiki wiki = mock(XWiki.class);
        when(wiki.getDefaultLocale(any())).thenReturn(Locale.ENGLISH);
        when(xcontext.getWiki()).thenReturn(wiki);
    }

    private void prepareTest(String testPackage) throws NoSuchFieldException, IllegalAccessException, ComponentLookupException, IOException, FilterException
    {
        Field contextField = DefaultLinkMapper.class.getDeclaredField("context");
        contextField.setAccessible(true);
        DefaultConfluenceInputContext context = (DefaultConfluenceInputContext) contextField.get(linkMapper);
        ConfluenceInputProperties properties = new ConfluenceInputProperties();
        properties.setRoot(new SpaceReference("xwiki", "Root"));
        ConfluenceXMLPackage confluencePackage = componentManager.getInstance(ConfluenceXMLPackage.class);
        context.set(confluencePackage, properties);

        URL p = getClass().getClassLoader().getResource("confluencexml/" + testPackage);
        assert p != null;
        confluencePackage.read(new DefaultFileInputSource(new File(p.getFile())));
    }

    private Map<String, Map<String, EntityReference>> getLinkMapping(String testPackage) throws Exception
    {
        prepareTest(testPackage);
        return linkMapper.getLinkMapping();
    }

    private EntityReference docRef(String ref) {
        return resolver.resolve(ref, EntityType.DOCUMENT);
    }

    @Test
    void getLinkMappingNested() throws Exception
    {
        // tests a package with nested pages
        Map<String, Map<String, EntityReference>> actual = getLinkMapping("nested");

        Map<String, Map<String, EntityReference>> expected = new LinkedHashMap<>(1);
        Map<String, EntityReference> smallNested = new LinkedHashMap<>(5);
        expected.put("SmallNested", smallNested);

        smallNested.put("Small Nested Home", docRef("xwiki:Root.SmallNested.WebHome"));
        smallNested.put("Page A", docRef("xwiki:Root.SmallNested.Page A.WebHome"));
        smallNested.put("Page B", docRef("xwiki:Root.SmallNested.Page B.WebHome"));
        smallNested.put("Under Page B", docRef("xwiki:Root.SmallNested.Page B.Under Page B.WebHome"));
        smallNested.put("Under Page A", docRef("xwiki:Root.SmallNested.Page A.Under Page A.WebHome"));

        Map<String, EntityReference> smallNestedIds = new LinkedHashMap<>(5);
        expected.put("SmallNested:ids", smallNestedIds);
        smallNestedIds.put("655097973", docRef("xwiki:Root.SmallNested.WebHome"));
        smallNestedIds.put("654934018", docRef("xwiki:Root.SmallNested.Page A.WebHome"));
        smallNestedIds.put("655392769", docRef("xwiki:Root.SmallNested.Page B.WebHome"));
        smallNestedIds.put("655491073", docRef("xwiki:Root.SmallNested.Page B.Under Page B.WebHome"));
        smallNestedIds.put("655360001", docRef("xwiki:Root.SmallNested.Page A.Under Page A.WebHome"));

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getLinkMappingBlogs() throws Exception
    {
        // tests a package with a blog
        Map<String, Map<String, EntityReference>> actual = getLinkMapping("blogs");

        Map<String, Map<String, EntityReference>> expected = new LinkedHashMap<>(1);
        Map<String, EntityReference> blogs = new LinkedHashMap<>(2);
        expected.put("SpaceA", blogs);
        blogs.put("SpaceA Home", docRef("xwiki:Root.SpaceA.WebHome"));
        blogs.put("Blog post", docRef("xwiki:Root.SpaceA.Blog.Blog post"));
        Map<String, EntityReference> blogsIds = new LinkedHashMap<>(2);
        expected.put("SpaceA:ids", blogsIds);
        blogsIds.put("2616328494", docRef("xwiki:Root.SpaceA.WebHome"));
        blogsIds.put("2616164357", docRef("xwiki:Root.SpaceA.Blog.Blog post"));

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getLinkMappingPages() throws Exception
    {
        // tests pages with missing parents, and packages with several spaces
        Map<String, Map<String, EntityReference>> actual = getLinkMapping("pages");

        Map<String, Map<String, EntityReference>> expected = new LinkedHashMap<>(4);

        Map<String, EntityReference> spaceA = new LinkedHashMap<>(2);
        expected.put("SpaceA", spaceA);
        spaceA.put("Page0", docRef("xwiki:Root.SpaceA.Page0.WebHome"));
        spaceA.put("Page1", docRef("xwiki:Root.SpaceA.Page1.WebHome"));

        Map<String, EntityReference> spaceAIds = new LinkedHashMap<>(2);
        expected.put("SpaceA:ids", spaceAIds);
        spaceAIds.put("0", docRef("xwiki:Root.SpaceA.Page0.WebHome"));
        spaceAIds.put("1", docRef("xwiki:Root.SpaceA.Page1.WebHome"));

        Map<String, EntityReference> spaceB = new LinkedHashMap<>(1);
        expected.put("SpaceB", spaceB);
        spaceB.put("Page10", docRef("xwiki:Root.SpaceB.WebHome"));

        Map<String, EntityReference> spaceBIds = new LinkedHashMap<>(1);
        expected.put("SpaceB:ids", spaceBIds);
        spaceBIds.put("10", docRef("xwiki:Root.SpaceB.WebHome"));

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getLinksForPagesWithDots() throws Exception
    {
        // tests page with a dot in the name
        Map<String, Map<String, EntityReference>> actual = getLinkMapping("pageswithdots");

        Map<String, Map<String, EntityReference>> expected = new LinkedHashMap<>(2);

        Map<String, EntityReference> spaceA = new LinkedHashMap<>(3);
        expected.put("testLinks", spaceA);
        spaceA.put("Space Home Page", docRef("xwiki:Root.testLinks.WebHome"));
        spaceA.put("page with links to check", docRef("xwiki:Root.testLinks.page with links to check.WebHome"));
        spaceA.put("1. page with dot in title", docRef("xwiki:Root.testLinks.1\\. page with dot in title.WebHome"));

        Map<String, EntityReference> spaceAIds = new LinkedHashMap<>(3);
        expected.put("testLinks:ids", spaceAIds);
        spaceAIds.put("200", docRef("xwiki:Root.testLinks.WebHome"));
        spaceAIds.put("201", docRef("xwiki:Root.testLinks.page with links to check.WebHome"));
        spaceAIds.put("202", docRef("xwiki:Root.testLinks.1\\. page with dot in title.WebHome"));

        Assertions.assertEquals(expected, actual);
    }
}
