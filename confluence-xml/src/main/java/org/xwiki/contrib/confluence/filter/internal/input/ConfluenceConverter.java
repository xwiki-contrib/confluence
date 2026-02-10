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

import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.confluence.filter.ConfluenceFilterReferenceConverter;
import org.xwiki.contrib.confluence.filter.Mapping;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.contrib.confluence.filter.input.ConfluenceProperties;
import org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceURLConverter;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageIdResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageTitleResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.contrib.confluence.resolvers.ConfluenceSpaceKeyResolver;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.listener.reference.UserResourceReference;

import com.xpn.xwiki.XWikiContext;

import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_GROUP_EXTERNAL_ID;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_GROUP_NAME;

/**
 * Default implementation of ConfluenceFilterReferenceConverter.
 *
 * Note: While the implementation itself is not deprecated, the ConfluenceConverter role is deprecated and will
 * eventually go away. We give a grace period to let projects switch to {@link ConfluenceFilterReferenceConverter},
 * ideally we should get rid of this deprecated role early 2027. We use the deprecated annotations to make the IDEs
 * warn against using it.
 *
 * @version $Id$
 * @since 9.26.0
 * @deprecated since 9.89.0, use {@link ConfluenceFilterReferenceConverter} instead
 */
@Component (roles = {ConfluenceFilterReferenceConverter.class, ConfluenceConverter.class})
@Singleton
@Deprecated (since = "9.89.0")
public class ConfluenceConverter implements ConfluenceFilterReferenceConverter
{
    private static final Pattern FORBIDDEN_USER_CHARACTERS = Pattern.compile("[. /]");

    private static final String XWIKI = "XWiki";

    private static final String WEB_HOME = "WebHome";

    private static final String AT_SELF = "@self";

    private static final String AT_HOME = "@home";

    private static final String AT_NONE = "@none";

    private static final String AT_PARENT = "@parent";

    private static final String CONFLUENCE_REF_EXPLANATION = "A Confluence reference will be used if possible. "
        + "Consider converting this reference later with a post import fix.";

    private static final String FAILED_TO_RESOLVE_SPACE = "Failed to resolve space [{}]. " + CONFLUENCE_REF_EXPLANATION;

    private static final Marker CONFLUENCE_REF_MARKER = MarkerFactory.getMarker("confluenceRef");

    private static final EntityReference GUEST = new EntityReference(
        "XWikiGuest", EntityType.DOCUMENT, new SpaceReference("xwiki", XWIKI));

    @Inject
    private Provider<EntityNameValidationManager> entityNameValidationManagerProvider;

    @Inject
    private Logger logger;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiSerializer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ConfluenceInputContext context;

    @Inject
    private ConfluencePageIdResolver pageIdResolver;

    @Inject
    private ConfluenceSpaceKeyResolver spaceKeyResolver;

    @Inject
    private ConfluencePageTitleResolver pageTitleResolver;

    @Inject
    private Provider<ComponentManager> componentManagerProvider;

    /**
     * @param name the name to validate
     * @return the validated name
     * @since 9.26.0
     */
    public String toEntityName(String name)
    {
        if (context.getProperties().isConvertToXWiki() && context.getProperties().isEntityNameValidation()) {
            return applyNamingStrategy(name);
        }

        return name;
    }

    private String applyNamingStrategy(String entityName)
    {
        String r = entityNameValidationManagerProvider.get().getEntityReferenceNameStrategy().transform(entityName);
        if (StringUtils.isEmpty(r)) {
            logger.warn("The name strategy transformed [{}] to an empty name, will use the original name instead of"
                + " following the name strategy", entityName);
            return entityName;
        }
        return r;
    }

    private EntityReference getRoot()
    {
        EntityReference root = context.getProperties().getRoot();
        if (root == null) {
            XWikiContext xcontext = contextProvider.get();
            if (xcontext == null) {
                return null;
            }
            root = xcontext.getWikiReference();
        }
        return root;
    }

    /**
     * @param groupName the Confluence username
     * @return the corresponding XWiki username, without forbidden characters
     * @since 9.45.0
     */
    public String toGroupReferenceName(String groupName)
    {
        ConfluenceInputProperties properties = context.getProperties();
        if (StringUtils.isEmpty(groupName) || !properties.isConvertToXWiki()) {
            return groupName;
        }

        Mapping groupMapping = properties.getGroupMapping();
        if (groupMapping != null) {
            String group = groupMapping.get(groupName);
            if (group != null) {
                return group;
            }
        }

        String format = properties.getGroupFormat();
        if (StringUtils.isEmpty(format)) {
            return groupName;
        }

        return UsernameCleaner.format(format, Map.of("group", groupName));
    }

    /**
     * @return the user reference name in XWiki corresponding to the given Confluence username
     * @param userName the name of the user
     * @deprecated since 9.89.0, use {@link #convertUserNameToReferenceName(String)}
     */
    @Deprecated (since = "9.89.0")
    public String toUserReferenceName(String userName)
    {
        return convertUserNameToReferenceName(userName);
    }

    @Override
    public String convertUserNameToReferenceName(String userName)
    {
        if (StringUtils.isEmpty(userName) || !context.getProperties().isConvertToXWiki()) {
            return userName;
        }

        // Apply the configured mapping
        Mapping userIdMapping = context.getProperties().getUserIdMapping();
        if (userIdMapping != null) {
            String mappedName = userIdMapping.getOrDefault(userName, "").trim();
            if (!mappedName.isEmpty()) {
                return mappedName;
            }
        }

        // Translate the usual default admin user in Confluence to its XWiki counterpart
        if (userName.equals("admin")) {
            return "Admin";
        }

        // Apply the user format
        String userFormat = context.getProperties().getUserFormat();
        if (StringUtils.isEmpty(userFormat)) {
            // Do some minimal cleanup which is backward compatible with older versions of the filter.
            return FORBIDDEN_USER_CHARACTERS.matcher(userName).replaceAll("_");
        }

        return UsernameCleaner.format(userFormat, Map.of("username", userName));
    }

    /**
     * @return the full XWiki reference to the user with the given Confluence username
     * @param userName the name of the user
     * @deprecated since 9.89.0, use {@link #convertUserName(String)}
     */
    @Deprecated (since = "9.89.0")
    public String toUserReference(String userName)
    {
        return convertUserName(userName);
    }

    @Override
    public String convertUserName(String userName)
    {
        return serialize(getUserOrGroupReference(convertUserNameToReferenceName(userName)));
    }

    /**
     * @param groupName the Confluence username
     * @return the corresponding XWiki user reference
     * @since 9.45.0
     */
    public String toGroupReference(String groupName)
    {
        return serialize(getUserOrGroupReference(toGroupReferenceName(groupName)));
    }

    @Override
    public String getGuestUser()
    {
        return serialize(GUEST);
    }

    EntityReference getUserOrGroupReference(String userOrGroupReferenceName)
    {
        // Transform user or group name according to configuration
        if (StringUtils.isEmpty(userOrGroupReferenceName)) {
            return null;
        }

        // Add the "XWiki" space and the wiki if configured. Ideally this should probably be done on XWiki Instance
        // Output filter side
        return context.getProperties().getUsersWiki() == null
            ? new LocalDocumentReference(XWIKI, userOrGroupReferenceName)
            : new DocumentReference(context.getProperties().getUsersWiki(), XWIKI, userOrGroupReferenceName);
    }

    /**
     * @return the converted resource reference
     * @param reference the reference to the user
     * @deprecated since 9.89.0, use {@link #convertUserKeyToResourceReference(String)}
     */
    @Deprecated (since = "9.89.0")
    public ResourceReference resolveUserReference(UserResourceReference reference)
    {
        ResourceReference ref = convertUserKeyToResourceReference(reference.getReference());
        if (ref != null) {
            ref.setParameters(reference.getParameters());
        }
        return ref;
    }

    /**
     * @return the resource reference corresponding to the provided user
     * @param userKey the user key in Confluence
     * @since 9.89.0
     */
    public ResourceReference convertUserKeyToResourceReference(String userKey)
    {
        String userName = toUserReferenceName(context.getConfluencePackage().resolveUserName(userKey, userKey));
        if (StringUtils.isEmpty(userName)) {
            logger.error("Failed to resolve user [{}]", userKey);
            return null;
        }

        return convertUserNameToResourceReference(userName);
    }

    private ResourceReference convertUserNameToResourceReference(String userName)
    {
        if (context.getProperties().isUserReferences()) {
            return new UserResourceReference(userName);
        }

        // Convert to link to user profile
        // FIXME: would not really been needed if the XWiki Instance output filter was taking care of that when
        // receiving a user reference

        String convertedUserName = convertUserName(userName);
        if (StringUtils.isEmpty(convertedUserName)) {
            return null;
        }

        return new DocumentResourceReference(convertedUserName);
    }

    private String convertUserKeyToReferenceName(String userKey)
    {
        return convertUserNameToReferenceName(
            context.getConfluencePackage().resolveUserName(userKey, userKey));
    }

    @Override
    public String convertUserReference(String userKey)
    {
        return serialize(getUserOrGroupReference(convertUserKeyToReferenceName(userKey)));
    }

    private EntityReference newEntityReference(String name, EntityType type, EntityReference parent)
    {
        if (StringUtils.isEmpty(name)) {
            this.logger.warn("Tried to create an entity reference with an empty name");
            return null;
        }
        return new EntityReference(name, type, parent);
    }

    private EntityReference fromSpaceKey(String ciSpaceKey)
    {
        String space = ensureNonEmptySpaceKey(ciSpaceKey);
        ConfluenceXMLPackage confluencePackage = context.getConfluencePackage();
        Long spaceId = confluencePackage.getSpaceId(space);
        if (spaceId != null) {
            String spaceWithCorrectCase = space;
            try {
                spaceWithCorrectCase = confluencePackage.getSpaceKey(spaceId);
            } catch (ConfigurationException e) {
                logger.warn("Could not find the correct case of space [{}]", space);
            }
            String convertedSpace = toEntityName(spaceWithCorrectCase);
            EntityReference root = getRoot();
            return newEntityReference(convertedSpace, EntityType.SPACE, root);
        }

        if (this.context.getProperties().isUseConfluenceResolvers()) {
            EntityReference spaceRef = getSpaceUsingResolver(ciSpaceKey);
            if (spaceRef != null) {
                return spaceRef;
            }
        }

        logger.warn(CONFLUENCE_REF_MARKER, FAILED_TO_RESOLVE_SPACE, ciSpaceKey);
        return null;
    }

    /**
     * Convert an external group ID to a XWiki reference.
     * @param groupId confluence external group ID
     * @return serialized XWiki group reference
     */
    public String convertGroupId(String groupId)
    {
        try {
            // search group ID from input properties
            Mapping groupIdMapping = context.getProperties().getGroupIdMapping();
            if (groupIdMapping.containsKey(groupId)) {
                String confluenceGroupName = groupIdMapping.get(groupId);
                return toGroupReference(confluenceGroupName);
            }
            for (long i : context.getConfluencePackage().getGroups()) {
                ConfluenceProperties properties = context.getConfluencePackage().getGroupProperties(i);
                String externalId = properties.getString(KEY_GROUP_EXTERNAL_ID);
                if (groupId.equals(externalId)) {
                    String confluenceGroupName = properties.getString(KEY_GROUP_NAME);
                    return toGroupReference(confluenceGroupName);
                }
            }
        } catch (ConfigurationException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
        logger.error("Can't find matching group for ID [{}]", groupId);
        return null;
    }

    /**
     * Converts a page ID into a XWiki reference.
     *
     * @param pageId confluence if of the page
     * @param asSpace if you want the reference as a space
     * @return a valid XWiki reference or null
     */
    public EntityReference convertDocumentReference(long pageId, boolean asSpace)
    {
        EntityReference ref = null;
        try {
            ConfluenceXMLPackage confluencePackage = context.getConfluencePackage();

            ConfluenceProperties pageProperties = confluencePackage.getPageProperties(pageId, false);
            if (pageProperties != null) {
                Long spaceId = pageProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_SPACE, null);
                if (spaceId != null) {
                    ref = convertDocumentReference(pageProperties, confluencePackage.getSpaceKey(spaceId), asSpace);
                }
            }

            if (ref == null) {
                ref = getDocRefFromLinkMapping(pageId, asSpace);
            }
        } catch (ConfigurationException e) {
            logger.error("Failed to resolve page id [{}]", pageId, e);
        }

        if (ref == null) {
            warnMissingPage(pageId);
        }
        return ref;
    }

    EntityReference convertDocumentReference(ConfluenceProperties pageProperties, String ciSpaceKey, boolean asSpace)
    {
        return toNestedDocumentReference(ciSpaceKey, pageProperties, asSpace, true);
    }

    private EntityReference toDocumentReference(String ciSpaceKey, String ciPageTitle)
    {
        Long currentPageId = context.getCurrentPage();
        if (AT_SELF.equals(ciPageTitle)) {
            return getSelfDocumentReference(currentPageId);
        }

        if (AT_HOME.equals(ciPageTitle) || AT_NONE.equals(ciPageTitle) || StringUtils.isEmpty(ciPageTitle)) {
            return getDocumentReference(fromSpaceKey(ciSpaceKey), false, false);
        }

        if (AT_PARENT.equals(ciPageTitle)) {
            try {
                if (currentPageId != null) {
                    ConfluenceProperties pageProperties = context.getConfluencePackage()
                        .getPageProperties(currentPageId, false);
                    Long parentId = pageProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_PARENT, null);
                    if (parentId != null) {
                        return convertDocumentReference(parentId, false);
                    }
                }
            } catch (ConfigurationException e) {
                logger.error("Could not get the properties of a page with resolving @parent.", e);
            }

            // Should not happen
            return new EntityReference(AT_PARENT, EntityType.DOCUMENT);
        }

        String nonEmptySpaceKey = ensureNonEmptySpaceKey(ciSpaceKey);
        // Try first producing a reference without assuming a page without parent is a root page and without using
        // link mapping. If this fails, produce a reference using the link mapping and doing this assumption.
        EntityReference res = toDocumentReferenceNoSpecialSpaceKey(nonEmptySpaceKey, ciPageTitle, false);
        return res == null ? toDocumentReferenceNoSpecialSpaceKey(nonEmptySpaceKey, ciPageTitle, true) : res;
    }

    private EntityReference getSelfDocumentReference(Long currentPageId)
    {
        if (currentPageId != null) {
            try {
                ConfluenceProperties pageProperties = context.getConfluencePackage()
                    .getPageProperties(currentPageId, false);
                boolean isBlogPost = pageProperties.getBoolean(ConfluenceXMLPackage.KEY_PAGE_BLOGPOST, false);
                if (isBlogPost) {
                    return convertDocumentReference(currentPageId, false);
                }
            } catch (ConfigurationException e) {
                logger.warn("Could not get page properties of id [{}]", currentPageId, e);
            }
        }
        return new EntityReference(WEB_HOME, EntityType.DOCUMENT);
    }

    private EntityReference toDocumentReferenceNoSpecialSpaceKey(String ciSpaceKey, String ciPageTitle, boolean guess)
    {
        if (ciSpaceKey != null) {
            ConfluenceXMLPackage confluencePackage = context.getConfluencePackage();

            Long pageId = confluencePackage.getPageId(ciSpaceKey, ciPageTitle);
            if (pageId != null) {
                try {
                    ConfluenceProperties pageProperties = confluencePackage.getPageProperties(pageId, false);
                    if (pageProperties != null) {
                        return toNestedDocumentReference(ciSpaceKey, pageProperties, false, guess);
                    }
                } catch (ConfigurationException e) {
                    logger.error("Failed to get properties for page id [{}]", pageId, e);
                }
            }

            if (guess) {
                return getDocRefFromLinkMapping(ciSpaceKey, ciPageTitle, false, true);
            }
        }

        return null;
    }

    private String ensureNonEmptySpaceKey(String ciSpaceKey)
    {
        return (StringUtils.isEmpty(ciSpaceKey) || ciSpaceKey.equals("currentSpace()") || ciSpaceKey.equals(AT_SELF))
            ? context.getCurrentSpace()
            : ciSpaceKey;
    }

    private void warnMissingPage(String ciSpaceKey, String ciPageTitle)
    {
        this.logger.warn(CONFLUENCE_REF_MARKER, "Could not find page [{}] in space [{}]. " + CONFLUENCE_REF_EXPLANATION,
            ciPageTitle, ciSpaceKey);
    }

    private void warnMissingPage(long pageId)
    {
        this.logger.warn(CONFLUENCE_REF_MARKER, "Could not find page id [{}]. " + CONFLUENCE_REF_EXPLANATION,
            pageId);
    }

    private EntityReference getDocRefFromLinkMapping(String ciSpaceKey, String ciPageTitle, boolean asSpace,
        boolean warn)
    {
        EntityReference ref = maybeAsSpace(context.getProperties().getLinkMapping()
            .getOrDefault(ciSpaceKey, Collections.emptyMap()).get(ciPageTitle), asSpace);

        if (ref == null && pageTitleResolver != null && this.context.getProperties().isUseConfluenceResolvers()) {
            ref = maybeAsSpace(getDocumentByTitleUsingResolver(ciSpaceKey, ciPageTitle), asSpace);
        }

        if (warn && ref == null) {
            warnMissingPage(ciSpaceKey, ciPageTitle);
        }

        return ref;
    }

    private EntityReference getDocumentByTitleUsingResolver(String spaceKey, String pageTitle)
    {
        if (context.getConfluencePackage().getSpaceId(spaceKey) != null) {
            // We are not going to resolve something that's supposed to be in the package being imported
            return null;
        }

        return context.getCachedReference(spaceKey, pageTitle, () -> {
            try {
                return pageTitleResolver.getDocumentByTitle(spaceKey, pageTitle);
            } catch (ConfluenceResolverException e) {
                logger.error("Failed to resolve page with space=[{}], pageTitle=[{}]", spaceKey, pageTitle, e);
            }
            return null;
        });
    }

    private EntityReference getSpaceUsingResolver(String spaceKey)
    {
        return context.getCachedReference(spaceKey, "", () -> {
            try {
                return spaceKeyResolver.getSpaceByKey(spaceKey);
            } catch (ConfluenceResolverException e) {
                logger.error(CONFLUENCE_REF_MARKER, FAILED_TO_RESOLVE_SPACE, spaceKey, e);
            }
            return null;
        });
    }

    private EntityReference maybeAsSpace(EntityReference entityReference, boolean asSpace)
    {
        if (entityReference != null && entityReference.getType() == EntityType.DOCUMENT && asSpace) {
            return entityReference.getParent();
        }
        return entityReference;
    }

    private EntityReference getDocRefFromLinkMapping(long pageId, boolean asSpace)
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
                        return maybeAsSpace(docRef, asSpace);
                    }
                }
            }
        }

        if (pageIdResolver != null && this.context.getProperties().isUseConfluenceResolvers()) {
            return maybeAsSpace(getDocumentByIdUsingResolver(pageId), asSpace);
        }

        return null;
    }

    private EntityReference getDocumentByIdUsingResolver(long pageId)
    {
        return context.getCachedReference(pageId, () -> {
            try {
                return pageIdResolver.getDocumentById(pageId);
            } catch (ConfluenceResolverException e) {
                logger.error("Failed to resolve page with id=[{}]", pageId, e);
            }
            return null;
        });
    }

    private EntityReference toNestedDocumentReference(String ciSpaceKey, ConfluenceProperties pageProperties,
        boolean asSpace, boolean guess)
    {
        String pageTitle = pageProperties.getString("title");

        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_HOMEPAGE)) {
            return getDocumentReference(fromSpaceKey(ciSpaceKey), asSpace, false);
        }

        EntityReference parent = null;

        Long parentId = pageProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_PARENT, null);
        if (parentId == null) {
            // is the page an orphan? Then it lives at the root of the space
            ConfluenceXMLPackage confluencePackage = context.getConfluencePackage();
            List<Long> orphans = confluencePackage.getOrphans(confluencePackage.getSpaceId(ciSpaceKey));
            Long id = pageProperties.getLong("id", null);
            if (id != null && orphans.contains(id)) {
                parent = fromSpaceKey(ciSpaceKey);
            }
        } else {
            parent = convertDocumentReference(parentId, true);
        }

        if (parent == null && guess) {
            // Missing parent, let's see if the provided link mapping has this document.
            // If parentId is null, this most likely means that this is a root page which parent is the space though.
            // But even in this case, we allow the link mapping data to override this conclusion.
            EntityReference docRef = getDocRefFromLinkMapping(ciSpaceKey, pageTitle, asSpace, parentId != null);
            if (docRef != null) {
                return docRef;
            }

            // if the page has no parent, if the parent page is missing, we consider the space as the parent
            parent = fromSpaceKey(ciSpaceKey);
        }

        if (parent == null) {
            return null;
        }

        String convertedName = toEntityName(pageTitle);
        boolean isBlogPost = pageProperties.getBoolean(ConfluenceXMLPackage.KEY_PAGE_BLOGPOST, false);
        if (isBlogPost) {
            parent = new EntityReference(context.getProperties().getBlogSpaceName(), EntityType.SPACE, parent);
        }
        return getDocumentReference(newEntityReference(convertedName, EntityType.SPACE, parent), asSpace, isBlogPost);
    }

    private static EntityReference getDocumentReference(EntityReference space, boolean asSpace, boolean isBlogPost)
    {
        if (asSpace || space == null) {
            return space;
        }

        if (isBlogPost) {
            return new EntityReference(space.getName(), EntityType.DOCUMENT, space.getParent());
        }

        return new EntityReference(WEB_HOME, EntityType.DOCUMENT, space);
    }

    @Override
    public String convertDocumentReference(String spaceKey, String pageTitle)
    {
        return serialize(getResourceReference(spaceKey, pageTitle, null, null));
    }

    @Override
    public ResourceReference getResourceReference(String ciSpaceKey, String ciPageTitle, String filename, String anchor)
    {
        String convertedAnchor = StringUtils.isBlank(anchor)
            ? null
            : convertAnchor("", ciPageTitle, anchor);

        if (StringUtils.isEmpty(ciPageTitle) && StringUtils.isEmpty(ciSpaceKey)) {
            if (StringUtils.isNotEmpty(filename)) {
                AttachmentResourceReference attachmentResourceReference =
                    new AttachmentResourceReference(escapeAtAndHash(filename));
                if (StringUtils.isNotEmpty(convertedAnchor)) {
                    attachmentResourceReference.setAnchor(convertedAnchor);
                }
                return attachmentResourceReference;
            }

            DocumentResourceReference docRef = new DocumentResourceReference("");
            if (StringUtils.isNotEmpty(convertedAnchor)) {
                docRef.setAnchor(convertedAnchor);
            }

            return docRef;
        }
        EntityReference documentReference = toDocumentReference(ciSpaceKey, ciPageTitle);
        if (documentReference == null) {
            boolean isHomePage = AT_HOME.equals(ciPageTitle);
            return new ConfluenceResourceReference(
                ensureNonEmptySpaceKey(ciSpaceKey), isHomePage ? null : ciPageTitle, filename, convertedAnchor,
                isHomePage);
        }
        return toResolvedResourceReference(documentReference, filename, convertedAnchor);
    }

    private static String escapeAtAndHash(String s)
    {
        return escapeHash(s).replace("@", "\\@");
    }

    private static String escapeHash(String s)
    {
        return s.replace("\\", "\\\\").replace("#", "\\#");
    }

    @Override
    public ResourceReference getResourceReference(long pageId, String filename, String anchor)
    {
        EntityReference docRef = convertDocumentReference(pageId, false);
        if (docRef == null) {
            return new ConfluenceResourceReference(pageId, filename, anchor);
        }
        String convertedAnchor = StringUtils.isBlank(anchor)
            ? null
            : convertAnchor("", getPageTitleForAnchor(pageId), anchor);
        return toResolvedResourceReference(docRef, filename, convertedAnchor);
    }

    private ResourceReference toResolvedResourceReference(EntityReference ref, String filename, String anchor)
    {
        if (StringUtils.isEmpty(filename)) {
            DocumentResourceReference documentResourceReference = new DocumentResourceReference(serialize(ref));
            if (!StringUtils.isBlank(anchor)) {
                documentResourceReference.setAnchor(anchor);
            }
            return documentResourceReference;
        }

        AttachmentResourceReference attachmentResourceReference = new AttachmentResourceReference(
            serialize(new EntityReference(filename, EntityType.ATTACHMENT, ref)));
        if (!StringUtils.isBlank(anchor)) {
            attachmentResourceReference.setAnchor(anchor);
        }
        return attachmentResourceReference;
    }

    @Override
    public String convertDocumentReference(long pageId)
    {
        return serialize(getResourceReference(pageId, null, null));
    }

    @Override
    public String convertAttachmentReference(String ciSpaceKey, String ciPageTitle, String filename)
    {
        if (StringUtils.isEmpty(filename)) {
            return "";
        }
        return serialize(getResourceReference(ciSpaceKey, ciPageTitle, filename, null));
    }

    static String spacesToDash(String name)
    {
        return clean(name, false).replaceAll("\\s+", "-");
    }

    private String getPageTitleForAnchor(long pageId)
    {
        String title = null;
        try {
            ConfluenceProperties pageProperties = context.getConfluencePackage().getPageProperties(pageId, false);
            if (pageProperties != null) {
                title = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE);
            }
        } catch (ConfigurationException e) {
            logger.warn("Failed to get the title of page [{}] to produce the anchor. Links may be broken.", pageId, e);
        }

        if (StringUtils.isEmpty(title)) {
            logger.warn("Could not get the title of page [{}] to produce the anchor. Links may be broken.", pageId);
        }

        return title;
    }

    String getCurrentPageTitleForAnchor()
    {
        return getPageTitleForAnchor(context.getCurrentPage());
    }

    private static String clean(String name, boolean removeWhitespace)
    {
        return (name == null ? "" : name).replaceAll("\\p{Z}+", removeWhitespace ? "" : " ").strip();
    }

    static String getConfluenceServerAnchor(String pageTitle, String name)
    {
        String convertedAnchor = clean(name, true);
        if (!StringUtils.isEmpty(pageTitle)) {
            convertedAnchor = clean(pageTitle, true) + '-' + convertedAnchor;
        }
        return convertedAnchor;
    }

    @Override
    public String convertAnchor(String spaceKey, String pageTitle, String anchor)
    {
        if (context.isConfluenceCloud()) {
            return spacesToDash(anchor);
        }

        String title = getAnchorTitle(spaceKey, pageTitle, anchor);
        return getConfluenceServerAnchor(title, anchor);
    }

    private String getAnchorTitle(String spaceKey, String pageTitle, String anchor)
    {
        if (!StringUtils.isEmpty(pageTitle)) {
            return pageTitle;
        }

        if (StringUtils.isEmpty(spaceKey)) {
            return getCurrentPageTitleForAnchor();
        }
        ConfluenceXMLPackage confluencePackage = context.getConfluencePackage();
        Long spaceId = confluencePackage.getSpaceId(context.getCurrentSpace());

        String title = null;
        if (spaceId != null) {
            Long homePage = confluencePackage.getHomePage(spaceId);
            if (homePage != null) {
                title = getPageTitleForAnchor(homePage);
            }
        }

        if (title == null) {
            logger.warn("Could not get the title of the home page of space [{}], anchor [{}] might be broken",
                spaceKey, anchor);
            title = "";
        }

        return title;
    }

    @Override
    public String convertSpaceReference(String spaceKey)
    {
        return convertSpaceReference(spaceKey, false);
    }

    @Override
    public String convertSpaceReference(String spaceKey, boolean asDocument)
    {
        EntityReference ref = fromSpaceKey(spaceKey);
        if (ref == null) {
            String spaceKey1 = ensureNonEmptySpaceKey(spaceKey);
            ConfluenceResourceReference r = new ConfluenceResourceReference(
                ensureNonEmptySpaceKey(spaceKey1), null, null, null, asDocument);
            return serialize(r);
        }
        if (asDocument) {
            ref = newEntityReference(WEB_HOME, EntityType.DOCUMENT, ref);
        }
        return serialize(ref);
    }

    private String serialize(EntityReference reference)
    {
        return this.compactWikiSerializer.serialize(reference, getRoot());
    }

    private String serialize(ResourceReference reference)
    {
        if (reference instanceof ConfluenceResourceReference) {
            return reference.getType().getScheme() + ':' + reference.getReference();
        }
        return reference.getReference();
    }

    /**
     * @deprecated since 9.76.0
     * Use {@link ConfluenceURLConverter#convertURL(String)}
     */
    @Override
    @Deprecated(since = "9.76.0")
    public ResourceReference convertURL(String url)
    {
        ConfluenceURLConverter urlConverter = null;
        try {
            urlConverter = componentManagerProvider.get().getInstance(ConfluenceURLConverter.class);
        } catch (ComponentLookupException e) {
            logger.error("Failed to get the Confluence URL converter component, the url will not be converted", e);
        }

        if (urlConverter != null) {
            ResourceReference resourceReference = urlConverter.convertURL(url);
            if (resourceReference != null) {
                return resourceReference;
            }
        }
        return new ResourceReference(url, ResourceType.URL);
    }
}
