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

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;

/**
 * Convert Confluence jira macro.
 *
 * @version $Id$
 * @since 9.11.3
 */
@Component(hints = { "jira", "jiraissues" })
@Singleton
public class JiraMacroConverter extends AbstractMacroConverter
{
    private static final String CONFLUENCE_JQL_PARAMETER_NAME = "jqlQuery";

    private static final String XWIKI_PARAM_ID = "id";

    private static final String XWIKI_PARAM_SOURCE = "source";

    private static final String CONFLUENCE_PARAM_KEY = "key";

    private static final String CONFLUENCE_PARAM_SERVER = "server";

    private static final String COMMA = ",";

    enum MacroUsageType
    {
        LIST,
        COUNT,
        SINGLE
    }

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return "jira";
    }

    @Override
    protected String toXWikiContent(String confluenceId, Map<String, String> parameters, String confluenceContent)
    {
        // return the jql query if one is specified
        String jqlQuery = parameters.get(CONFLUENCE_JQL_PARAMETER_NAME);
        if (jqlQuery != null) {
            return jqlQuery;
        }

        // return the content of the key parameter if no jql query is specified
        String keyValue = parameters.get(CONFLUENCE_PARAM_KEY);
        if (keyValue != null) {
            return keyValue;
        }

        // return (empty) confluence content by default
        return confluenceContent;
    }

    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> parameters = new LinkedHashMap<>(confluenceParameters.size());
        parameters.put(XWIKI_PARAM_ID, confluenceParameters.get(CONFLUENCE_PARAM_SERVER));

        switch (getMacroUsageType(confluenceParameters)) {
            case LIST:
                parameters.put("maxCount", confluenceParameters.get("maximumIssues"));
                if (!StringUtils.isBlank(confluenceParameters.get(CONFLUENCE_JQL_PARAMETER_NAME))) {
                    parameters.put(XWIKI_PARAM_SOURCE, "jql");
                }
                parameters.put("fields",
                    convertColumnParams(confluenceParameters.get("columnIds"), confluenceParameters.get("columns")));
                break;
            case COUNT:
                throw new RuntimeException(
                    "Converting the jira macro with count=true is not yet supported");
            case SINGLE:
                parameters.put(XWIKI_PARAM_SOURCE, "list");
                parameters.put("style", "enum");
                break;
            default:
                throw new RuntimeException("Unsupported macro usage type");
        }
        markHandledParameter(confluenceParameters, "serverId", false);
        return parameters;
    }

    private MacroUsageType getMacroUsageType(Map<String, String> confluenceParameters)
    {
        if (confluenceParameters.containsKey(CONFLUENCE_PARAM_KEY)) {
            return MacroUsageType.SINGLE;
        } else if ("true".equals(confluenceParameters.get("count"))) {
            return MacroUsageType.COUNT;
        } else {
            return MacroUsageType.LIST;
        }
    }

    private String convertColumnParams(String columnIds, String columns)
    {
        if (StringUtils.isNotEmpty(columnIds)) {
            String[] columnIdsList = columnIds.split(COMMA);
            String[] columnsNames = columns != null ? columns.split(COMMA) : new String[] {};
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < columnIdsList.length; i++) {
                String columnId = columnIdsList[i];
                String columnName = columnsNames.length > i ? columnsNames[i] : null;
                result.append(columnId);
                if (columnName != null) {
                    result.append(":");
                    result.append(columnName);
                }
                if (i < columnIdsList.length - 1) {
                    result.append(COMMA);
                }
            }
            return result.toString();
        } else {
            return "";
        }
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.NO;
    }
}
