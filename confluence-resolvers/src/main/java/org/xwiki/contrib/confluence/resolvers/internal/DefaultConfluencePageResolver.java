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

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageIdResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageTitleResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Default ConfluenceIdResolver, using the available implementations.
 * @version $Id$
 * @since 9.54.0
 */
@Component
@Unstable
@Singleton
@Priority(900)
public class DefaultConfluencePageResolver extends AbstractConfluenceResolver
    implements ConfluencePageIdResolver, ConfluencePageTitleResolver
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Override
    public EntityReference getDocumentById(long id) throws ConfluenceResolverException
    {
        for (ConfluencePageIdResolver resolver : getResolvers(componentManager, ConfluencePageIdResolver.class)) {
            if (resolver != this) {
                EntityReference docRef = resolver.getDocumentById(id);
                if (docRef != null) {
                    logger.debug("Confluence document id [{}] resolved to [{}] using [{}]", id, docRef, resolver);
                    return docRef;
                }
            }
        }

        return null;
    }

    @Override
    public EntityReference getDocumentByTitle(String spaceKey, String title) throws ConfluenceResolverException
    {
        for (ConfluencePageTitleResolver resolver : getResolvers(componentManager, ConfluencePageTitleResolver.class)) {
            if (resolver != this) {
                EntityReference docRef = resolver.getDocumentByTitle(spaceKey, title);
                if (docRef != null) {
                    logger.debug("Confluence document title [{}] resolved to [{}] using [{}]", title, docRef, resolver);
                    return docRef;
                }
            }
        }

        return null;
    }
}
