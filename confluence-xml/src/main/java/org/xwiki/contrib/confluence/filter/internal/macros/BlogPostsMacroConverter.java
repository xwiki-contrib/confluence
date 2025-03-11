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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverter;

/**
 * blog-posts macro converter.
 * @since 9.81.0
 * @version $Id$
 */
@Component
@Named("blog-posts")
@Singleton
public class BlogPostsMacroConverter extends AbstractMacroConverter
{

    private static final String CONTENT = "content";
    private static final String TITLES = "titles";
    private static final String FULL = "full";
    private static final String EXCERPTS = "excerpts";
    private static final String[] UNSUPPORTED_PARAMETERS = { "label", "author", "time" };
    private static final String[] UNSUPPORTED_SPACES = {
        "@personal", "@global", "@favorite", "@favourite", "@all", "-", "+", "*" };
    private static final String SELF = "@self";
    private static final Map<String, String> CONFLUENCE_TO_XWIKI_LAYOUT = Map.of(
        TITLES, "link",
        EXCERPTS, FULL,
        "entire", FULL
    );

    private static final String KILLING_THE_CONVERSION = "Killing the conversion.";

    @Inject
    private ConfluenceInputContext context;

    @Inject
    private ConfluenceConverter converter;

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return "blogpostlist";
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> parameters = new HashMap<>();

        killConversionForSomeUnsupportedParameters(confluenceParameters);
        handleSpaces(confluenceParameters, parameters);
        handleMax(confluenceParameters, parameters);
        handleLayout(confluenceParameters, parameters);

        return parameters;
    }

    private static void handleMax(Map<String, String> confluenceParameters, Map<String, String> parameters)
    {
        String max = confluenceParameters.get("max");
        if (StringUtils.isNotEmpty(max)) {
            parameters.put("limit", max);
        }
    }

    private static void killConversionForSomeUnsupportedParameters(Map<String, String> confluenceParameters)
    {
        for (String k : UNSUPPORTED_PARAMETERS) {
            if (StringUtils.isNotEmpty(confluenceParameters.get(k))) {
                throw new RuntimeException(
                    "The blogpostlist macro converter doesn't currently support the " + k + "parameter."
                        + KILLING_THE_CONVERSION);
            }
        }
    }

    private void handleSpaces(Map<String, String> confluenceParameters, Map<String, String> parameters)
    {
        String spaces = confluenceParameters.get("spaces");
        if (StringUtils.isEmpty(spaces)) {
            spaces = SELF;
        }

        for (String spaceVal : UNSUPPORTED_SPACES) {
            if (spaces.contains(spaceVal)) {
                throw new RuntimeException(
                    "The blogpostlist macro converter doesn't currently support this spaces parameter value:" + spaces
                        + KILLING_THE_CONVERSION);
            }
        }

        String space = converter.convertSpaceReference(spaces);
        if (StringUtils.isEmpty(space) || space.startsWith("confluence")) {
            throw new RuntimeException(
                "Failed to convert the spaces parameter [" + spaces + "] of the blogpostlist macro."
                    + KILLING_THE_CONVERSION);
        }

        parameters.put("reference", space + "." + context.getProperties().getBlogSpaceName() + ".WebHome");
    }

    private void handleLayout(Map<String, String> confluenceParameters, Map<String, String> parameters)
    {
        String confluenceLayout = confluenceParameters.get(CONTENT);
        if (StringUtils.isEmpty(confluenceLayout)) {
            confluenceLayout = TITLES;
        }
        String xwikiLayout = CONFLUENCE_TO_XWIKI_LAYOUT.get(confluenceLayout);
        if (StringUtils.isEmpty(xwikiLayout)) {
            markUnhandledParameterValue(confluenceParameters, CONTENT);
        } else {
            parameters.put("layout", xwikiLayout);
        }

        if (EXCERPTS.equals(confluenceLayout)) {
            parameters.put("layoutParams", "displayTitle=true|useSummary=true");
        }
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.NO;
    }
}
