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

import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;

import org.xwiki.contrib.confluence.filter.AbstractMacroConverter;

/**
 * Convert Confluence plantumlrender macros.
 *
 * @version $Id$
 * @since 9.66.0
 */
@Component (hints = {PlantumlRenderMacroConverter.PLANTUML, PlantumlRenderMacroConverter.PLANTUML_RENDER})
@Singleton
public class PlantumlRenderMacroConverter extends AbstractMacroConverter
{
    /**
     * The ID of the XWiki plantuml macro.
     */
    public static final String PLANTUML = "plantuml";

    /**
     * The ID of the Confluence plantuml render macro.
     */
    public static final String PLANTUML_RENDER = PLANTUML + "render";

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return PLANTUML;
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return "BLOCK".equals(parameters.get("atlassian-macro-output-type"))
            ? InlineSupport.NO
            : InlineSupport.YES;
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> parameters = new LinkedHashMap<>();

        saveParameter(confluenceParameters, parameters, "server", true);
        saveParameter(confluenceParameters, parameters, "title", true);

        for (String parameterName : new String[] { "type", "format" }) {
            String value = confluenceParameters.get(parameterName);
            if (StringUtils.isNotEmpty(value)) {
                parameters.put(parameterName, value.toLowerCase());
            }
        }

        return parameters;
    }

    @Override
    protected String toXWikiContent(String confluenceId, Map<String, String> parameters, String confluenceContent)
    {
        // Remove the white spaces at the ends, including non-breaking spaces that have been observed and are bound to
        // cause issues. The list of characters comes from the following documentation:
        // https://docs.oracle.com/javase/8/docs/api/java/lang/Character.html#isWhitespace-char-
        return StringUtils.strip(confluenceContent, "\n\r\t\f\u00A0\u2007\u202F\u000B\u001C\u001D\u001E\u001F ");
    }
}
