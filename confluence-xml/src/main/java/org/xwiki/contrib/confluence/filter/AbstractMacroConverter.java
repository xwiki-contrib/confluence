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
package org.xwiki.contrib.confluence.filter;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.contrib.confluence.filter.internal.macros.TracedMap;
import org.xwiki.rendering.listener.Listener;

/**
 * Base class for {@link MacroConverter} implementations.
 * @version $Id$
 * @since 9.89.0
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
    public final void toXWiki(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline, Listener listener)
    {
        try {
            toXWiki(confluenceId, confluenceParameters, inline, confluenceContent, listener);
        } catch (ConversionException e) {
            // This exception is caught in DefaultMacroConverter
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert the passed macro to the XWiki equivalent.
     *
     * @param confluenceId the Confluence macro id (eg "toc" for the TOC macro)
     * @param confluenceParameters the macro parameters (which can be an unmodifiable map, don't attempt to modify
     *     it)
     * @param inline if true the macro is located in an inline content (like paragraph, etc.)
     * @param confluenceContent the macro content
     * @param listener the listener to send events to
     */
    protected void toXWiki(String confluenceId, Map<String, String> confluenceParameters,
        boolean inline, String confluenceContent, Listener listener) throws ConversionException
    {
        TracedMap<String, String> tracedParameters = new TracedMap<>(confluenceParameters);
        String id = toXWikiId(confluenceId, tracedParameters, confluenceContent, inline);
        Map<String, String> parameters = toXWikiParameters(confluenceId, tracedParameters, confluenceContent);
        parameters = maybeKeepConfluenceParameters(confluenceParameters, parameters,
            tracedParameters.getParametersWithUnhandledValues(), tracedParameters.getUnhandledParameters());
        String content = toXWikiContent(confluenceId, tracedParameters, confluenceContent);
        printUnhandledInfo(confluenceId, tracedParameters);
        listener.onMacro(StringUtils.isEmpty(id) ? "confluence_" : id, parameters, content, inline);
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
        String mode = properties.getMacroParameterKeepingMode();
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

    /**
     * Convert Confluence parameters to XWiki parameters. If you override toXWiki, toXWikiParameters won't be used.
     * You should return something sensible, or an empty Map.
     * @param confluenceId the name of the macro in Confluence
     * @param confluenceParameters the parameters of the macro in Confluence
     * @param content the content of the macro.
     * @return the converted parameters
     */
    protected abstract Map<String, String> toXWikiParameters(String confluenceId,
        Map<String, String> confluenceParameters,
        String content) throws ConversionException;

    /**
     * Convert the macro content.
     * @param confluenceId the name of the macro in Confluence
     * @param parameters the parameters of the macro
     * @param confluenceContent the content in Confluence
     * @return the content in XWiki
     * @throws ConversionException if the conversion cannot continue
     */
    protected String toXWikiContent(String confluenceId, Map<String, String> parameters, String confluenceContent)
        throws ConversionException
    {
        return confluenceContent;
    }

    /**
     * Mark a Confluence parameter as (un)handled.
     * @param confluenceParameters the parameters in Confluence, as passed to toXWikiParameters
     * @param name the name of the parameter to mark as handled
     * @param handled whether the parameter is to be marked as handled
     */
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
            logCantMarkError();
        }
    }

    /**
     * Mark a Confluence parameter value as unhandled.
     * @param confluenceParameters the parameters in Confluence, as passed to toXWikiParameters
     * @param parameterName the name of the parameter of which the value is unhandled.
     */
    protected void markUnhandledParameterValue(Map<String, String> confluenceParameters, String parameterName)
    {
        if (confluenceParameters instanceof TracedMap) {
            TracedMap<String, String> tracedParameters = (TracedMap<String, String>) confluenceParameters;
            tracedParameters.markAsUnhandledValue(parameterName);
        } else {
            logCantMarkError();
        }
    }

    /**
     * Marks the given confluence parameter as handled and put it in the xwiki parameters map using the given xwiki
     * parameter name.
     * @param confluenceParameters the confluence parameters map
     * @param xwikiParameters the xwiki parameters map
     * @param confluenceName  the name of the parameter in Confluence
     * @param xwikiName the name of the parameter in XWiki
     * @param ignoreEmpty consider an empty parameter as if it were not there at all
     * @return the parameter value, or null if the parameter is missing or if it is empty and ignoreEmpty is true
     */
    protected static String saveParameter(Map<String, String> confluenceParameters, Map<String, String> xwikiParameters,
        String confluenceName, String xwikiName, boolean ignoreEmpty)
    {
        String v = confluenceParameters.get(confluenceName);
        if (v != null && !(v.isEmpty() && ignoreEmpty)) {
            xwikiParameters.put(xwikiName, v);
            return v;
        }

        return null;
    }

    /**
     * Marks the given confluence parameter as handled and put it in the xwiki parameters map using the given xwiki
     * parameter name.
     * @param confluenceParameters the confluence parameters map
     * @param xwikiParameters the xwiki parameters map
     * @param confluenceNames  the possible names of the parameter in Confluence
     * @param xwikiName the name of the parameter in XWiki
     * @param ignoreEmpty consider an empty parameter as if it were not there at all
     * @return the parameter value, or null if the parameter is missing or if it is empty and ignoreEmpty is true
     */
    protected static String saveParameter(Map<String, String> confluenceParameters, Map<String, String> xwikiParameters,
        String[] confluenceNames, String xwikiName, boolean ignoreEmpty)
    {
        for (String confluenceName : confluenceNames) {
            String v = saveParameter(confluenceParameters, xwikiParameters, confluenceName, xwikiName, ignoreEmpty);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    /**
     * Marks the given confluence parameter as handled and put it in the xwiki parameters map using the given xwiki
     * parameter name.
     * @param confluenceParameters the confluence parameters map
     * @param xwikiParameters the xwiki parameters map
     * @param name  the name of the parameter in Confluence and XWiki
     * @param ignoreEmpty consider an empty parameter as if it were not there at all
     * @return the parameter value, or null if the parameter is missing or if it is empty and ignoreEmpty is true
     */
    protected static String saveParameter(Map<String, String> confluenceParameters, Map<String, String> xwikiParameters,
        String name, boolean ignoreEmpty)
    {
        return saveParameter(confluenceParameters, xwikiParameters, name, name, ignoreEmpty);
    }

    private void logCantMarkError()
    {
        logger.error(
            "Can't mark parameter as (un)handled or missing. Please pass the original Confluence parameter map.");
    }

    // Code editors might suggest that supportsInlineMode already exists in MacroConverter and can be removed here, but
    // one should not remove it: we want subclass to explicitly implement supportsInlineMode. The default
    // implementation in the interface is there for backward-compatibility but this stuff should not be implicit.
    @Override
    public abstract InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content);
}
