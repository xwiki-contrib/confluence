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

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Provides utility methods for managing and validating Confluence spaces within XWiki during a Confluence import.
 *
 * @version $Id$
 * @since 9.89.0
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
    @Named("local")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private QueryManager queryManager;

    /**
     * Sets the Confluence input properties.
     * @param properties the Confluence input properties
     * @since 9.89.0
     */
    public void setProperties(ConfluenceInputProperties properties)
    {
        this.properties.set(properties);
    }

    /**
     * @return whether it is forbidden to overwrite a given space.
     * @param spaceReference the spaceReference of the space that is checked
     * @since 9.89.0
     */
    public boolean isSpaceOverwriteProtected(SpaceReference spaceReference)
    {
        Set<String> forbiddenSpaces = this.properties.get().getOverwriteProtectedSpaces();

        if (forbiddenSpaces == null || forbiddenSpaces.isEmpty()) {
            return false;
        }

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
     * @param spaceReference the spaceKey of the space that is checked
     * @param confluenceSpacesAreProtected flag that specifies whether to check if another space imported from
     *     Confluence already exists, or to simply search for any space.
     * @return a user reference.
     * @since 9.89.0
     */
    public boolean isCollidingWithAProtectedSpace(SpaceReference spaceReference,
        Boolean confluenceSpacesAreProtected)
    {
        String spaceTargetName = entityReferenceSerializer.serialize(spaceReference);

        String queryString =
            "select 1 from XWikiDocument as doc"
                + " where doc.space = :spaceName or doc.space like :spacePrefix";

        try {
            List<Long> result = queryManager.createQuery(queryString, Query.HQL)
                .setWiki(spaceReference.getWikiReference().getName())
                .bindValue("spaceName", spaceTargetName)
                .bindValue("spacePrefix", spaceTargetName + ".%")
                .execute();

            if (result.isEmpty()) {
                return false;
            }

            if (!confluenceSpacesAreProtected) {
                DocumentReference docRef = new DocumentReference("WebHome", spaceReference);

                try {
                    XWikiContext xContext = contextProvider.get();

                    XWiki xWiki = xContext.getWiki();

                    if (!xWiki.exists(docRef, xContext)) {
                        return true;
                    }

                    String objectQueryString =
                        "select obj "
                            + "from BaseObject as obj, XWikiDocument as doc "
                            + "where obj.name = doc.fullName "
                            + "and doc.fullName = :fullName "
                            + "and obj.className = :className";

                    List<BaseObject> objectQueryResult = queryManager
                        .createQuery(objectQueryString, Query.HQL)
                        .setWiki(docRef.getWikiReference().getName())
                        .bindValue("fullName", entityReferenceSerializer.serialize(docRef))
                        .bindValue("className", "Confluence.Code.ConfluencePageClass")
                        .execute();

                    return objectQueryResult.isEmpty();
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

    /**
     * Retrieves the complete {@link SpaceReference} for the specified space,
     * including the root parameter.
     *
     * @param target the identifier of the space
     * @return the complete {@link SpaceReference} including root
     * @since 9.89.0
     */
    public SpaceReference getSpaceReferenceWithRoot(String target)
    {
        EntityReference rootReference = this.properties.get().getRoot();

        if (rootReference == null) {
            rootReference = contextProvider.get().getWikiReference();
        }
        if (rootReference.getRoot().getType() != EntityType.WIKI) {
            rootReference = rootReference.appendParent(contextProvider.get().getWikiReference());
        }
        return new SpaceReference(target, rootReference);
    }
}
