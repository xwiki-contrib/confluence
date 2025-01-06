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
package org.xwiki.contrib.confluence.urlmapping.internal;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.contrib.confluence.resolvers.ConfluenceSpaceKeyResolver;
import org.xwiki.contrib.confluence.urlmapping.ConfluenceURLMapper;
import org.xwiki.contrib.urlmapping.AbstractURLMapper;
import org.xwiki.contrib.urlmapping.DefaultURLMappingMatch;
import org.xwiki.contrib.urlmapping.suggestions.URLMappingSuggestionUtils;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
/**
 * URL Mapper for Confluence display (regular) page links.
 * @since 9.54.0
 * @version $Id$
 */
@Component
@Singleton
@Named("spaceDisplay")
public class ConfluenceSpaceDisplayURLMapper extends AbstractURLMapper implements ConfluenceURLMapper
{
    private static final String SPACE_KEY = "spaceKey";

    @Inject
    private ConfluenceSpaceKeyResolver confluenceSpaceKeyResolver;

    @Inject
    private URLMappingSuggestionUtils suggestionUtils;

    @Inject
    private Logger logger;

    /**
     * Constructor.
     */
    public ConfluenceSpaceDisplayURLMapper()
    {
        super("display/(?<spaceKey>[^/?]+)/?(?<params>\\?.*)?");
    }

    @Override
    public ResourceReference convert(DefaultURLMappingMatch match)
    {
        Matcher matcher = match.getMatcher();
        String spaceKey = URLDecoder.decode(matcher.group(SPACE_KEY), StandardCharsets.UTF_8);
        EntityReference docRef = null;
        try {
            docRef = confluenceSpaceKeyResolver.getSpaceByKey(spaceKey);
        } catch (ConfluenceResolverException e) {
            logger.error("Failed to convert URL", e);
        }

        if (docRef == null) {
            return null;
        }

        return new EntityResourceReference(docRef, EntityResourceAction.VIEW);
    }

    @Override
    protected Block getSuggestions(DefaultURLMappingMatch match)
    {
        Matcher matcher = match.getMatcher();
        String spaceKey = matcher.group(SPACE_KEY);
        return suggestionUtils.getSuggestionsFromDocumentReference(new LocalDocumentReference(spaceKey, "WebHome"));
    }
}
