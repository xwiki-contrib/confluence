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
import java.util.Collections;
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
    @Inject
    private ConfluenceConverter confluenceConverter;

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return StringUtils.isNotEmpty(getPage(confluenceParameters)) ? "documentTree" : "children";
    }

    private static String getPage(Map<String, String> confluenceParameters)
    {
        return confluenceParameters.get("page");
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        String pageTitle = getPage(confluenceParameters);
        if (StringUtils.isNotEmpty(pageTitle)) {
            // FIXME not sure the following parameter exists, maybe it doesn't, or maybe it does but has another name.
            String spaceKey = confluenceParameters.get("space");
            return Map.of(
                "root", "document:" + confluenceConverter.convertDocumentReference(spaceKey, pageTitle)
            );
        }

        return Collections.emptyMap();
    }
}
