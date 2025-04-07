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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.ConfluenceFilterReferenceConverter;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.UserResourceReference;

import org.xwiki.contrib.confluence.filter.AbstractMacroConverter;

/**
 * Convert mention macro.
 *
 * @version $Id$
 * @since 9.19
 */
@Component
@Singleton
@Named("mention")
public class MentionMacroConverter extends AbstractMacroConverter
{
    private static final String REFERENCE_PARAMETER_KEY = "reference";

    @Inject
    private ConfluenceFilterReferenceConverter confluenceConverter;

    @Inject
    private Logger logger;

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        UserResourceReference userReference =
            new UserResourceReference(confluenceParameters.get(REFERENCE_PARAMETER_KEY));

        ResourceReference reference = confluenceConverter.resolveUserReference(userReference);
        if (reference == null) {
            throw new RuntimeException("Failed to resolve the mentioned user for the mention macro");
        }

        Map<String, String> parameters = new LinkedHashMap<>(3);
        String stringReference = reference.getReference();
        parameters.put(REFERENCE_PARAMETER_KEY, stringReference);
        parameters.put("style", "FULL_NAME");
        parameters.put("anchor", createAnchor(stringReference));
        return parameters;
    }

    protected String createAnchor(String stringReference)
    {
        return stringReference.replace('.', '-') + '-' + RandomStringUtils.random(5, true, false);
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.YES;
    }
}
