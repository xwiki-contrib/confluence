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

import org.xwiki.contrib.confluence.filter.ConversionException;
import org.xwiki.contrib.confluence.filter.MacroConverter;

/**
 * Formerly the internal, base class for {@link MacroConverter} implementations.
 * We wanted outside projects to be able to use it, hence the move to public.
 * But since a few projects have been using this internal class, we are giving a grace / transition period and
 * deprecating it instead of removing it directly, not to break them hard. We will remove it soon, so use the public
 * version instead and don't procrastinate doing this.
 * We also want macro converters to be more explicit about the parameter handling, and in particular we don't want
 * Confluence parameters to be implicitly imported when converting macros, so we are deprecating the implementation
 * of {@link AbstractMacroConverter#toXWikiParameters(String, Map, String)}, and methods
 * {@link AbstractMacroConverter#toXWikiParameterName(String, String, Map, String)} and
 * {@link AbstractMacroConverter#toXWikiParameterValue(String, String, String, Map, String)}.
 *
 * @version $Id$
 * @since 9.1
 * @deprecated since 9.89.0, use {@link org.xwiki.contrib.confluence.filter.AbstractMacroConverter} instead
 */
@Deprecated (since = "9.89.0")
public abstract class AbstractMacroConverter extends org.xwiki.contrib.confluence.filter.AbstractMacroConverter
{
    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return confluenceId;
    }

    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content) throws ConversionException
    {
        Map<String, String> parameters = new LinkedHashMap<>(confluenceParameters.size());

        for (Map.Entry<String, String> entry : confluenceParameters.entrySet()) {
            String confluenceParameterName = entry.getKey();
            String parameterName = toXWikiParameterName(
                confluenceParameterName, confluenceId, confluenceParameters, content);
            String confluenceParameterValue = entry.getValue();
            String parameterValue = toXWikiParameterValue(
                confluenceParameterName, confluenceParameterValue, confluenceId, confluenceParameters, content);

            parameters.put(parameterName, parameterValue);
            markHandledParameter(confluenceParameters, confluenceParameterName, true);
        }

        return parameters;
    }

    @Deprecated (since = "9.89.0")
    protected String toXWikiParameterName(String confluenceParameterName, String id,
        Map<String, String> confluenceParameters, String confluenceContent)
    {
        markHandledParameter(confluenceParameters, confluenceParameterName, true);
        if (confluenceParameterName.isEmpty()) {
            // xwiki/2.x syntax does not currently support empty parameter name so we workaround it using the same
            // default parameter name than the Confluence wiki syntax parser
            // TODO: should probably get rid of that hack when https://jira.xwiki.org/browse/XRENDERING-601 is fixed
            return "0";
        }

        return confluenceParameterName;
    }

    @Deprecated (since = "9.89.0")
    protected String toXWikiParameterValue(String confluenceParameterName, String confluenceParameterValue,
        String confluenceId, Map<String, String> parameters, String confluenceContent)
    {
        markHandledParameter(parameters, confluenceParameterName, true);
        return confluenceParameterValue;
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.MAYBE;
    }
}
