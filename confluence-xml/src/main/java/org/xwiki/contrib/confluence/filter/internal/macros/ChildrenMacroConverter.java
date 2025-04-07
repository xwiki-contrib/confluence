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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Children Macro converter.
 * @since 6.62.0
 * @version $Id$
 */
@Component
@Named("children")
@Singleton
public class ChildrenMacroConverter extends AbstractWithSortAndReverseMacroConverter
{
    private static final String ALL_CHILDREN = "allChildren";
    private static final String ROOT = "root";

    @Inject
    private ConfluenceConverter confluenceConverter;

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return shouldConvertToDocumentTree(confluenceParameters) ? "documentTree" : "children";
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.NO;
    }

    private static boolean shouldConvertToDocumentTree(Map<String, String> confluenceParameters)
    {
        String first = getFirst(confluenceParameters);
        return StringUtils.isNotEmpty(getPage(confluenceParameters))
            || StringUtils.isNotEmpty(first);
    }

    private static String getFirst(Map<String, String> confluenceParameters)
    {
        String first = confluenceParameters.get("first");
        if ("0".equals(first)) {
            return null;
        }
        return first;
    }

    private static String getPage(Map<String, String> confluenceParameters)
    {
        return confluenceParameters.get("page");
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> parameters = new HashMap<>();
        String pageTitle = getPage(confluenceParameters);
        // Unfortunately, sometimes children have a link as parameter, sometimes it is bare. In the first case, the
        // reference is already converted. In the second case, it needs to be converted
        // FIXME: find some cleaner way to detect that the reference has already been converted.
        if (StringUtils.isNotEmpty(pageTitle)) {
            parameters.put(
                ROOT, "document:" + (
                    (pageTitle.equals("WebHome") || pageTitle.endsWith(".WebHome"))
                        ? pageTitle
                        : confluenceConverter.convertDocumentReference("", pageTitle)
                )
            );
        } else if (shouldConvertToDocumentTree(confluenceParameters)) {
            parameters.put(ROOT, "document:WebHome");
        }

        String first = getFirst(confluenceParameters);
        if (StringUtils.isNotEmpty(first)) {
            parameters.put("limit", first);
        }

        String allChildren = confluenceParameters.get(ALL_CHILDREN);
        if (StringUtils.isNotEmpty(allChildren) && !"true".equals(allChildren)) {
            markUnhandledParameterValue(confluenceParameters, ALL_CHILDREN);
        }

        handleSortParameter(confluenceParameters, parameters);
        return parameters;
    }
}
