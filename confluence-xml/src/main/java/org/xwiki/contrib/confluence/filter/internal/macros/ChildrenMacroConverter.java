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
public class ChildrenMacroConverter extends AbstractMacroConverter
{

    private static final String SORT = "sort";
    private static final String REVERSE = "reverse";
    private static final String SORT_AND_REVERSE = "sortAndReverse";
    private static final String ALL_CHILDREN = "allChildren";
    private static final String TRUE = "true";

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
                "root", "document:" + (
                    pageTitle.endsWith(".WebHome")
                        ? pageTitle
                        : confluenceConverter.convertDocumentReference("", pageTitle)
                )
            );
        }

        String first = getFirst(confluenceParameters);
        if (StringUtils.isNotEmpty(first)) {
            parameters.put("limit", first);
        }

        String allChildren = confluenceParameters.get(ALL_CHILDREN);
        if (StringUtils.isNotEmpty(allChildren) && !TRUE.equals(allChildren)) {
            markUnhandledParameterValue(confluenceParameters, ALL_CHILDREN);
        }

        handleSortParameter(confluenceParameters, parameters);
        return parameters;
    }

    private void handleSortParameter(Map<String, String> confluenceParameters, Map<String, String> parameters)
    {
        String sortAndReverse = confluenceParameters.get(SORT_AND_REVERSE);
        String[] sortReverse = StringUtils.isEmpty(sortAndReverse) ? null : sortAndReverse.split("\\.");

        String convertedSort = getConvertedSortParameter(confluenceParameters, sortReverse);
        boolean reverse = getConvertedReverseParameter(confluenceParameters, sortReverse);

        if (convertedSort != null) {
            if (reverse) {
                convertedSort += ":desc";
            }
            parameters.put("sortDocumentsBy", convertedSort);
        }
    }

    private boolean getConvertedReverseParameter(Map<String, String> confluenceParameters, String[] sortReverse)
    {
        String reverse = confluenceParameters.get(REVERSE);
        if (StringUtils.isEmpty(reverse) && sortReverse != null && sortReverse.length > 1) {
            return convertReverse(confluenceParameters, sortReverse[1], SORT_AND_REVERSE);
        }
        return convertReverse(confluenceParameters, reverse, REVERSE);
    }

    private boolean convertReverse(Map<String, String> confluenceParameters, String reverse, String parameterName)
    {
        if (TRUE.equals(reverse)) {
            return true;
        }

        if (StringUtils.isNotEmpty(reverse) && !"false".equals(reverse)) {
            markUnhandledParameterValue(confluenceParameters, parameterName);
        }

        return false;
    }

    private String getConvertedSortParameter(Map<String, String> confluenceParameters, String[] sortReverse)
    {
        String sort = confluenceParameters.get(SORT);
        if (StringUtils.isEmpty(sort) && sortReverse != null && sortReverse.length > 0) {
            return convertSort(confluenceParameters, sortReverse[0], SORT_AND_REVERSE);
        }
        return convertSort(confluenceParameters, sort, SORT);
    }

    private String convertSort(Map<String, String> confluenceParameters, String sort, String parameterName)
    {
        if (StringUtils.isEmpty(sort)) {
            return null;
        }
        switch (sort) {
            case "creation":
                return "creationDate";
            case "title":
                return sort;
            case "modified":
                return "date";
            default:
                markUnhandledParameterValue(confluenceParameters, parameterName);
                return null;
        }
    }
}
