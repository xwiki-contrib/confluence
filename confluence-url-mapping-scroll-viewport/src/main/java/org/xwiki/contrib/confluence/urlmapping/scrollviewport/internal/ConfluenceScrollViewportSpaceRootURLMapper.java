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
import java.util.regex.Matcher;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.contrib.confluence.resolvers.ConfluenceScrollViewportSpacePrefixResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceSpaceKeyResolver;
import org.xwiki.contrib.confluence.urlmapping.ConfluenceURLMapper;
import org.xwiki.contrib.urlmapping.AbstractURLMapper;
import org.xwiki.contrib.urlmapping.DefaultURLMappingMatch;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.stability.Unstable;

/**
 * URL Mapper for Scroll viewport confluence extension URLs. This implementation do only the mapping for url for the
 * root of the space. Note that for a flat or for hierarchical URL the space root URL is composed this way:
 * https://<domain>/<prefix>/<space-prefix>
 *
 * @version $Id$
 * @since 9.77.0
 */
@Component
@Unstable
@Singleton
@Named("scrollViewportRoot")
public class ConfluenceScrollViewportSpaceRootURLMapper extends AbstractURLMapper implements ConfluenceURLMapper
{
    @Inject
    private Logger logger;

    @Inject
    private ConfluenceScrollViewportSpacePrefixResolver prefixResolver;

    @Inject
    private ConfluenceSpaceKeyResolver confluenceSpaceKeyResolver;

    /**
     * Constructor.
     */
    public ConfluenceScrollViewportSpaceRootURLMapper()
    {
        super("^(?!(" + String.join("|", ConfluenceScrollViewportUtils.EXCLUDED_PREFIX_LIST) + ")/)"
            + "(?<fullPath>[\\w-][\\w-/]+[\\w-])/?(\\?(?<params>.*))?$");
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
            if (!entry.getKey().equals(fullPath)) {
                // We are not at the root at the space, leave it for another resolver
                return null;
            }
            EntityReference docRef = null;
            try {
                docRef = confluenceSpaceKeyResolver.getSpaceByKey(entry.getValue());
            } catch (ConfluenceResolverException e) {
                logger.error("Failed to convert URL", e);
            }
            if (docRef == null) {
                return null;
            }
            return new EntityResourceReference(docRef, EntityResourceAction.VIEW);
        } catch (ConfluenceResolverException e) {
            logger.error("Could not convert URL", e);
            return null;
        }
    }
}
