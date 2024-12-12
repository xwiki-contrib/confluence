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

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.contrib.confluence.resolvers.ConfluenceScrollViewportSpacePrefixResolver;
import org.xwiki.contrib.confluence.urlmapping.ConfluenceURLMapper;
import org.xwiki.contrib.urlmapping.AbstractURLMapper;
import org.xwiki.contrib.urlmapping.DefaultURLMappingMatch;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.stability.Unstable;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * URL Mapper for Scroll viewport confluence extension URLs. This implementation do only the mapping for the
 * hierarchical structure. The description of the URL is described
 * <a href="https://help.k15t.com/scroll-viewport-data-center/2.22.0/configure-scroll-viewport">here</a>
 *
 * @version $Id$
 * @since 9.67.0
 */
@Component
@Unstable
@Singleton
@Named("scrollViewportHierarchical")
public class ConfluenceScrollViewportHierarchicalURLMapper extends AbstractURLMapper implements ConfluenceURLMapper
{
    @Inject
    private Logger logger;

    @Inject
    private QueryManager queryManager;

    @Inject
    private ConfluenceScrollViewportSpacePrefixResolver prefixResolver;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    /**
     * Constructor.
     */
    public ConfluenceScrollViewportHierarchicalURLMapper()
    {
        super("^(?!(" + String.join("|", ConfluenceScrollViewportUtils.EXCLUDED_PREFIX_LIST) + ")/)"
            + "(?<fullPath>([\\w-]+/)+([\\w-]+))(\\?(?<params>.*))?$");
    }

    @Override
    public ResourceReference convert(DefaultURLMappingMatch match)
    {
        Matcher matcher = match.getMatcher();
        String fullPath = matcher.group("fullPath");
        try {
            Map.Entry<String, String> entry = prefixResolver.getSpaceAndPrefixForUrl(fullPath);
            if (entry == null) {
                logger.error("Can't find corresponding space for path [{}]", fullPath);
                return null;
            }
            String pathPrefixValue = entry.getKey();
            String spaceName = entry.getValue();
            String pathWithoutPrefix = fullPath.substring(pathPrefixValue.length());
            String documentReferenceSuffix = pathWithoutPrefix.replace('/', '.') + ".webhome";
            String hql = "SELECT doc.fullName "
                + "FROM XWikiDocument as doc, BaseObject as obj, StringProperty as prop "
                + "WHERE obj.name = doc.fullName "
                + "  AND obj.className = 'Confluence.Code.ConfluencePageClass' "
                + "  AND obj.id=prop.id.id "
                + "  AND prop.id.name = 'space' "
                + "  AND prop.value = :space "
                + "  AND LOWER(doc.fullName) LIKE :spaceSuffix";

            for (String wikiId : wikiDescriptorManager.getAllIds()) {
                Query query = queryManager.createQuery(hql, Query.HQL)
                    .setWiki(wikiId)
                    .bindValue("space", spaceName)
                    .bindValue("spaceSuffix").anyChars().literal(documentReferenceSuffix).query();

                Optional<Object> queryRes = query.execute().stream().findFirst();
                if (queryRes.isPresent()) {
                    String fullDocumentReferenceStr = (String) (queryRes.get());
                    DocumentReference reference = resolver.resolve(fullDocumentReferenceStr, new WikiReference(wikiId));
                    return new EntityResourceReference(reference, EntityResourceAction.VIEW);
                }
            }
            logger.error("Can't find corresponding page for path [{}]", fullPath);
            return null;
        } catch (QueryException | ConfluenceResolverException | WikiManagerException e) {
            logger.error("Could not convert URL", e);
            return null;
        }
    }
}
