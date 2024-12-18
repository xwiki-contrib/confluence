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
package org.xwiki.contrib.confluence.filter.internal.input;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage;
import org.xwiki.model.reference.EntityReference;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Default implementation of {@link ConfluenceInputContext}.
 * 
 * @version $Id$
 * @since 9.7
 */
@Component
@Singleton
public class DefaultConfluenceInputContext implements ConfluenceInputContext
{
    private final ThreadLocal<ConfluenceInputProperties> properties = new ThreadLocal<>();

    private final ThreadLocal<ConfluenceXMLPackage> confluencePackage = new ThreadLocal<>();

    private final ThreadLocal<String> currentSpace = new ThreadLocal<>();

    private final ThreadLocal<Long> currentPage = new ThreadLocal<>();

    private final ThreadLocal<Map<String, Map<String, EntityReference>>> titleReferenceCache = new ThreadLocal<>();

    private final ThreadLocal<Map<Long, EntityReference>> idReferenceCache = new ThreadLocal<>();

    /**
     * @param confluencePackage the Confluence input package
     * @param properties the Confluence input properties
     */
    public void set(ConfluenceXMLPackage confluencePackage, ConfluenceInputProperties properties)
    {
        this.confluencePackage.set(confluencePackage);
        this.properties.set(properties);
        this.titleReferenceCache.set(new HashMap<>());
        this.idReferenceCache.set(new HashMap<>());
    }

    /**
     * @param space the space to set
     */
    public void setCurrentSpace(String space)
    {
        currentSpace.set(space);
    }

    /**
     * @param pageId the page to set
     */
    public void setCurrentPage(long pageId)
    {
        currentPage.set(pageId);
    }

    /**
     * Clean the current context.
     */
    public void remove()
    {
        this.confluencePackage.remove();
        this.properties.remove();
        this.currentPage.remove();
        this.currentSpace.remove();
        this.titleReferenceCache.remove();
        this.idReferenceCache.remove();
    }

    @Override
    public ConfluenceInputProperties getProperties()
    {
        return this.properties.get();
    }

    @Override
    public ConfluenceXMLPackage getConfluencePackage()
    {
        return this.confluencePackage.get();
    }

    @Override
    public String getCurrentSpace()
    {
        return currentSpace.get();
    }

    @Override
    public Long getCurrentPage()
    {
        return currentPage.get();
    }

    @Override
    public EntityReference getCachedReference(long pageId, Supplier<EntityReference> supplier)
    {
        Map<Long, EntityReference> m = idReferenceCache.get();
        EntityReference ref = m.get(pageId);
        if (ref == null && !m.containsKey(pageId)) {
            // don't replace with compute if absent because null is a valid value
            ref = supplier.get();
            m.put(pageId, ref);
        }
        return ref;
    }

    @Override
    public EntityReference getCachedReference(String spaceKey, String pageTitle, Supplier<EntityReference> supplier)
    {
        Map<String, Map<String, EntityReference>> m = titleReferenceCache.get();
        Map<String, EntityReference> space = m.computeIfAbsent(spaceKey, k -> new HashMap<>());
        EntityReference ref = space.get(pageTitle);
        if (ref == null && !space.containsKey(pageTitle)) {
            // don't replace with compute if absent because null is a valid value
            ref = supplier.get();
            space.put(pageTitle, ref);
        }
        return ref;
    }
}
