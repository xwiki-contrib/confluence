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

import java.util.Map;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * Converter used for swagger open API macro.
 *
 * @version $Id$
 * @since 9.64.0
 */
@Singleton
@Component(hints = { "swagger-open-api", "swagger-integration" })
public class SwaggerOpenApiMacroConverter extends AbstractMacroConverter
{
    private static final String[] KNOWN_PARAMETERS = {
        "displayOperationId",
        "operationsSorter",
        "supportedSubmitMethods",
        "tagsSorter",
        "userName",
        "url",
        "token",
        "docExpansion",
        "defaultModelRendering",
        "filter",
        "proxy",
        "validatorUrl",
        "password",
        "showCommonExtensions",
        "maxDisplayedTags",
        "displayRequestDuration",
        "showExtensions"
    };

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        // Useless for now, since we don't override toXWikiParameters, all the parameters are output, but we could
        // decide to warn about unknown parameters in the future even for such macro converters.
        for (String p : KNOWN_PARAMETERS) {
            markHandledParameter(confluenceParameters, p, true);
        }
        return "swagger";
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.NO;
    }
}
