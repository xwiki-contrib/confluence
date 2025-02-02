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
package org.xwiki.contrib.confluence.resolvers.internal;

import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.contrib.confluence.resolvers.ConfluenceScrollTranslationResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceScrollVariantResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Default ConfluenceScrollVersionsResolver, using the available implementations.
 *
 * @version $Id$
 * @since 9.68.0
 */
@Component
@Unstable
@Singleton
@Priority(900)
public class DefaultConfluenceScrollVersionsResolver extends AbstractConfluenceResolver
    implements ConfluenceScrollVariantResolver, ConfluenceScrollTranslationResolver
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Override
    public DocumentReference getEquivalentVariantReference(String attributeId, String attributeValueId)
        throws ConfluenceResolverException
    {
        for (ConfluenceScrollVariantResolver resolver : getResolvers(componentManager,
            ConfluenceScrollVariantResolver.class)) {
            if (resolver != this) {
                DocumentReference variantReference =
                    resolver.getEquivalentVariantReference(attributeId, attributeValueId);
                if (variantReference != null) {
                    logger.debug(
                        "Confluence attribute ID [{}] with atrtibute value ID [{}] resolved to variantReference [{}]"
                            + " using [{}]",
                        attributeId, attributeValueId, resolver);
                    return variantReference;
                }
            }
        }

        return null;
    }

    @Override
    public Map<String, String> getMacroParameters(Long confluenceId, String language) throws ConfluenceResolverException
    {
        for (ConfluenceScrollTranslationResolver resolver : getResolvers(componentManager,
            ConfluenceScrollTranslationResolver.class)) {
            if (resolver != this) {
                Map<String, String> parameters = resolver.getMacroParameters(confluenceId, language);
                if (parameters != null) {
                    logger.debug("Scroll parameters for language [{}] in page with id [{}] resolved to [{}] using [{}]",
                        confluenceId, language, parameters, resolver);
                    return parameters;
                }
            }
        }

        return new TreeMap<String, String>();
    }
}
