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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.xwiki.contrib.confluence.filter.internal.macros.TracedMap;
import org.xwiki.rendering.listener.Listener;

/**
 * Base class for {@link MacroConverter} implementations.
 * @version $Id$
 * @since 9.82.0
 */
public abstract class AbstractMacroConverter implements MacroConverter
{
    private static final Marker UNHANDLED_PARAMETER_MARKER = MarkerFactory.getMarker("unhandledConfluenceParameter");
    private static final Marker UNHANDLED_PARAMETER_VALUE_MARKER =
        MarkerFactory.getMarker("unhandledConfluenceParameterValue");

    private final Logger
        logger = LoggerFactory.getLogger(AbstractMacroConverter.class.getName());

    @Override
    public void toXWiki(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline, Listener listener)
    {
        TracedMap<String, String> tracedParameters = new TracedMap<>(confluenceParameters);
        String id = toXWikiId(confluenceId, tracedParameters, confluenceContent, inline);
        Map<String, String> parameters = toXWikiParameters(confluenceId, tracedParameters, confluenceContent);
        String content = toXWikiContent(confluenceId, tracedParameters, confluenceContent);
        printUnhandledInfo(confluenceId, tracedParameters);
        listener.onMacro(id, parameters, content, inline);
    }

    /**
     * Convert Confluence parameters to XWiki parameters. If you override toXWiki, toXWikiParameters won't be used. You
     * should return something sensible, or an empty Map.
     * @param confluenceId the name of the macro in Confluence
     * @param confluenceParameters the parameters of the macro in Confluence
     * @param content the content of the macro.
     * @return the converted parameters
     */
    protected abstract Map<String, String> toXWikiParameters(String confluenceId,
        Map<String, String> confluenceParameters,
        String content);

    private void printUnhandledInfo(String confluenceId, TracedMap<String, String> confluenceParameters)
    {
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
     * Convert the macro content.
     * @param confluenceId the name of the macro in Confluence
     * @param parameters the parameters of the macro
     * @param confluenceContent the content in Confluence
     * @return the content in XWiki
     */
    protected String toXWikiContent(String confluenceId, Map<String, String> parameters, String confluenceContent)
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
            warnCantMark();
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
            warnCantMark();
        }
    }

    private void warnCantMark()
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
