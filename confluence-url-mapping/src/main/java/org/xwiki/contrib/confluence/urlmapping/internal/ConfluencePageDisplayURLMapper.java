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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.contrib.urlmapping.AbstractURLMapper;
import org.xwiki.contrib.urlmapping.DefaultURLMappingMatch;
import org.xwiki.contrib.urlmapping.suggestions.URLMappingSuggestionUtils;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageIdResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageTitleResolver;

/**
 * URL Mapper for Confluence display (regular) page links.
 * @since 9.54.0
 * @version $Id$
 */
@Component
@Singleton
@Named("pageDisplay")
public class ConfluencePageDisplayURLMapper extends AbstractURLMapper implements ConfluenceURLMapper
{
    private static final String SPACE_KEY = "spaceKey";

    private static final String PAGE_TITLE = "pageTitle";

    @Inject
    private ConfluencePageIdResolver confluencePageIdResolver;

    @Inject
    private ConfluencePageTitleResolver confluencePageTitleResolver;

    @Inject
    private URLMappingSuggestionUtils suggestionUtils;

    @Inject
    private Logger logger;

    /**
     * Constructor.
     */
    public ConfluencePageDisplayURLMapper()
    {
        super(
            "display/(?<spaceKey>[^/]+)/(?<pageTitle>[^?/#]+)(?<params>\\?.*)?",
            "spaces/(?<spaceKey>[^/]+)/pages/(?<pageId>\\d+)/(?<pageTitle>[^?/#]+)(?<params>\\?.*)?"
        );
    }

    @Override
    public ResourceReference convert(DefaultURLMappingMatch match)
    {
        Matcher matcher = match.getMatcher();
        String spaceKey = matcher.group(SPACE_KEY);
        String pageTitle = URLDecoder.decode(matcher.group(PAGE_TITLE), StandardCharsets.UTF_8);
        EntityReference docRef = null;
        try {
            docRef = confluencePageTitleResolver.getDocumentByTitle(spaceKey, pageTitle);
            if (docRef == null) {
                try {
                    // Let's see if the URL contains a pageId
                    String pageIdStr = matcher.group("pageId");
                    if (pageIdStr != null) {
                        long pageId = Long.parseLong(pageIdStr);
                        docRef = confluencePageIdResolver.getDocumentById(pageId);
                    }
                } catch (IllegalArgumentException e) {
                    // The url doesn't contain a pageId, the regex doesn't contain such a named group
                }
            }
        } catch (NumberFormatException | ConfluenceResolverException e) {
            // NumberFormatException can be thrown by Long.parseLong()
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
        String pageTitle = URLDecoder.decode(matcher.group(PAGE_TITLE), StandardCharsets.UTF_8);
        return suggestionUtils.getSuggestionsFromDocumentReference(new LocalDocumentReference(spaceKey, pageTitle));
    }
}
