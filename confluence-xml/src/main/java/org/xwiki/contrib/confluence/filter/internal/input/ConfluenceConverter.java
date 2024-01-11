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

import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.Mapping;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceReferenceConverter;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.UserResourceReference;

/**
 * Default implementation of ConfluenceConverter.
 * @version $Id$
 * @since 9.26.0
 */
@Component(roles = ConfluenceConverter.class)
@Singleton
public class ConfluenceConverter implements ConfluenceReferenceConverter
{
    private static final Pattern FORBIDDEN_USER_CHARACTERS = Pattern.compile("[. /]");

    private static final String XWIKI = "XWiki";

    @Inject
    private XWikiConverter converter;

    @Inject
    private Logger logger;

    @Inject
    @Named("relative")
    private EntityReferenceResolver<String> relativeResolver;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private ConfluenceInputContext context;

    /**
     * @param name the name to validate
     * @return the validated name
     * @since 9.26.0
     */
    public String toEntityName(String name)
    {
        if (context.getProperties().isConvertToXWiki() && context.getProperties().isEntityNameValidation()) {
            return this.converter.convert(name);
        }

        return name;
    }

    /**
     * @param entityReference the reference to convert
     * @return the converted reference
     * @since 9.26.0
     */
    public EntityReference convert(EntityReference entityReference)
    {
        if (context.getProperties().isConvertToXWiki() && context.getProperties().isEntityNameValidation()) {
            EntityReference newDocumentReference = null;

            for (EntityReference entityElement : entityReference.getReversedReferenceChain()) {
                if (entityElement.getType() == EntityType.DOCUMENT || entityElement.getType() == EntityType.SPACE) {
                    newDocumentReference = new EntityReference(this.converter.convert(entityElement.getName()),
                        entityElement.getType(), newDocumentReference);
                } else if (newDocumentReference == null || entityElement.getParent() == newDocumentReference) {
                    newDocumentReference = entityElement;
                } else {
                    newDocumentReference = new EntityReference(entityElement, newDocumentReference);
                }
            }

            return newDocumentReference;
        }

        return entityReference;
    }

    /**
     * @param entityReference the reference to convert
     * @param entityType the type of the reference
     * @return the converted reference
     * @since 9.26.0
     */
    public String convert(String entityReference, EntityType entityType)
    {
        if (StringUtils.isNotEmpty(entityReference) && context.getProperties().isConvertToXWiki()
            && context.getProperties().isEntityNameValidation()) {
            // Parse the reference
            EntityReference documentReference = this.relativeResolver.resolve(entityReference, entityType);

            // Fix the reference according to entity conversion rules
            documentReference = convert(documentReference);

            // Serialize the fixed reference
            return this.serializer.serialize(documentReference);
        }

        return entityReference;
    }

    private String toMappedUser(String confluenceUser)
    {
        Mapping userIdMapping = context.getProperties().getUserIdMapping();
        if (userIdMapping != null) {
            String mappedName = userIdMapping.getOrDefault(confluenceUser, "").trim();
            if (!mappedName.isEmpty()) {
                return mappedName;
            }
        }

        return confluenceUser;
    }

    /**
     * @param userName the Confluence username
     * @return the corresponding XWiki username, without forbidden characters
     */
    public String toUserReferenceName(String userName)
    {
        if (userName == null || !context.getProperties().isConvertToXWiki()) {
            // Apply the configured mapping
            return toMappedUser(userName);
        }

        // Translate the usual default admin user in Confluence to it's XWiki counterpart
        if (userName.equals("admin")) {
            return "Admin";
        }

        // Apply the configured mapping and protect from characters not well-supported in user page name depending on
        // the version of XWiki
        return FORBIDDEN_USER_CHARACTERS.matcher(toMappedUser(userName)).replaceAll("_");
    }

    /**
     * @param userName the Confluence username
     * @return the corresponding XWiki user reference
     */
    public String toUserReference(String userName)
    {
        // Transform user name according to configuration
        String userReferenceName = toUserReferenceName(userName);
        if (userReferenceName == null || userReferenceName.isEmpty()) {
            return null;
        }

        // Add the "XWiki" space and the wiki if configured. Ideally this should probably be done on XWiki Instance
        // Output filter side
        EntityReference reference = context.getProperties().getUsersWiki() == null
            ? new LocalDocumentReference(XWIKI, userReferenceName)
            : new DocumentReference(context.getProperties().getUsersWiki(), XWIKI, userReferenceName);

        return this.serializer.serialize(reference);
    }

    /**
     * @param reference the reference of a user that can be either a username or a user key.
     * @return a XWiki user reference.
     * @since 9.26
     */
    public ResourceReference resolveUserReference(UserResourceReference reference)
    {
        String userReference = reference.getReference();
        ConfluenceXMLPackage confluencePackage = context.getConfluencePackage();

        if (context.getProperties().isUserReferences()) {
            // Keep the UserResourceReference

            // Clean the user id
            String userName = toUserReferenceName(confluencePackage.resolveUserName(userReference, userReference));
            if (userName == null || userName.isEmpty()) {
                return null;
            }

            reference.setReference(userName);

            return reference;
        }

        // Convert to link to user profile
        // FIXME: would not really been needed if the XWiki Instance output filter was taking care of that when
        // receiving a user reference

        String userName = toUserReference(confluencePackage.resolveUserName(userReference, userReference));
        if (userName == null || userName.isEmpty()) {
            return null;
        }

        DocumentResourceReference documentReference = new DocumentResourceReference(userName);

        documentReference.setParameters(reference.getParameters());

        return documentReference;
    }

    @Override
    public String convertUserReference(String userId)
    {
        return resolveUserReference(new UserResourceReference(userId)).getReference();
    }

    @Override
    public String convertDocumentReference(String parentSpaceReference, String documentReference)
    {
        if (parentSpaceReference == null || parentSpaceReference.isEmpty()) {
            return convert(documentReference, EntityType.DOCUMENT);
        }
        return this.serializer.serialize(convert(new LocalDocumentReference(parentSpaceReference, documentReference)));
    }

    @Override
    public String convertSpaceReference(String spaceReference)
    {
        return convert(spaceReference, EntityType.SPACE);
    }
}
