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

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

/**
 * Code macro converter.
 * @since 9.74.0
 * @version $Id$
 */
@Component
@Named(CodeMacroConverter.CODE)
@Singleton
public class CodeMacroConverter extends AbstractMacroConverter
{

    static final String CODE = "code";
    private static final String LANGUAGE = "language";
    private static final String LINENUMBERS = "linenumbers";
    private static final String FIRST_LINE = "firstLine";
    private static final String COLLAPSE = "collapse";
    private static final String FALSE = "false";
    private static final String TITLE = "title";

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return CODE;
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> parameters = new HashMap<>(1);
        String language = confluenceParameters.get(LANGUAGE);
        if (StringUtils.isNotEmpty(language)) {
            parameters.put(LANGUAGE, language);
        }

        if (content != null && containsEndCode(content)) {
            parameters.put("source",  "string:" + content);
        }

        if (isTrue(confluenceParameters, LINENUMBERS)) {
            parameters.put("layout", LINENUMBERS);
        }

        checkUnhandledParameterValues(confluenceParameters);

        return parameters;
    }

    private void checkUnhandledParameterValues(Map<String, String> confluenceParameters)
    {
        String firstLine = confluenceParameters.get(FIRST_LINE);
        if (StringUtils.isNotEmpty(firstLine) && !"1".equals(firstLine)) {
            markUnhandledParameterValue(confluenceParameters, FIRST_LINE);
        }

        String collapse = confluenceParameters.get(COLLAPSE);
        if (StringUtils.isNotEmpty(collapse) && !FALSE.equals(collapse)) {
            markUnhandledParameterValue(confluenceParameters, COLLAPSE);
        }

        if (StringUtils.isNotEmpty(confluenceParameters.get(TITLE))) {
            markUnhandledParameterValue(confluenceParameters, TITLE);
        }
    }

    private boolean isTrue(Map<String, String> confluenceParameters, String parameterName)
    {
        String v = confluenceParameters.get(parameterName);

        if ("true".equals(v)) {
            return true;
        }

        if (StringUtils.isNotEmpty(v) && !FALSE.equals(v)) {
            markUnhandledParameterValue(confluenceParameters, parameterName);
        }

        return false;
    }

    private static boolean containsEndCode(String content)
    {
        return content.contains("{{/code}}");
    }

    @Override
    protected String toXWikiContent(String confluenceId, Map<String, String> parameters, String confluenceContent)
    {
        return (confluenceContent == null || containsEndCode(confluenceContent)) ? null : confluenceContent;
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.NO;
    }
}
