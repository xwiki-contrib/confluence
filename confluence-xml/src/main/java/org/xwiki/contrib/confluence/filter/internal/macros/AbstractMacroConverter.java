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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.contrib.confluence.filter.MacroConverter;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverter;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverterListener;
import org.xwiki.model.EntityType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.reference.UserResourceReference;

import javax.inject.Inject;

/**
 * Base class for {@link MacroConverter} implementations.
 * 
 * @version $Id$
 * @since 9.1
 */
public abstract class AbstractMacroConverter implements MacroConverter
{
    private static final String USER_PARAMETER_PREFIX = "user--";

    private static final String SPACES_PARAMETER_PREFIX = "spaces--";

    private static final String DOC_PARAMETER_PREFIX = "doc--";

    private static final String DELIMITER = ",";

    private static final String CONFLUENCE_REGEX_DELIMITER = "\\s*,\\s*";

    @Inject
    private ConfluenceConverter confluenceConverter;

    @Inject
    private ConfluenceInputContext context;

    @Override
    public void toXWiki(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline, Listener listener)
    {
        String id = toXWikiId(confluenceId, confluenceParameters, confluenceContent, inline);

        Map<String, String> parameters = toXWikiParameters(confluenceId, confluenceParameters, confluenceContent);

        String content = toXWikiContent(confluenceId, confluenceParameters, confluenceContent);

        ((ConfluenceConverterListener) listener).getWrappedListener().onMacro(id, parameters, content, inline);
    }

    protected String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return toXWikiMacroName(confluenceId);
    }

    private String toXWikiMacroName(String confluenceMacroName)
    {
        if (!this.context.getProperties().getUnprefixedMacros().isEmpty()) {
            if (this.context.getProperties().getUnprefixedMacros().contains(confluenceMacroName)) {
                return confluenceMacroName;
            }

            // If there is an explicit list of unprefixed macros the others are prefixed
            return this.context.getProperties().getUnknownMacroPrefix() + confluenceMacroName;
        }

        // Check the explicit list of prefixed macros
        if (this.context.getProperties().getPrefixedMacros().contains(confluenceMacroName)) {
            return this.context.getProperties().getUnknownMacroPrefix() + confluenceMacroName;
        }

        // By default macros are not prefixed
        return confluenceMacroName;
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
        String name;
        if (confluenceParameterName.startsWith(USER_PARAMETER_PREFIX)) {
            name = confluenceParameterName.substring(USER_PARAMETER_PREFIX.length());
        } else if (confluenceParameterName.startsWith(SPACES_PARAMETER_PREFIX)) {
            name = confluenceParameterName.substring(SPACES_PARAMETER_PREFIX.length());
        } else if (confluenceParameterName.startsWith(DOC_PARAMETER_PREFIX)) {
            name = confluenceParameterName.substring(DOC_PARAMETER_PREFIX.length());
        } else {
            name = confluenceParameterName;
        }

        if (name.isEmpty()) {
            // xwiki/2.x syntax does not currently support empty parameter name so we workaround it using the same
            // default parameter name than the Confluence wiki syntax parser
            // TODO: should probably get rid of that hack when https://jira.xwiki.org/browse/XRENDERING-601 is fixed
            return "0";
        }

        return name;
    }

    protected String toXWikiParameterValue(String confluenceParameterName, String confluenceParameterValue,
        String confluenceId, Map<String, String> parameters, String confluenceContent)
    {
        if (confluenceParameterName.startsWith(USER_PARAMETER_PREFIX)) {
            List<String> userIds = Arrays.asList(confluenceParameterValue.split(CONFLUENCE_REGEX_DELIMITER));
            userIds.replaceAll(userId -> confluenceConverter.resolveUserReference(
                new UserResourceReference(userId)).getReference());

            return String.join(DELIMITER, userIds);
        }

        if (confluenceParameterName.startsWith(SPACES_PARAMETER_PREFIX)) {
            List<String> spaceRefs = Arrays.asList(confluenceParameterValue.split(CONFLUENCE_REGEX_DELIMITER));
            spaceRefs.replaceAll(spaceRef -> confluenceConverter.convert(spaceRef, EntityType.SPACE));
            return String.join(DELIMITER, spaceRefs);
        }

        if (confluenceParameterName.startsWith(DOC_PARAMETER_PREFIX)) {
            return confluenceConverter.convert(confluenceParameterValue, EntityType.DOCUMENT);
        }

        return confluenceParameterValue;
    }

    protected String toXWikiContent(String confluenceId, Map<String, String> parameters, String confluenceContent)
    {
        return confluenceContent;
    }
}
