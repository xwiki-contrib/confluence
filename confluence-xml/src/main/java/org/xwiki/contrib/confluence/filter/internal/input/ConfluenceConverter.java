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

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.Mapping;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.contrib.confluence.filter.input.ConfluenceProperties;
import org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceReferenceConverter;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.UserResourceReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

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

    private static final String WEB_HOME = "WebHome";

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
        if (!(context.getProperties().isConvertToXWiki() && context.getProperties().isEntityNameValidation())) {
            return entityReference;
        }

        EntityReference parent = entityReference.getParent();
        if (EntityType.ATTACHMENT.equals(entityReference.getType())) {
            EntityReference convertedParent = parent == null ? null : convert(parent);
            return new EntityReference(entityReference.getName(), EntityType.ATTACHMENT, convertedParent);
        }

        if (parent == null && EntityType.DOCUMENT.equals(entityReference.getType())) {
            return toDocumentReference(context.getCurrentSpace(), entityReference.getName());
        }

        if (parent != null && parent.getParent() == null) {
            // The reference is of shape "Space.Doc title", it needs to go through the normal Confluence
            // document reference conversion.
            return toDocumentReference(parent.getName(), entityReference.getName());
        }

        EntityReference newRef = null;

        for (EntityReference entityElement : entityReference.getReversedReferenceChain()) {
            if (entityElement.getType() == EntityType.DOCUMENT || entityElement.getType() == EntityType.SPACE) {
                String name = entityElement.getName();
                String convertedName = this.converter.convert(name);
                if (convertedName == null || convertedName.isEmpty()) {
                    logger.warn("Could not convert entity part [{}] in [{}]. This is a bug, please report it", name,
                        entityReference);
                    return null;
                }
                newRef = new EntityReference(convertedName, entityElement.getType(), newRef);
            } else if (newRef == null || entityElement.getParent() == newRef) {
                newRef = entityElement;
            } else {
                newRef = new EntityReference(entityElement, newRef);
            }
        }

        if (newRef == null) {
            return null;
        }

        SpaceReference root = context.getProperties().getRootSpace();
        if (root != null) {
            EntityType rootType = newRef.getRoot().getType();
            if (EntityType.SPACE.equals(rootType) || EntityType.WIKI.equals(rootType)) {
                // We don't want to add the root if the reference is a local document, otherwise we end up with a
                // broken reference (Root.Document instead of Root.SpaceContainingDocument.Document)
                newRef = newRef.appendParent(root);
            }
        }
        return newRef;
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
            EntityReference reference = this.relativeResolver.resolve(entityReference, entityType);

            // Fix the reference according to entity conversion rules
            reference = convert(reference);

            // Serialize the fixed reference
            return this.serializer.serialize(reference);
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

    // taken from ldap-authenticator (DefaultLDAPDocumentHelper.java)
    private String clean(String str)
    {
        return StringUtils.removePattern(str, "[\\.\\:\\s,@\\^\\/]");
    }

    /**
     * @param groupName the Confluence username
     * @return the corresponding XWiki username, without forbidden characters
     * @since 9.45.0
     */
    public String toGroupReferenceName(String groupName)
    {
        if (groupName == null) {
            return null;
        }

        ConfluenceInputProperties properties = context.getProperties();

        if (!properties.isConvertToXWiki() || properties.getGroupMapping() == null) {
            return groupName;
        }

        String group = properties.getGroupMapping().get(groupName);
        if (group != null) {
            return group;
        }

        String format = properties.getGroupFormat();
        if (StringUtils.isEmpty(format)) {
            return groupName;
        }

        return format.replace("${group}", groupName).replace("${group._clean}", clean(groupName));
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
        return getUserOrGroupReference(toUserReferenceName(userName));
    }

    /**
     * @param groupName the Confluence username
     * @return the corresponding XWiki user reference
     * @since 9.45.0
     */
    public String toGroupReference(String groupName)
    {
        return getUserOrGroupReference(toGroupReferenceName(groupName));
    }

    private String getUserOrGroupReference(String userReferenceName)
    {
        // Transform user name according to configuration
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

    private EntityReference toNonNestedDocumentReference(String spaceKey, String documentName)
    {
        String convertedName = toEntityName(documentName);
        EntityReference space = spaceKey == null ? null : fromSpaceKey(spaceKey);
        return getDocumentReference(new EntityReference(convertedName, EntityType.SPACE, space), false);
    }

    private EntityReference fromSpaceKey(String spaceKey)
    {
        String convertedSpace = toEntityName(ensureNonEmptySpaceKey(spaceKey));
        SpaceReference root = context.getProperties().getRootSpace();
        return new EntityReference(convertedSpace, EntityType.SPACE, root);
    }

    EntityReference convertDocumentReference(long pageId, boolean asSpace) throws ConfigurationException
    {
        ConfluenceXMLPackage confluencePackage = context.getConfluencePackage();

        ConfluenceProperties pageProperties = confluencePackage.getPageProperties(pageId, false);

        if (pageProperties == null) {
            EntityReference docRef = getDocRefFromLinkMapping(pageId);
            if (docRef == null) {
                return null;
            }
            if (asSpace) {
                return new EntityReference(docRef.getName(), EntityType.SPACE, docRef.getParameters());
            }
            return docRef;
        }

        Long spaceId = pageProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_SPACE, null);
        if (spaceId == null) {
            return null;
        }
        return convertDocumentReference(pageProperties, confluencePackage.getSpaceKey(spaceId), asSpace);
    }

    EntityReference convertDocumentReference(ConfluenceProperties pageProperties, String spaceKey,
        boolean asSpace) throws ConfigurationException
    {
        String documentName = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE);

        if (documentName == null) {
            return null;
        }

        return toNestedDocumentReference(spaceKey, documentName, pageProperties, asSpace);
    }

    EntityReference toDocumentReference(String spaceKey, String documentName)
    {
        EntityReference docRef = null;
        String spaceKey1 = ensureNonEmptySpaceKey(spaceKey);

        if (spaceKey1 != null) {
            ConfluenceXMLPackage confluencePackage = context.getConfluencePackage();

            Long pageId = WEB_HOME.equals(documentName)
                ? confluencePackage.getHomePage(confluencePackage.getSpacesByKey().get(spaceKey1))
                : confluencePackage.getPageId(spaceKey1, documentName);
            // FIXME: ConfluenceXWikiGeneratorListener hardcodes a WebHome document name when encountering absolute URLs
            // to Confluence spaces. It would be better to avoid anything XWiki from the links we handle here.
            if (pageId == null) {
                docRef = getDocRefFromLinkMapping(spaceKey1, documentName);
                if (docRef == null) {
                    warnMissingPage(spaceKey1, documentName);
                }
            } else {
                try {
                    ConfluenceProperties pageProperties = confluencePackage.getPageProperties(pageId, false);
                    if (pageProperties != null) {
                        return toNestedDocumentReference(spaceKey1, documentName, pageProperties, false);
                    }
                } catch (ConfigurationException e) {
                    docRef = getDocRefFromLinkMapping(spaceKey1, documentName);
                    if (docRef == null) {
                        this.logger.error("Could not convert link, falling back to non nested conversion", e);
                    }
                }
            }
        }
        return docRef == null ? toNonNestedDocumentReference(spaceKey1, documentName) : docRef;
    }

    private String ensureNonEmptySpaceKey(String spaceKey)
    {
        return (spaceKey == null || spaceKey.equals("currentSpace()"))
            ? context.getCurrentSpace()
            : spaceKey;
    }

    private void warnMissingPage(String spaceKey, String documentName)
    {
        this.logger.warn(
            "Could not find page [{}] in space [{}]. Links to this page may be broken. "
                + "This may happen when importing a space that links to another space which is not present "
                + "in this Confluence export, or the page is missing", documentName, spaceKey);
    }

    private EntityReference getDocRefFromLinkMapping(String spaceKey, String documentName)
    {
        return context.getProperties().getLinkMapping()
            .getOrDefault(spaceKey, Collections.emptyMap()).get(documentName);
    }

    private EntityReference getDocRefFromLinkMapping(long pageId)
    {
        String pageIdString = Long.toString(pageId);
        Map<String, Map<String, EntityReference>> linkMapping = context.getProperties().getLinkMapping();
        for (Map.Entry<String, Map<String, EntityReference>> mappingEntry : linkMapping.entrySet()) {
            // FIXME: this loop might be inefficient, should we group all the [spaceKey]:ids entries once and for all?
            if (mappingEntry.getKey().endsWith(":ids")) {
                Map<String, EntityReference> spaceMapping = mappingEntry.getValue();
                if (spaceMapping != null) {
                    EntityReference docRef = spaceMapping.get(pageIdString);
                    if (docRef != null) {
                        return docRef;
                    }
                }
            }
        }
        return null;
    }

    private EntityReference toNestedDocumentReference(String spaceKey, String documentName,
        ConfluenceProperties pageProperties, boolean asSpace) throws ConfigurationException
    {
        if (StringUtils.isEmpty(documentName)) {
            return null;
        }

        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_HOMEPAGE)) {
            return getDocumentReference(fromSpaceKey(spaceKey), asSpace);
        }

        Long parentId = pageProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_PARENT, null);
        EntityReference parent = null;
        if (parentId != null) {
            parent = convertDocumentReference(parentId, true);
            if (parent == null) {
                EntityReference docRef = getDocRefFromLinkMapping(spaceKey, documentName);
                if (docRef != null) {
                    return docRef;
                }
                warnMissingPage(spaceKey, documentName);
            }
        }

        if (parent == null) {
            // missing parent, let's see if the provided link mapping has it
            EntityReference docRef = getDocRefFromLinkMapping(spaceKey, documentName);
            if (docRef != null) {
                return docRef;
            }

            // if the page has no parent, if the parent page is missing, we consider the space as the parent
            parent = fromSpaceKey(spaceKey);
        }

        String convertedName = toEntityName(documentName);

        if (asSpace && WEB_HOME.equals(convertedName)) {
            return parent;
        }

        if (convertedName == null) {
            return getDocRefFromLinkMapping(spaceKey, documentName);
        }

        return getDocumentReference(new EntityReference(convertedName, EntityType.SPACE, parent), asSpace);
    }

    private static EntityReference getDocumentReference(EntityReference space, boolean asSpace)
    {
        if (asSpace) {
            return space;
        }

        return new EntityReference(WEB_HOME, EntityType.DOCUMENT, space);
    }

    @Override
    public String convertDocumentReference(String parentSpaceReference, String documentReference)
    {
        return this.serializer.serialize(toDocumentReference(parentSpaceReference, documentReference));
    }

    @Override
    public String convertSpaceReference(String spaceReference)
    {
        return this.serializer.serialize(fromSpaceKey(spaceReference));
    }
}
