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

import com.xpn.xwiki.XWikiContext;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceURLConverter;
import org.xwiki.contrib.confluence.filter.Mapping;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.contrib.confluence.filter.input.ConfluenceProperties;
import org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceReferenceConverter;
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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_GROUP_EXTERNAL_ID;
import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_GROUP_NAME;

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

    private static final String AT_SELF = "@self";

    private static final String AT_HOME = "@home";

    private static final String AT_PARENT = "@parent";

    private static final String CONFLUENCE_REF_EXPLANATION = "A Confluence reference will be used if possible. "
        + "Consider converting this reference later with a post import fix.";

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
            logger.warn("The naming strategy transformed [{}] to an empty name", r);
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
     * @param userName the Confluence username
     * @return the corresponding XWiki username, without forbidden characters
     */
    public String toUserReferenceName(String userName)
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

        // Translate the usual default admin user in Confluence to it's XWiki counterpart
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

    String getGuestUser()
    {
        return serialize(GUEST);
    }

    private String getUserOrGroupReference(String userReferenceName)
    {
        // Transform user name according to configuration
        if (StringUtils.isEmpty(userReferenceName)) {
            return null;
        }

        // Add the "XWiki" space and the wiki if configured. Ideally this should probably be done on XWiki Instance
        // Output filter side
        EntityReference reference = context.getProperties().getUsersWiki() == null
            ? new LocalDocumentReference(XWIKI, userReferenceName)
            : new DocumentReference(context.getProperties().getUsersWiki(), XWIKI, userReferenceName);

        return serialize(reference);
    }

    /**
     * @param reference the reference of a user that can be either a username or a user key.
     * @return a XWiki user reference.
     * @since 9.26
     */
    public ResourceReference resolveUserReference(UserResourceReference reference)
    {
        String userReference = reference.getReference();

        if (context.getProperties().isUserReferences()) {
            // Keep the UserResourceReference

            // Clean the user id
            String userName = toUserReferenceName(resolveUserName(userReference));
            if (userName == null || userName.isEmpty()) {
                return null;
            }

            reference.setReference(userName);

            return reference;
        }

        // Convert to link to user profile
        // FIXME: would not really been needed if the XWiki Instance output filter was taking care of that when
        // receiving a user reference

        String userName = getReferenceFromUserKey(userReference);
        if (StringUtils.isEmpty(userName)) {
            return null;
        }

        DocumentResourceReference documentReference = new DocumentResourceReference(userName);

        documentReference.setParameters(reference.getParameters());

        return documentReference;
    }

    String resolveUserName(String userKey)
    {
        if (StringUtils.isEmpty(userKey)) {
            return null;
        }
        String userName = context.getConfluencePackage().resolveUserName(userKey, userKey);
        if (StringUtils.isEmpty(userName)) {
            logger.error("Could not resolve user key [{}]", userKey);
        }
        return userName;
    }

    String getReferenceFromUserKey(String userKey)
    {
        return toUserReference(resolveUserName(userKey));
    }

    @Override
    public String convertUserReference(String userId)
    {
        return resolveUserReference(new UserResourceReference(userId)).getReference();
    }

    private EntityReference newEntityReference(String name, EntityType type, EntityReference parent)
    {
        if (StringUtils.isEmpty(name)) {
            this.logger.warn("Tried to create an entity reference with an empty name");
            return null;
        }
        return new EntityReference(name, type, parent);
    }

    private EntityReference fromSpaceKey(String spaceKey)
    {
        String space = ensureNonEmptySpaceKey(spaceKey);
        if (context.getConfluencePackage().getSpacesByKey().containsKey(space)) {
            String convertedSpace = toEntityName(space);
            EntityReference root = getRoot();
            return newEntityReference(convertedSpace, EntityType.SPACE, root);
        }

        if (this.context.getProperties().isUseConfluenceResolvers()) {
            return getSpaceUsingResolver(spaceKey);
        }

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
        try {
            ConfluenceXMLPackage confluencePackage = context.getConfluencePackage();

            ConfluenceProperties pageProperties = confluencePackage.getPageProperties(pageId, false);

            if (pageProperties == null) {
                return getDocRefFromLinkMapping(pageId, asSpace);
            }

            Long spaceId = pageProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_SPACE, null);
            if (spaceId == null) {
                return null;
            }
            return convertDocumentReference(pageProperties, confluencePackage.getSpaceKey(spaceId), asSpace);
        } catch (ConfigurationException exception) {
            logger.error(exception.getMessage(), exception);
            return null;
        }
    }

    EntityReference convertDocumentReference(ConfluenceProperties pageProperties, String spaceKey, boolean asSpace)
    {
        String pageTitle = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE);

        if (pageTitle == null) {
            return null;
        }

        return toNestedDocumentReference(spaceKey, pageTitle, pageProperties, asSpace, true);
    }

    private EntityReference toDocumentReference(String spaceKey, String pageTitle)
    {
        if (AT_SELF.equals(pageTitle)) {
            return new EntityReference(WEB_HOME, EntityType.DOCUMENT);
        }

        if (AT_HOME.equals(pageTitle) || StringUtils.isEmpty(pageTitle)) {
            return getDocumentReference(fromSpaceKey(spaceKey), false, false);
        }

        if (AT_PARENT.equals(pageTitle)) {
            Long currentPageId = context.getCurrentPage();
            ConfluenceXMLPackage confluencePackage = context.getConfluencePackage();

            try {
                if (currentPageId != null) {
                    ConfluenceProperties pageProperties = confluencePackage.getPageProperties(currentPageId, false);
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

        String nonEmptySpaceKey = ensureNonEmptySpaceKey(spaceKey);
        // Try first producing a reference without assuming a page without parent is a root page and without using
        // link mapping. If this fails, produce a reference using the link mapping and doing this assumption.
        EntityReference res = toDocumentReferenceNoSpecialSpaceKey(nonEmptySpaceKey, pageTitle, false);
        return res == null ? toDocumentReferenceNoSpecialSpaceKey(nonEmptySpaceKey, pageTitle, true) : res;
    }

    private EntityReference toDocumentReferenceNoSpecialSpaceKey(String spaceKey, String pageTitle, boolean guess)
    {
        if (spaceKey != null) {
            ConfluenceXMLPackage confluencePackage = context.getConfluencePackage();

            Long pageId = confluencePackage.getPageId(spaceKey, pageTitle);
            if (pageId != null) {
                try {
                    ConfluenceProperties pageProperties = confluencePackage.getPageProperties(pageId, false);
                    if (pageProperties != null) {
                        return toNestedDocumentReference(spaceKey, pageTitle, pageProperties, false, guess);
                    }
                } catch (ConfigurationException e) {
                    logger.error("Failed to get properties for page id [{}]", pageId, e);
                }
            }

            if (guess) {
                return getDocRefFromLinkMapping(spaceKey, pageTitle, false, true);
            }
        }

        return null;
    }

    private String ensureNonEmptySpaceKey(String spaceKey)
    {
        return (StringUtils.isEmpty(spaceKey) || spaceKey.equals("currentSpace()") || spaceKey.equals(AT_SELF))
            ? context.getCurrentSpace()
            : spaceKey;
    }

    private void warnMissingPage(String spaceKey, String pageTitle)
    {
        this.logger.warn(CONFLUENCE_REF_MARKER, "Could not find page [{}] in space [{}]. " + CONFLUENCE_REF_EXPLANATION,
            pageTitle, spaceKey);
    }

    private EntityReference getDocRefFromLinkMapping(String spaceKey, String pageTitle, boolean asSpace, boolean warn)
    {
        EntityReference ref = maybeAsSpace(context.getProperties().getLinkMapping()
            .getOrDefault(spaceKey, Collections.emptyMap()).get(pageTitle), asSpace);

        if (ref == null && pageTitleResolver != null && this.context.getProperties().isUseConfluenceResolvers()) {
            ref = maybeAsSpace(getDocumentByTitleUsingResolver(spaceKey, pageTitle), asSpace);
        }

        if (warn && ref == null) {
            warnMissingPage(spaceKey, pageTitle);
        }

        return ref;
    }

    private EntityReference getDocumentByTitleUsingResolver(String spaceKey, String pageTitle)
    {
        if (context.getConfluencePackage().getSpacesByKey().containsKey(spaceKey)) {
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
                logger.error("Failed to resolve space=[{}]", spaceKey, e);
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

    private EntityReference toNestedDocumentReference(String spaceKey, String pageTitle,
        ConfluenceProperties pageProperties, boolean asSpace, boolean guess)
    {
        if (StringUtils.isEmpty(pageTitle)) {
            return null;
        }

        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_HOMEPAGE)) {
            return getDocumentReference(fromSpaceKey(spaceKey), asSpace, false);
        }

        EntityReference parent = null;

        Long parentId = pageProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_PARENT, null);
        if (parentId != null) {
            parent = convertDocumentReference(parentId, true);
        }

        if (parent == null && guess) {
            // Missing parent, let's see if the provided link mapping has this document.
            // If parentId is null, this most likely means that this is a root page which parent is the space though.
            // But even in this case, we allow the link mapping data to override this conclusion.
            EntityReference docRef = getDocRefFromLinkMapping(spaceKey, pageTitle, asSpace, parentId != null);
            if (docRef != null) {
                return docRef;
            }

            // if the page has no parent, if the parent page is missing, we consider the space as the parent
            parent = fromSpaceKey(spaceKey);
        }

        if (parent == null) {
            return null;
        }

        String convertedName = toEntityName(pageTitle);
        boolean isBlogPost = pageProperties.getBoolean(ConfluenceXMLPackage.KEY_PAGE_BLOGPOST, false);
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
    public ResourceReference getResourceReference(String spaceKey, String pageTitle, String filename, String anchor)
    {
        String convertedAnchor = StringUtils.isBlank(anchor)
            ? null
            : convertAnchor("", pageTitle, anchor);

        if (StringUtils.isEmpty(pageTitle) && StringUtils.isEmpty(spaceKey)) {
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
        EntityReference documentReference = toDocumentReference(spaceKey, pageTitle);
        if (documentReference == null) {
            return new ConfluenceResourceReference(
                ensureNonEmptySpaceKey(spaceKey), pageTitle, filename, convertedAnchor, false);
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
    public String convertAttachmentReference(String spaceKey, String pageTitle, String filename)
    {
        if (StringUtils.isEmpty(filename)) {
            return "";
        }
        return serialize(getResourceReference(spaceKey, pageTitle, filename, null));
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

        String title = pageTitle;
        if (StringUtils.isEmpty(title)) {
            if (StringUtils.isEmpty(spaceKey)) {
                title = getCurrentPageTitleForAnchor();
            } else {
                ConfluenceXMLPackage confluencePackage = context.getConfluencePackage();
                Long spaceId = confluencePackage.getSpacesByKey().get(context.getCurrentSpace());
                if (spaceId == null) {
                    logger.warn("Could not get the home page of space [{}], anchor [{}] might be broken",
                        spaceKey, anchor);
                    title = "";
                } else {
                    title = getPageTitleForAnchor(confluencePackage.getHomePage(spaceId));
                }
            }
        }

        return getConfluenceServerAnchor(title, anchor);
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
                ensureNonEmptySpaceKey(spaceKey1), null, null, null, true);
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
     * Use ConfluenceURLConverter#convertURL(String)
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
