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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.urlmapping.DefaultURLMappingMatch;
import org.xwiki.contrib.urlmapping.suggestions.URLMappingSuggestionUtils;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.stability.Unstable;

/**
 * URL Mapper for Confluence pages with the .html extension ending with the page id.
 * @since 9.77.0
 * @version $Id$
 */
@Component
@Unstable
@Singleton
@Named("html")
public class ConfluenceHTMLURLMapper extends AbstractIDConfluenceURLMapper
{
    @Inject
    private URLMappingSuggestionUtils suggestionUtils;

    /**
     * Constructor.
     */
    public ConfluenceHTMLURLMapper()
    {
        super("(?<prefix>.*)-(?<pageId>\\d+).html");
    }

    @Override
    protected Block getSuggestions(DefaultURLMappingMatch match)
    {
        String prefix = match.getMatcher().group("prefix");
        String[] parts = StringUtils.split(prefix, '/');
        if (parts.length == 2) {
            return suggestionUtils.getSuggestionsFromDocumentReference(new LocalDocumentReference(parts[0], parts[1]));
        }

        return null;
    }
}
