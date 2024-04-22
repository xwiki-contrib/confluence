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
package org.xwiki.contrib.confluence.filter;

import org.slf4j.Logger;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverter;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.internal.macros.AbstractMacroConverter;
import org.xwiki.model.EntityType;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Component
@Singleton
@Named("livesearch")
public class LivesearchMacroConverter extends AbstractMacroConverter
{
    @Inject
    private ConfluenceConverter converter;

    @Inject
    private Logger logger;

    @Override
    protected String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return "locationSearch";
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        String space = confluenceParameters.get("spaceKey");
        Map<String, String> parameters = new HashMap<>();

        if (space != null && !space.isEmpty()) {
            parameters.put("reference", converter.convertSpaceReference(space, true));
        }

        boolean excerpt = confluenceParameters.getOrDefault("additional", "").contains("excerpt");
        parameters.put("excerpt", excerpt ? "true" : "false");

        if ("large".equals(confluenceParameters.get("size"))) {
            parameters.put("width", "100%");
        }

        String placeholder = confluenceParameters.get("placeholder");
        if (placeholder != null && !placeholder.isEmpty()) {
            parameters.put("placeholder", placeholder);
        }

        String labels = confluenceParameters.get("labels");
        if (labels != null) {
            // same format (comma-separated tags) between XWiki and Confluence, isn't it cute?
            parameters.put("tags",labels);
        }

        String type = confluenceParameters.getOrDefault("type", "");
        switch (type) {
            case "":
                break;
            case "page":
                parameters.put("kind", "document");
                break;
            case "blogpost":
                parameters.put("kind", "document");
                parameters.put("className", "Blog.BlogPostClass");
                break;
            case "comment":
                parameters.put("kind", "comment");
                parameters.put("className", "Blog.BlogPostClass");
                break;
            case "spacedesc":
                logger.warn("livesearch's type='spacedesc' parameter is not supported, as Confluence's space descriptions are currently not imported into XWiki");
                break;
            default:
                logger.warn("livesearch's type='[{}] parameter' is not supported", type);
                break;
        }
        return parameters;
    }
}
