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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * Convert Confluence jira macro.
 * 
 * @version $Id$
 * @since 9.11.3
 */
@Component
@Singleton
@Named("jira")
public class JiraMacroConverter extends AbstractMacroConverter
{
    private static final String CONFLUENCE_JQL_PARAMETER_NAME = "jqlQuery";
    
    @Override
    protected String toXWikiParameterName(String confluenceParameterName, String id,
        Map<String, String> confluenceParameters, String confluenceContent)
    {
        // change the name of the 'server' parameter to 'id'
        if (confluenceParameterName.equals("server")) {
            return "id";
        }

        return super.toXWikiParameterName(confluenceParameterName, id, confluenceParameters, confluenceContent);
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
        String keyValue = parameters.get("key");
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

        for (Map.Entry<String, String> entry : confluenceParameters.entrySet()) {
            String parameterName = toXWikiParameterName(entry.getKey(), confluenceId, confluenceParameters, content);
            String parameterValue =
                toXWikiParameterValue(entry.getKey(), entry.getValue(), confluenceId, confluenceParameters, content);

            parameters.put(parameterName, parameterValue);
            
            // set the source to jql if the jqlQuery parameter is present and has a value
            if (parameterName.equals(CONFLUENCE_JQL_PARAMETER_NAME) && !parameterValue.trim().isEmpty()) {
                parameters.put("source", "jql");
            }
        }

        return parameters;
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.NO;
    }
}
