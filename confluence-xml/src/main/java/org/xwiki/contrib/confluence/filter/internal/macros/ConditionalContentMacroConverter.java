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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.resolvers.ConfluenceScrollVariantResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Convert the Conditional Content macro.
 *
 * @version $Id$
 * @since 9.71.0
 */
@Singleton
@Component
@Named("conditionalcontent")
public class ConditionalContentMacroConverter extends AbstractMacroConverter
{
    private static final String MACRO_ID = "variant";

    private static final String MACRO_PARAMETER_NAME = "name";

    private static final String CONFLUENCE_PARAMETER_PREFIX = "sv-attr:";

    /**
     * Default serializer.
     */
    @Inject
    private Provider<EntityReferenceSerializer<String>> serializer;

    @Inject
    private Provider<ConfluenceScrollVariantResolver> variantResolverProvider;

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

        List<String> variantReferences = new ArrayList<String>();

        for (Entry<String, String> confluenceParameter : confluenceParameters.entrySet()) {
            String attributePrefixedID = confluenceParameter.getKey();
            if (attributePrefixedID.startsWith(CONFLUENCE_PARAMETER_PREFIX)) {
                String attributeID = attributePrefixedID.replaceFirst(CONFLUENCE_PARAMETER_PREFIX, "");
                String parameterValues = confluenceParameter.getValue();

                List<String> attributeValueIDs = new ArrayList<String>();

                if (parameterValues != null) {
                    attributeValueIDs = Stream.of(parameterValues.split(" ")).collect(Collectors.toList());
                }

                for (String attributeValueId : attributeValueIDs) {
                    try {
                        variantReferences.add(serializer.get().serialize(variantResolverProvider.get()
                            .getEquivalentVariantReference(attributeID, attributeValueId)));
                    } catch (ConfluenceResolverException e) {
                        cause = e;
                    }
                }
            }
        }

        if (variantReferences.isEmpty()) {
            // we throw a runtime exception so the macro is prevented from being converted, as it doesn't make sense
            // to convert it if we can't resolve the variant references. A post migration fix will then be possible
            // using
            // something like the "Replace macros using Macro Converters from XDOM" snippet.
            throw new RuntimeException(String.format("Could not generate variant references for id [{}]", confluenceId),
                cause);
        }

        return Map.of(MACRO_PARAMETER_NAME, variantReferences.stream()
            .filter(reference -> Objects.nonNull(reference)).collect(Collectors.joining(",")));
    }

}
