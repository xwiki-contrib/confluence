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

import java.util.Collections;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * Convert the style macro.
 *
 * @version $Id$
 * @since 9.52.0
 */
@Component
@Singleton
@Named("style")
public class StyleMacroConvertor extends AbstractMacroConverter
{
    private static final String IMPORT = "import";

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return "html";
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        return Collections.singletonMap("clean", "false");
    }

    @Override
    protected String toXWikiContent(String confluenceId, Map<String, String> parameters, String confluenceContent)
    {
        StringBuilder stringBuilder = new StringBuilder();

        if (confluenceContent != null && !confluenceContent.isEmpty()) {
            stringBuilder.append("<style>");
            stringBuilder.append(confluenceContent);
            stringBuilder.append("</style>");
        }

        if (parameters.containsKey(IMPORT)) {
            stringBuilder.append("<link rel=\"stylesheet\"");
            stringBuilder.append("href=");
            stringBuilder.append(parameters.get(IMPORT));
            stringBuilder.append(">");
        }
        return stringBuilder.toString();
    }
}


