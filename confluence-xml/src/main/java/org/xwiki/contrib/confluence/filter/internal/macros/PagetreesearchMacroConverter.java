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
import java.util.Map;

import org.xwiki.contrib.confluence.filter.AbstractMacroConverter;

/**
 * Pagetreesearch Macro Converter - converts pagetreesearch to the Location Search Macro.
 *
 * See:
 * - https://support.atlassian.com/confluence-cloud/docs/insert-the-page-tree-search-macro/
 * - https://extensions.xwiki.org/xwiki/bin/view/Extension/Location%20Search%20Macro
 *
 * @version $Id$
 * @since 9.80.O
 */
@Component
@Singleton
@Named("pagetreesearch")
public class PagetreesearchMacroConverter extends AbstractMacroConverter
{
    @Inject
    private ConfluenceConverter converter;

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return "locationSearch";
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        String root = confluenceParameters.get("rootPage");
        if (StringUtils.isEmpty(root)) {
            root = "@self";
        }
        return Map.of("reference", converter.convertDocumentReference(null, root));
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.NO;
    }
}
