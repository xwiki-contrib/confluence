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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Provides utility methods for managing and validating Confluence spaces within XWiki during a Confluence import.
 *
 * @version $Id$
 * @since 9.88.5
 */
@Component (roles = ConfluenceSpaceHelpers.class)
@Singleton
public class ConfluenceSpaceHelpers
{
    private final ThreadLocal<ConfluenceInputProperties> properties = new ThreadLocal<>();

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Inject
    private QueryManager queryManager;

    /**
     * Sets the Confluence input properties.
     * @param properties the Confluence input properties
     * @since 9.88.5
     */
    public void setProperties(ConfluenceInputProperties properties)
    {
        this.properties.set(properties);
    }

    /**
     * Checks whether a given space is forbidden to be overwritten based on the configured forbidden spaces.
     * @param spaceKey the spaceKey of the space that is checked
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
     * @param spaceKey the spaceKey of the space that is checked
     * @param checkIfConfluenceImported flag that specifies whether to check if another space imported from
     *     Confluence already exists, or to simply search for any space.
     * @return a user reference.
     * @since 9.88.5
     */
    public boolean checkIfSpaceExists(String spaceKey, Boolean checkIfConfluenceImported)
    {
        EntityReference rootReference = this.properties.get().getRoot();

        SpaceReference spaceReference = getRootWithWikiSpaceReference(rootReference, spaceKey);

        String spaceName = spaceReference.getName();

        String queryString =
            "select count(doc) from XWikiDocument as doc"
                + " where doc.space = :spaceName or doc.space like :spacePrefix";

        try {
            List<Long> result = queryManager.createQuery(queryString, Query.HQL)
                .setWiki(spaceReference.getWikiReference().getName())
                .bindValue("spaceName", spaceName)
                .bindValue("spacePrefix", spaceName + ".%")
                .execute();

            if (result.isEmpty() || result.get(0) == 0) {
                return false;
            }

            if (checkIfConfluenceImported) {
                String webHomeString = "WebHome";
                XWikiContext xContext = contextProvider.get();
                DocumentReference docRef;

                if (rootReference != null) {
                    docRef = new DocumentReference(webHomeString, spaceReference);
                } else {
                    docRef = new DocumentReference(xContext.getWikiId(), spaceKey, webHomeString);
                }

                try {
                    XWiki xWiki = xContext.getWiki();

                    XWikiDocument doc = xWiki.getDocument(docRef, xContext);

                    if (!xWiki.exists(docRef, xContext)) {
                        return true;
                    }

                    DocumentReference confluenceClassReference =
                        new DocumentReference(docRef.getRoot().getName(), Arrays.asList("Confluence", "Code"),
                            "ConfluencePageClass");
                    BaseObject obj = doc.getXObject(confluenceClassReference);

                    return obj == null;
                } catch (XWikiException e) {
                    throw new RuntimeException("An exception occurred while checking if a space was already "
                        + "imported!", e);
                }
            }

            return true;
        } catch (QueryException e) {
            throw new RuntimeException("An exception occurred while checking if a space already exists", e);
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
}
