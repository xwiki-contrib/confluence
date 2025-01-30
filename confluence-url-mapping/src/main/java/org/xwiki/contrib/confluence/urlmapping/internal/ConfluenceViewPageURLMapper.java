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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageIdResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageTitleResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
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
import org.xwiki.stability.Unstable;

/**
 * URL Mapper for Confluence "viewpage.action" URLs (linking to pages using an ID, or a space key and a title).
 * @since 9.54.0
 * @version $Id$
 */
@Component
@Unstable
@Singleton
@Named("viewPage")
public class ConfluenceViewPageURLMapper extends AbstractURLMapper implements ConfluenceURLMapper
{

    private static final String SPACE_KEY = "spaceKey";
    private static final String TITLE = "title";
    private static final String PAGE_ID = "pageId";
    private static final String PARAMS = "params";

    @Inject
    private ConfluencePageIdResolver confluenceIdResolver;

    @Inject
    private ConfluencePageTitleResolver confluencePageTitleResolver;

    @Inject
    private URLMappingSuggestionUtils suggestionUtils;

    @Inject
    private Logger logger;

    /**
     * Constructor.
     */
    public ConfluenceViewPageURLMapper()
    {
        super("pages/(?<action>view|edit)page.action\\?(?<params>.*)?");
    }

    /**
     * Used to when this class is used by subclass to convert URL with pageId.
     *
     * @param regex to match the URL. This should contain a named 'pageId' group matcher.
     */
    public ConfluenceViewPageURLMapper(String... regex)
    {
        super(regex);
    }

    // FIXME copy-pasted from BaseConfluenceURLConverter
    private Map<String, String> parseURLParameters(String queryString)
    {
        if (queryString == null) {
            return Collections.emptyMap();
        }

        String[] elements = StringUtils.split(queryString, '&');

        Map<String, String> parameters = new HashMap<>(elements.length);

        for (String element : elements) {
            String[] p = StringUtils.split(element, "=", 2);
            if (p.length == 2) {
                parameters.put(p[0], URLDecoder.decode(p[1], StandardCharsets.UTF_8));
            }
        }

        return parameters;
    }

    @Override
    public ResourceReference convert(DefaultURLMappingMatch match)
    {
        Matcher matcher = match.getMatcher();
        Map<String, String> params = parseURLParameters(matcher.group(PARAMS));
        EntityReference docRef = null;

        try {
            docRef = convert(params);
        } catch (NumberFormatException | ConfluenceResolverException e) {
            logger.error("Could not convert URL", e);
            return null;
        }

        if (docRef == null) {
            return null;
        }
        EntityResourceAction action =  new EntityResourceAction(match.getMatcher().group("action"));
        return new EntityResourceReference(docRef, action);
    }

    private EntityReference convert(Map<String, String> params) throws ConfluenceResolverException
    {
        String pageIdStr = params.get(PAGE_ID);
        if (StringUtils.isNotEmpty(pageIdStr)) {
            return confluenceIdResolver.getDocumentById(Long.parseLong(pageIdStr));
        }

        String spaceKey = params.get(SPACE_KEY);
        String pageTitle = params.get(TITLE);
        if (StringUtils.isNotEmpty(spaceKey) && StringUtils.isNotEmpty(pageTitle)) {
            return confluencePageTitleResolver.getDocumentByTitle(spaceKey, pageTitle);
        }

        return null;
    }

    @Override
    protected Block getSuggestions(DefaultURLMappingMatch match)
    {
        Map<String, String> params = parseURLParameters(match.getMatcher().group(PARAMS));
        String spaceKey = params.get(SPACE_KEY);
        String pageTitle = params.get(TITLE);
        if (StringUtils.isNotEmpty(spaceKey) && StringUtils.isNotEmpty(pageTitle)) {
            return suggestionUtils.getSuggestionsFromDocumentReference(new LocalDocumentReference(spaceKey, pageTitle));
        }
        return null;
    }
}
