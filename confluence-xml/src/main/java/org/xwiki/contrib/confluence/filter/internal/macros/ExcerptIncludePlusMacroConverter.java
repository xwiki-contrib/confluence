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
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.resolvers.ConfluenceScrollPageIdResolver;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverter;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Convert the excerpt-includeplus macro.
 *
 * @version $Id$
 * @since 9.69.0
 */
@Singleton
@Component
@Named("excerpt-includeplus")
public class ExcerptIncludePlusMacroConverter extends AbstractMacroConverter
{
    private static final String MACRO_ID = "excerptIncludeLibrary";

    private static final String MACRO_PARAMETER_SCROLLPAGEID = "scrollPageId";

    private static final String MACRO_PARAMETER_NAME = "name";

    private static final String MACRO_PARAMETER_REFERENCE = "reference";

    @Inject
    private ConfluenceScrollPageIdResolver confluenceScrollPageIdResolver;

    @Inject
    private ConfluenceConverter confluenceConverter;

    /**
     * Default serializer.
     */
    @Inject
    private Provider<EntityReferenceSerializer<String>> serializer;

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
        Throwable cause = null;
        Map<String, String> parameters = new TreeMap<>();

        EntityReference entityReference = null;
        String scrollPageId = confluenceParameters.get(MACRO_PARAMETER_SCROLLPAGEID);
        if (StringUtils.isNotEmpty(scrollPageId)) {
            try {
                Long confluencePageId = confluenceScrollPageIdResolver.getConfluencePageId(scrollPageId);
                if (confluencePageId != null) {
                    entityReference = confluenceConverter.convertDocumentReference(confluencePageId, false);
                }
            } catch (ConfluenceResolverException e) {
                cause = e;
            }
        }

        if (entityReference == null) {
            // We throw a runtime exception so the macro is prevented from being converted, as it doesn't make sense
            // to convert it if we can't resolve the reference. A post migration fix will then be possible using
            // something like the "Replace macros using Macro Converters from XDOM" snippet.
            throw new RuntimeException(String.format("Could not get the referenced page for id [{}]", confluenceId),
                cause);
        }
        parameters.put(MACRO_PARAMETER_REFERENCE, serializer.get().serialize(entityReference));

        String name = confluenceParameters.get(MACRO_PARAMETER_NAME);
        if (name == null) {
            // We throw a runtime exception so the macro is prevented from being converted, as it doesn't make sense
            // to convert it if we can't resolve the name. A post migration fix will then be possible using
            // something like the "Replace macros using Macro Converters from XDOM" snippet.
            throw new RuntimeException(String.format("Could not find the name of the excerpt.", confluenceId));
        }
        parameters.put(MACRO_PARAMETER_NAME, name);

        return parameters;
    }

}
