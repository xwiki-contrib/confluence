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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.resolvers.ConfluenceScrollPageIdResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Convert the alert macro.
 *
 * @version $Id$
 * @since 9.53.0
 */
@Singleton
@Component
@Named("includeplus")
public class IncludePlusMacroConverter extends AbstractMacroConverter
{
    private static final String MACRO_ID = "includeLibrary";

    private static final String MACRO_PARAMETER_SCROLLPAGEID = "scrollPageId";

    private static final String MACRO_PARAMETER_REFERENCE = "reference";

    /**
     * Resolver for document references.
     */
    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private ConfluenceScrollPageIdResolver confluenceScrollPageIdResolver;

    /**
     * Default serializer.
     */
    @Inject
    private Provider<EntityReferenceSerializer<String>> defaultSerializerProvider;

    @Inject
    private Logger logger;

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return MACRO_ID;
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.YES;
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> parameters = new HashMap<>();

        try {
            EntityReference entityReference =
                confluenceScrollPageIdResolver.getDocumentById(getScrollPageId(confluenceParameters));
            parameters.put(MACRO_PARAMETER_REFERENCE,
                entityReference != null ? defaultSerializerProvider.get().serialize(entityReference) : "");
        } catch (ConfluenceResolverException e) {
            logger.error("Could not get the referenced page.");
        }

        // Fallback on the confluence parameters, so that in case the conversion goes
        // wrong, a post-migration script could identify all missing data and it could
        // eventually fix the situation.
        parameters = super.toXWikiParameters(confluenceId, confluenceParameters, content);
        parameters.put(MACRO_PARAMETER_REFERENCE, parameters.remove("0"));
        return parameters;
    }

    private Long getScrollPageId(Map<String, String> confluenceParameters)
    {
        if (confluenceParameters.containsKey(MACRO_PARAMETER_SCROLLPAGEID)) {
            return Long.valueOf(MACRO_PARAMETER_SCROLLPAGEID);
        }

        return Long.MIN_VALUE;
    }
}
