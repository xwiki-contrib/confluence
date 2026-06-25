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

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.filter.FilterException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Provides utility methods for managing and validating Confluence spaces within XWiki during a Confluence import.
 *
 * @version $Id$
 * @since 9.89.0
 */
@Component(roles = ConfluenceSpaceHelpers.class)
@Singleton
public class ConfluenceSpaceHelpers
{
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
     * @param spaceReference the spaceReference of the space that is checked
     * @param overwriteProtectedSpaces the set of Overwrite Protected Spaces
     * @return whether it is forbidden to overwrite a given space.
     * @since 9.89.0
     */
    public boolean isSpaceOverwriteProtected(SpaceReference spaceReference, Set<String> overwriteProtectedSpaces)
    {
        if (overwriteProtectedSpaces == null || overwriteProtectedSpaces.isEmpty()) {
            return false;
        }

        for (String overwriteProtectedSpace : overwriteProtectedSpaces) {
            EntityReference overwriteReference =
                entityReferenceResolver.resolve(overwriteProtectedSpace, EntityType.SPACE);
            if (overwriteReference.equals(spaceReference)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param spaceReference the spaceKey of the space that is checked
     * @param confluenceSpacesAreProtected whether to check if another space imported from
     *     Confluence already exists, or to simply search for any space.
     * @return a user reference.
     * @since 9.89.0
     */
    public boolean isCollidingWithAProtectedSpace(
        SpaceReference spaceReference, boolean confluenceSpacesAreProtected) throws FilterException
    {
        String spaceTargetName = entityReferenceSerializer.serialize(spaceReference);
        String wikiName = spaceReference.getWikiReference().getName();
        try {
            String q = "select 1 from XWikiDocument doc where doc.space = :spaceName or doc.space like :spacePrefix";
            boolean nothingThere = queryManager.createQuery(q, Query.HQL)
                .setWiki(wikiName)
                .bindValue("spaceName", spaceTargetName)
                .bindValue("spacePrefix", spaceTargetName + ".%")
                .setLimit(1)
                .execute()
                .isEmpty();

            if (nothingThere) {
                // nothing to collide with
                return false;
            }

            if (confluenceSpacesAreProtected) {
                // if Confluence spaces are also protected, we don't need to determine whether the content there is
                // a Confluence space
                return true;
            }

            // We are willing to overwrite confluence spaces.
            // To detect whether a space is (in) a Confluence space, we check whether the root WebHome page has a
            // ConfluencePageClass object.
            // Checking if a ConfluencePageClass object is present in the whole subtree would be too broad, we don't
            // want to overwrite a normal space under in which a Confluence space was imported deeper in the
            // hierarchy.
            // if we don't find a ConfluencePageClass object, this is not a Confluence space: collision.
            q = "select 1 from BaseObject obj where obj.className = 'Confluence.Code.ConfluencePageClass' "
                        + "and obj.name = :candidateSpaceHome";
            return queryManager.createQuery(q, Query.HQL)
                .setWiki(wikiName)
                .bindValue("candidateSpaceHome", spaceTargetName + ".WebHome")
                .setLimit(1)
                .execute()
                .isEmpty();
        } catch (QueryException e) {
            throw new FilterException("An exception occurred while checking if a space already exists", e);
        }
    }

    /**
     * Retrieve the complete {@link SpaceReference} for the specified space, including the root parameter.
     *
     * @param target the identifier of the space
     * @param rootReference the root reference of the space
     * @return the complete {@link SpaceReference} including root
     * @since 9.89.0
     */
    public SpaceReference getSpaceReferenceWithRoot(String target, EntityReference rootReference)
    {
        EntityReference newReference = rootReference;
        if (rootReference == null) {
            newReference = contextProvider.get().getWikiReference();
        }

        if (newReference.getRoot().getType() != EntityType.WIKI) {
            newReference = newReference.appendParent(contextProvider.get().getWikiReference());
        }
        return new SpaceReference(target, newReference);
    }
}
