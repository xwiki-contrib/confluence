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
package org.xwiki.contrib.confluence.filter.internal.macros;

import org.slf4j.Logger;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverter;
import org.xwiki.component.annotation.Component;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Livesearch Macro Converter - converts livesearch to the Location Search Macro.
 *
 * See:
 * - https://confluence.atlassian.com/doc/livesearch-macro-163415902.html
 * - https://extensions.xwiki.org/xwiki/bin/view/Extension/Location%20Search%20Macro
 *
 * @version $Id$
 * @since 9.80.O
 */
@Component
@Singleton
@Named("livesearch")
public class LivesearchMacroConverter extends AbstractMacroConverter
{
    private static final String KIND = "kind";
    private static final String DOCUMENT = "document";
    private static final String CLASS_NAME = "className";
    private static final String BLOG_POST_CLASS = "Blog.BlogPostClass";
    private static final String COMMENT = "comment";
    private static final String SPACEDESC = "spacedesc";
    private static final String BLOGPOST = "blogpost";
    private static final String PAGE = "page";
    private static final String TAGS = "tags";
    private static final String TYPE = "type";
    private static final String EXCERPT = "excerpt";
    private static final String LARGE = "large";
    private static final String WIDTH = "width";
    private static final String PLACEHOLDER = "placeholder";

    @Inject
    private ConfluenceConverter converter;

    @Inject
    private Logger logger;

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return "locationSearch";
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.NO;
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        String space = confluenceParameters.get("spaceKey");
        Map<String, String> parameters = new HashMap<>();

        if (space != null && !space.isEmpty()) {
            parameters.put("reference", space);
        }

        boolean excerpt = confluenceParameters.getOrDefault("additional", "").contains("none");
        parameters.put("showExcerpts", excerpt ? "true" : "false");

        if (LARGE.equals(confluenceParameters.get("size"))) {
            parameters.put(WIDTH, "100%");
        }

        String placeholder = confluenceParameters.get(PLACEHOLDER);
        if (placeholder != null && !placeholder.isEmpty()) {
            parameters.put(PLACEHOLDER, placeholder);
        }

        String labels = confluenceParameters.get("labels");
        if (labels != null) {
            // same format (comma-separated tags) between XWiki and Confluence, isn't it cute?
            parameters.put(TAGS, labels);
        }

        handleTypeParameter(confluenceParameters, parameters);
        return parameters;
    }

    private void handleTypeParameter(Map<String, String> confluenceParameters, Map<String, String> parameters)
    {
        String type = confluenceParameters.getOrDefault(TYPE, "");
        switch (type) {
            case "":
                break;
            case PAGE:
                parameters.put(KIND, DOCUMENT);
                break;
            case BLOGPOST:
                parameters.put(KIND, DOCUMENT);
                parameters.put(CLASS_NAME, BLOG_POST_CLASS);
                break;
            case COMMENT:
                parameters.put(KIND, COMMENT);
                parameters.put(CLASS_NAME, BLOG_POST_CLASS);
                break;
            case SPACEDESC:
                // Type=spacedesc cannot be supported since Confluence space descriptions are currently not imported
                markUnhandledParameterValue(confluenceParameters, TYPE);
                break;
            default:
                markUnhandledParameterValue(confluenceParameters, TYPE);
                break;
        }
    }
}
