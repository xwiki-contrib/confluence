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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.xwiki.contrib.confluence.filter.MacroConverter;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.rendering.listener.Listener;

/**
 * Base class for {@link MacroConverter} implementations.
 *
 * @version $Id$
 * @since 9.1
 */
public abstract class AbstractMacroConverter implements MacroConverter
{
    private static final String ATLASSIAN_MACRO_OUTPUT_TYPE = "atlassian-macro-output-type";
    private static final Marker UNHANDLED_PARAMETER_MARKER = MarkerFactory.getMarker("unhandledConfluenceParameter");
    private static final Marker UNHANDLED_PARAMETER_VALUE_MARKER =
        MarkerFactory.getMarker("unhandledConfluenceParameterValue");

    private final Logger logger = LoggerFactory.getLogger(AbstractMacroConverter.class.getName());

    @Inject
    private ConfluenceInputContext inputContext;

    @Override
    public void toXWiki(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline, Listener listener)
    {
        TracedMap<String, String> tracedParameters = new TracedMap<>(confluenceParameters);

        String id = toXWikiId(confluenceId, tracedParameters, confluenceContent, inline);

        Map<String, String> parameters = toXWikiParameters(confluenceId, tracedParameters, confluenceContent);

        parameters = maybeKeepConfluenceParameters(confluenceParameters, parameters,
            tracedParameters.getParametersWithUnhandledValues(), tracedParameters.getUnhandledParameters());

        String content = toXWikiContent(confluenceId, tracedParameters, confluenceContent);

        printUnhandledInfo(confluenceId, tracedParameters);

        listener.onMacro(id, parameters, content, inline);
    }

    private Map<String, String> maybeKeepConfluenceParameters(Map<String, String> confluenceParameters,
        Map<String, String> parameters, Collection<String> parametersWithUnhandledValues,
        Collection<String> unhandledParameters)
    {
        if (inputContext == null) {
            logger.info("Could not determine the keeping parameter mode, assuming NONE");
            return parameters;
        }

        ConfluenceInputProperties properties = inputContext.getProperties();
        String mode = properties.getKeptMacroParameterMode();
        if (StringUtils.isEmpty(mode) || mode.equals("NONE")) {
            return parameters;
        }

        String prefix = properties.getKeptMacroParameterPrefix();

        Map<String, String> newParameters = null;
        if (mode.equals("ALL")) {
            for (Map.Entry<String, String> confluenceParameter : confluenceParameters.entrySet()) {
                newParameters = maybeAddKeptParameter(parameters, prefix, newParameters,
                    confluenceParameter.getKey(), confluenceParameter.getValue());
            }
        } else if (mode.equals("UNHANDLED")) {
            newParameters = addUnhandledParameters(confluenceParameters, parameters, parametersWithUnhandledValues,
                newParameters, prefix);
            newParameters = addUnhandledParameters(confluenceParameters, parameters, unhandledParameters,
                newParameters, prefix);
        } else {
            logger.error("Unexpected keeping parameter mode [{}]. Assuming NONE. This should not happen.", mode);
        }

        return newParameters == null ? parameters : newParameters;
    }

    private static Map<String, String> addUnhandledParameters(Map<String, String> confluenceParameters,
        Map<String, String> parameters, Collection<String> parametersWithUnhandledValues,
        Map<String, String> newParameters, String prefix)
    {
        Map<String, String> np = newParameters;
        for (String parameterName : parametersWithUnhandledValues) {
            String parameterValue = confluenceParameters.get(parameterName);
            np = maybeAddKeptParameter(parameters, prefix, newParameters, parameterName, parameterValue);
        }
        return np;
    }

    private static Map<String, String> maybeAddKeptParameter(Map<String, String> parameters,
        String prefix, Map<String, String> newParameters, String parameterName, String parameterValue)
    {
        Map<String, String> np = newParameters;
        String keptParameterName =
            (ATLASSIAN_MACRO_OUTPUT_TYPE.equals(parameterName) || parameterName.startsWith(prefix))
            ? parameterName
            : prefix + parameterName;
        if (!parameters.containsKey(parameterName) && !parameters.containsKey(keptParameterName)) {
            if (np == null) {
                np = new TreeMap<>(parameters);
            }
            np.put(keptParameterName, parameterValue);
        }
        return np;
    }

    private void printUnhandledInfo(String confluenceId, TracedMap<String, String> confluenceParameters)
    {
        if (!logger.isInfoEnabled()) {
            return;
        }

        Collection<String> parametersWithUnhandledValues = confluenceParameters.getParametersWithUnhandledValues();
        Collection<String> unhandledParameters = confluenceParameters.getUnhandledParameters();

        for (String p : unhandledParameters) {
            if (!parametersWithUnhandledValues.contains(p)) {
                logger.info(UNHANDLED_PARAMETER_MARKER, "Unhandled parameter [{}] (with value [{}]) in macro [{}]",
                    p, confluenceParameters.get(p), confluenceId);
            }
        }

        for (String p : parametersWithUnhandledValues) {
            logger.info(UNHANDLED_PARAMETER_VALUE_MARKER, "Unhandled value [{}] for parameter [{}] in macro [{}]",
                confluenceParameters.get(p), p, confluenceId);
        }
    }

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return confluenceId;
    }

    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
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

    protected String toXWikiParameterValue(String confluenceParameterName, String confluenceParameterValue,
        String confluenceId, Map<String, String> parameters, String confluenceContent)
    {
        markHandledParameter(parameters, confluenceParameterName, true);
        return confluenceParameterValue;
    }

    protected String toXWikiContent(String confluenceId, Map<String, String> parameters, String confluenceContent)
    {
        return confluenceContent;
    }

    protected void markHandledParameter(Map<String, String> confluenceParameters, String name, boolean handled)
    {
        if (confluenceParameters instanceof TracedMap) {
            TracedMap<String, String> tracedParameters = (TracedMap<String, String>) confluenceParameters;
            if (handled) {
                tracedParameters.markAsUsed(name);
            } else {
                tracedParameters.markAsUnused(name);
            }
        } else {
            warnCantMark();
        }
    }

    protected void markUnhandledParameterValue(Map<String, String> confluenceParameters, String parameterName)
    {
        if (confluenceParameters instanceof TracedMap) {
            TracedMap<String, String> tracedParameters = (TracedMap<String, String>) confluenceParameters;
            tracedParameters.markAsUnhandledValue(parameterName);
        } else {
            warnCantMark();
        }
    }

    private void warnCantMark()
    {
        logger.error(
            "Can't mark parameter as (un)handled or missing. Please pass the original Confluence parameter map.");
    }
}
