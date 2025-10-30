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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

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

    private final ThreadLocal<Locale> currentLocale = new ThreadLocal<>();

    private final ThreadLocal<Locale> defaultLocale = new ThreadLocal<>();

    private final ThreadLocal<Set<Locale>> currentlyUsedLocales = new ThreadLocal<>();

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private EntityReferenceResolver<String> entityReferenceResolver;

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
        initializeDefaultLocale();
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
        this.currentlyUsedLocales.set(new LinkedHashSet<>());
        this.currentLocale.set(defaultLocale.get());
    }

    /**
     * Checks whether a given space is forbidden to be overwritten based on the configured forbidden spaces.
     * @param spaceKey the space to check
     * @return true if the specified space is listed among the forbidden spaces and cannot be overwritten
     * @since 9.88.5
     */
    public boolean checkIfTheSpaceOverwriteIsForbidden(String spaceKey)
    {
        Set<String> forbiddenSpaces = this.properties.get().getForbiddenSpaces();

        if (forbiddenSpaces == null || forbiddenSpaces.isEmpty()) {
            return false;
        }

        EntityReference rootReference = this.properties.get().getRoot();

        SpaceReference spaceReference = getRootWithWikiSpaceReference(rootReference, spaceKey);

        for (String forbiddenSpace : forbiddenSpaces) {
            EntityReference forbiddenRef =
                entityReferenceResolver.resolve(forbiddenSpace, EntityType.SPACE);
            if (forbiddenRef.equals(spaceReference)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param spaceKey the reference of a user, which can be either a username or a user key.
     * @param checkIfConfluenceImported flag that specifies whether to check if another space imported from
     *     Confluence already exists, or to simply search for any space.
     * @return a user reference.
     * @since 9.88.5
     */
    public boolean checkIfSpaceExists(String spaceKey, Boolean checkIfConfluenceImported)
    {
        String webHomeString = "WebHome";
        XWikiContext xContext = contextProvider.get();

        DocumentReference docRef;

        EntityReference rootReference = this.properties.get().getRoot();

        if (rootReference != null) {
            SpaceReference rootSpaceReference = getRootWithWikiSpaceReference(rootReference, spaceKey);
            docRef = new DocumentReference(webHomeString, rootSpaceReference);
        } else {
            docRef = new DocumentReference(xContext.getWikiId(), spaceKey, webHomeString);
        }

        try {
            XWiki xWiki = xContext.getWiki();

            XWikiDocument doc = xWiki.getDocument(docRef, xContext);

            if (!xWiki.exists(docRef, xContext)) {
                return false;
            }

            if (checkIfConfluenceImported) {
                DocumentReference confluenceClassReference =
                    new DocumentReference(docRef.getRoot().getName(), Arrays.asList("Confluence", "Code"),
                        "ConfluencePageClass");
                BaseObject obj = doc.getXObject(confluenceClassReference);
                return obj != null;
            }

            return true;
        } catch (XWikiException e) {
            throw new RuntimeException("An exception occurred while checking if a space was already imported!");
        }
    }

    private SpaceReference getRootWithWikiSpaceReference(EntityReference rootReference, String spaceKey)
    {
        XWikiContext xContext = contextProvider.get();
        WikiReference wikiReference = new WikiReference(xContext.getWikiId());
        if (rootReference == null) {
            return new SpaceReference(spaceKey, wikiReference);
        }

        if (rootReference.getType().equals(EntityType.SPACE)) {
            SpaceReference rootSpaceReference = new SpaceReference(rootReference,
                rootReference.getParent() == null ? wikiReference
                    :
                    rootReference.getParent());
            return new SpaceReference(spaceKey, rootSpaceReference);
        } else {
            WikiReference wikiRef = new WikiReference(rootReference.extractReference(EntityType.WIKI));
            return new SpaceReference(spaceKey, wikiRef);
        }
    }

    private void initializeDefaultLocale()
    {
        Locale dl = this.properties.get().getDefaultLocale();
        if (dl == null || dl == Locale.ROOT) {
            XWikiContext xcontext = contextProvider.get();
            if (xcontext != null) {
                dl = xcontext.getWiki().getDefaultLocale(xcontext);
            }
        }
        if (dl == null || dl == Locale.ROOT) {
            dl = Locale.ENGLISH;
        }
        this.defaultLocale.set(dl);
    }

    @Override
    public Locale getDefaultLocale()
    {
        return this.defaultLocale.get();
    }

    @Override
    public void setCurrentLocale(Locale locale)
    {
        this.currentLocale.set(locale);
    }

    @Override
    public void addUsedLocale(Locale locale)
    {
        Set<Locale> locales = this.currentlyUsedLocales.get();
        if (locales != null) {
            locales.add(locale);
        }
    }

    @Override
    public Collection<Locale> getCurrentlyUsedLocales()
    {
        return this.currentlyUsedLocales.get();
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
        this.currentLocale.remove();
        this.currentlyUsedLocales.remove();
        this.defaultLocale.remove();
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

    @Override
    public Locale getCurrentLocale()
    {
        return currentLocale.get();
    }
}
