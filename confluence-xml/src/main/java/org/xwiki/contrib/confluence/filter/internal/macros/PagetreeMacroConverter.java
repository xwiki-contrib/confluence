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

import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.ConfluenceFilterReferenceConverter;

/**
 * Page Tree macro converter.
 * @since 9.80.0
 * @version $Id$
 */
@Component
@Singleton
@Named("pagetree")
public class PagetreeMacroConverter extends AbstractWithSortAndReverseMacroConverter
{
    private static final String ROOT = "root";
    private static final String START_DEPTH = "startDepth";
    private static final String DOT_WEB_HOME = ".WebHome";

    @Inject
    private ConfluenceFilterReferenceConverter confluenceConverter;

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return "documentTree";
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
        // We get it even if we don't use it so it is marked as handled
        String spaces = confluenceParameters.get("spaces");

        String root = confluenceParameters.get(ROOT);

        if (StringUtils.isEmpty(root)) {
            if (StringUtils.isEmpty(spaces)) {
                root = confluenceConverter.convertSpaceReference("@self", true);
            } else {
                //FIXME if several spaces are specified, this probably won't go well...
                if (!spaces.endsWith(DOT_WEB_HOME)) {
                    root = spaces.contains(".")
                        ? spaces + DOT_WEB_HOME
                        : confluenceConverter.convertSpaceReference(spaces, true);
                }
            }
        }

        String startDepth = confluenceParameters.get(START_DEPTH);
        if (StringUtils.isNotEmpty(startDepth) && !"1".equals(startDepth)) {
            markUnhandledParameterValue(confluenceParameters, START_DEPTH);
        }

        Map<String, String> parameters = new TreeMap<>();
        parameters.put(ROOT, "document:" + root);
        handleSortParameter(confluenceParameters, parameters);

        return parameters;
    }
}
