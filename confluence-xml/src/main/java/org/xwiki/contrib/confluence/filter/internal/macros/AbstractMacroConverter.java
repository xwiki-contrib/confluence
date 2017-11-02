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

import org.xwiki.contrib.confluence.filter.MacroConverter;
import org.xwiki.rendering.listener.Listener;

/**
 * Base class for {@link MacroConverter} implementations.
 * 
 * @version $Id$
 * @since 9.1
 */
public abstract class AbstractMacroConverter implements MacroConverter
{
    @Override
    public void toXWiki(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline, Listener listener)
    {
        String id = toXWikiId(confluenceId, confluenceParameters, confluenceContent, inline);

        Map<String, String> parameters = toXWikiParameters(confluenceId, confluenceParameters, confluenceContent);

        String content = toXWikiContent(confluenceId, confluenceParameters, confluenceContent);

        listener.onMacro(id, parameters, content, inline);
    }

    protected String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return confluenceId;
    }

    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> parameters = new LinkedHashMap<>(confluenceParameters.size());

        for (Map.Entry<String, String> entry : confluenceParameters.entrySet()) {
            String parameterName = toXWikiParameterName(entry.getKey(), confluenceId, parameters, content);
            String parameterValue =
                toXWikiParameterValue(entry.getKey(), entry.getValue(), confluenceId, parameters, content);

            parameters.put(parameterName, parameterValue);
        }

        return parameters;
    }

    protected String toXWikiParameterName(String confluenceParameterName, String id,
        Map<String, String> confluenceParameters, String confluenceContent)
    {
        return confluenceParameterName;
    }

    protected String toXWikiParameterValue(String confluenceParameterName, String confluenceParameterValue,
        String confluenceId, Map<String, String> parameters, String confluenceContent)
    {
        return confluenceParameterValue;
    }

    protected String toXWikiContent(String confluenceId, Map<String, String> parameters, String confluenceContent)
    {
        return confluenceContent;
    }
}
