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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.confluence.resolvers.ConfluenceSpaceKeyResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.stability.Unstable;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Default ConfluenceSpaceKeyResolver, using the available implementations.
 * @version $Id$
 * @since 9.54.0
 */
@Component
@Unstable
@Singleton
@Priority(900)
public class DefaultConfluenceSpaceResolver extends AbstractConfluenceResolver implements ConfluenceSpaceKeyResolver
{

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public EntityReference getSpaceByKey(String spaceKey) throws ConfluenceResolverException
    {
        for (ConfluenceSpaceKeyResolver r : getResolvers(componentManager, ConfluenceSpaceKeyResolver.class)) {
            if (r != this) {
                EntityReference docRef = r.getSpaceByKey(spaceKey);
                if (docRef != null) {
                    logger.debug("Confluence space [{}] resolved to [{}] using [{}]", spaceKey, docRef, r);
                    return docRef;
                }
            }
        }

        // Fallback.
        // This is a bit optimistic. We'd need to make this work with Confluence spaces migrated in a (non-empty)
        // root space.
        EntityReference spaceRef = new EntityReference(spaceKey, EntityType.SPACE);
        EntityReference spaceHomeRef = new LocalDocumentReference("WebHome", spaceRef);

        XWikiContext xcontext = xcontextProvider.get();
        try {
            if (xcontext.getWiki().getDocument(spaceHomeRef, xcontext).isNew()) {
                return null;
            }
        } catch (XWikiException e) {
            throw new ConfluenceResolverException("Failed to resolve the given space", e);
        }

        logger.debug("Confluence space [{}] resolved to [{}] using fallback", spaceKey, spaceHomeRef);
        return spaceRef;
    }
}
