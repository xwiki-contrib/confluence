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

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.input.ConfluenceProperties;
import org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage;
import org.xwiki.contrib.confluence.filter.input.LinkMapper;
import org.xwiki.model.reference.EntityReference;

import javax.inject.Inject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compute the link mapping.
 * @since 9.40.0
 * @version $Id$
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultLinkMapper implements LinkMapper
{
    @Inject
    private ConfluenceInputContext context;

    @Inject
    private ConfluenceConverter converter;

    @Inject
    private Logger logger;

    @Override
    public Map<String, Map<String, EntityReference>> getLinkMapping()
    {
        ConfluenceXMLPackage confluencePackage = context.getConfluencePackage();
        Map<String, Long> spacesByKey = confluencePackage.getSpacesByKey();
        Map<String, Map<String, EntityReference>> mapping = new LinkedHashMap<>(spacesByKey.size());
        Map<Long, List<Long>> pages = confluencePackage.getPages();
        Map<Long, List<Long>> blogPages = confluencePackage.getBlogPages();

        for (Map.Entry<String, Long> spaceEntry : spacesByKey.entrySet()) {
            String spaceKey = spaceEntry.getKey();
            Long spaceId = spaceEntry.getValue();
            List<Long> spacePages = pages.getOrDefault(spaceId, Collections.emptyList());
            List<Long> spaceBlogPages = blogPages.getOrDefault(spaceId, Collections.emptyList());
            Map<String, EntityReference> spaceMapping = new LinkedHashMap<>(spacePages.size() + blogPages.size());

            addMapping(confluencePackage, spacePages, spaceMapping, spaceKey);
            addMapping(confluencePackage, spaceBlogPages, spaceMapping, spaceKey);
            mapping.put(spaceKey, spaceMapping);
        }

        return mapping;
    }

    private void addMapping(ConfluenceXMLPackage confluencePackage, List<Long> pages,
        Map<String, EntityReference> spaceMapping, String spaceKey)
    {
        for (Long pageId : pages) {
            try {
                ConfluenceProperties pageProperties = confluencePackage.getPageProperties(pageId, false);
                String pageTitle = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE, null);
                if (pageTitle != null) {
                    EntityReference docRef = converter.convertDocumentReference(pageProperties, spaceKey, false);
                    if (docRef == null) {
                        logger.warn("Could not produce document reference for page id [{}], title [{}] in space [{}]: "
                                + "the computed reference is null", pageId, pageTitle, spaceKey);
                    } else {
                        spaceMapping.put(pageTitle, docRef);
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not produce document reference for page id [{}] in space [{}]", pageId, spaceKey, e);
            }
        }
    }
}
