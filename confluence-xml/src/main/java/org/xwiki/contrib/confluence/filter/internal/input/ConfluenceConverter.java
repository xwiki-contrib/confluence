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
import org.apache.commons.lang3.ObjectUtils;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
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
    private static final Pattern PATTERN_URL_DISPLAY = Pattern.compile("^/display/(.+)/([^?#]+)(\\?.*)?$");

    private static final Pattern PATTERN_URL_VIEWPAGE =
        Pattern.compile("^/pages/viewpage.action\\?pageId=(\\d+)(&.*)?$");

    private static final Pattern PATTERN_TINY_LINK = Pattern.compile("^/x/([^?#]+)(\\?.*)?$");

    private static final Pattern PATTERN_URL_SPACES = Pattern.compile("^/spaces/(.+)/pages/\\d+/([^?#]+)(\\?.*)?$");

    private static final Pattern PATTERN_URL_ATTACHMENT =
        Pattern.compile("^/download/(?:attachments|thumbnails)/(\\d+)/([^?#]+)(\\?.*)?$");

    private static final Pattern PATTERN_URL_EMOTICON =
        Pattern.compile("^/images/icons/emoticons/([^?#]+)(\\....)(\\?.*)?$");

    private static final Pattern FORBIDDEN_USER_CHARACTERS = Pattern.compile("[. /]");

    private static final String XWIKI = "XWiki";

    private static final String WEB_HOME = "WebHome";

    private static final String AT_SELF = "@self";

    private static final String AT_HOME = "@home";

    private static final String AT_PARENT = "@parent";

    private static final String BROKEN_LINK_EXPLANATION = "Links to this page may be broken. "
        + "This may happen when importing a space that links to another space which is not present "
        + "in this Confluence export, or the page is missing";

    private static final EntityReference GUEST = new EntityReference(
        "XWikiGuest", EntityType.DOCUMENT, new SpaceReference("xwiki", XWIKI));

    @Inject
    private Provider<EntityNameValidationManager> entityNameValidationManagerProvider;

    @Inject
    private Logger logger;

    @Inject
    @Named("relative")
    private EntityReferenceResolver<String> relativeResolver;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiSerializer;

    @Inject
    private Provider<XWikiContext> contextProvider;

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
            return newEntityReference(entityReference.getName(), EntityType.ATTACHMENT, convertedParent);
        }

        if (parent == null && EntityType.DOCUMENT.equals(entityReference.getType())) {
            return toDocumentReference(context.getCurrentSpace(), entityReference.getName());
        }

        if (parent != null && parent.getParent() == null) {
            // The reference is of shape "Space.Doc title", it needs to go through the normal Confluence
            // document reference conversion.
            return toDocumentReference(parent.getName(), entityReference.getName());
        }

        return maybeAppendRoot(convertRef(entityReference));
    }

    private EntityReference maybeAppendRoot(EntityReference ref)
    {
        if (ref == null) {
            return null;
        }

        EntityReference root = getRoot();
        if (root == null) {
            return ref;
        }

        EntityType rootType = ref.getRoot().getType();
        if (EntityType.SPACE.equals(rootType) || EntityType.WIKI.equals(rootType)) {
            // We don't want to add the root if the reference is a local document, otherwise we end up with a
            // broken reference (Root.Document instead of Root.SpaceContainingDocument.Document)
            return ref.appendParent(root);
        }

        return ref;
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

    private EntityReference convertRef(EntityReference entityReference)
    {
        EntityReference newRef = null;

        for (EntityReference entityElement : entityReference.getReversedReferenceChain()) {
            if (entityElement.getType() == EntityType.DOCUMENT || entityElement.getType() == EntityType.SPACE) {
                String name = entityElement.getName();
                String convertedName = applyNamingStrategy(name);
                newRef = newEntityReference(convertedName, entityElement.getType(), newRef);
            } else if (newRef == null || entityElement.getParent() == newRef) {
                newRef = entityElement;
            } else {
                newRef = new EntityReference(entityElement, newRef);
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
            if (reference == null) {
                return "";
            }

            // Serialize the fixed reference
            return serialize(reference);
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

    String getGuestUser()
    {
        return serialize(GUEST);
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

    private EntityReference newEntityReference(String name, EntityType type, EntityReference parent)
    {
        if (StringUtils.isEmpty(name)) {
            this.logger.warn("Tried to create an entity reference with an empty name");
            return null;
        }
        return new EntityReference(name, type, parent);
    }

    private EntityReference toNonNestedDocumentReference(String spaceKey, String pageTitle)
    {
        String convertedName = toEntityName(pageTitle);
        EntityReference space = spaceKey == null ? null : fromSpaceKey(spaceKey);
        return getDocumentReference(newEntityReference(convertedName, EntityType.SPACE, space), false);
    }

    private EntityReference fromSpaceKey(String spaceKey)
    {
        String convertedSpace = toEntityName(ensureNonEmptySpaceKey(spaceKey));
        EntityReference root = getRoot();
        return newEntityReference(convertedSpace, EntityType.SPACE, root);
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

    EntityReference convertDocumentReference(ConfluenceProperties pageProperties, String spaceKey,
        boolean asSpace)
    {
        String pageTitle = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE);

        if (pageTitle == null) {
            return null;
        }

        return toNestedDocumentReference(spaceKey, pageTitle, pageProperties, asSpace, true);
    }

    EntityReference toDocumentReference(String spaceKey, String pageTitle)
    {
        if (AT_SELF.equals(pageTitle)) {
            return new EntityReference(WEB_HOME, EntityType.DOCUMENT);
        }

        if (AT_HOME.equals(pageTitle)) {
            return getDocumentReference(fromSpaceKey(spaceKey), false);
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

        return toDocumentReferenceNoSpecialSpaceKey(ensureNonEmptySpaceKey(spaceKey), pageTitle);
    }

    private EntityReference toDocumentReferenceNoSpecialSpaceKey(String spaceKey, String pageTitle)
    {
        // Try first producing a reference without assuming a page without parent is a root page and without using
        // link mapping. If this fails, produce a reference using the link mapping and doing this assumption.
        EntityReference res = toDocumentReferenceNoSpecialSpaceKey(spaceKey, pageTitle, false);
        return res == null ? toDocumentReferenceNoSpecialSpaceKey(spaceKey, pageTitle, true) : res;
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
                EntityReference docRef = getDocRefFromLinkMapping(spaceKey, pageTitle, false, true);
                if (docRef != null) {
                    return docRef;
                }
            }
        }

        if (guess) {
            return toNonNestedDocumentReference(spaceKey, pageTitle);
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
        this.logger.warn("Could not find page [{}] in space [{}]. " + BROKEN_LINK_EXPLANATION, pageTitle, spaceKey);
    }

    private EntityReference getDocRefFromLinkMapping(String spaceKey, String pageTitle, boolean asSpace, boolean warn)
    {
        EntityReference ref = maybeAsSpace(context.getProperties().getLinkMapping()
            .getOrDefault(spaceKey, Collections.emptyMap()).get(pageTitle), asSpace);

        if (warn && ref == null) {
            warnMissingPage(spaceKey, pageTitle);
        }

        return ref;
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
        return null;
    }

    private EntityReference toNestedDocumentReference(String spaceKey, String pageTitle,
        ConfluenceProperties pageProperties, boolean asSpace, boolean guess)
    {
        if (StringUtils.isEmpty(pageTitle)) {
            return null;
        }

        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_HOMEPAGE)) {
            return getDocumentReference(fromSpaceKey(spaceKey), asSpace);
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
        return getDocumentReference(newEntityReference(convertedName, EntityType.SPACE, parent), asSpace);
    }

    private static EntityReference getDocumentReference(EntityReference space, boolean asSpace)
    {
        if (asSpace) {
            return space;
        }

        return new EntityReference(WEB_HOME, EntityType.DOCUMENT, space);
    }

    @Override
    public String convertDocumentReference(String spaceKey, String pageTitle)
    {
        return serialize(toDocumentReference(spaceKey, pageTitle));
    }

    @Override
    public String convertAttachmentReference(String spaceKey, String pageTitle, String filename)
    {
        if (StringUtils.isEmpty(filename)) {
            return "";
        }
        if (pageTitle == null && spaceKey == null) {
            return filename;
        }

        EntityReference docRef = toDocumentReference(spaceKey, pageTitle);
        if (docRef == null || docRef.getParent() == null && WEB_HOME.equals(docRef.getName())) {
            return filename;
        }
        return serialize(new EntityReference(filename, EntityType.ATTACHMENT, docRef));
    }

    static String spacesToDash(String name)
    {
        return clean(name, false).replaceAll("\\s+", "-");
    }


    String getPageTitleForAnchor(long pageId)
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
        EntityReference space = fromSpaceKey(spaceKey);
        if (space == null) {
            return null;
        }
        return serialize(space);
    }

    @Override
    public String convertSpaceReference(String spaceKey, boolean asDocument)
    {
        EntityReference ref = fromSpaceKey(spaceKey);
        if (asDocument) {
            ref = newEntityReference(WEB_HOME, EntityType.DOCUMENT, ref);
        }
        if (ref == null) {
            return "";
        }
        return serialize(ref);
    }

    private String serializeURLParameters(List<String[]> parameters)
    {
        StringBuilder builder = new StringBuilder();

        for (String[] parameter : parameters) {
            if (builder.length() > 0) {
                builder.append('&');
            }

            builder.append(parameter[0]);
            builder.append('=');
            builder.append(parameter[1]);
        }

        return builder.toString();
    }

    private EntityReference fromPageId(long pageId) throws NumberFormatException
    {
        EntityReference ref = convertDocumentReference(pageId, false);
        if (ref == null) {
            this.logger.warn("Could not find page id [{}]. " + BROKEN_LINK_EXPLANATION, pageId);
        }
        return ref;
    }

    private AttachmentResourceReference createAttachmentResourceReference(EntityReference reference,
        String pageTitle, String urlAnchor)
    {
        if (reference == null) {
            return null;
        }

        AttachmentResourceReference resourceReference =
            new AttachmentResourceReference(serialize(reference));

        // Anchor
        if (StringUtils.isNotBlank(urlAnchor)) {
            resourceReference.setAnchor(convertAnchor("", pageTitle, urlAnchor));
        }

        return resourceReference;
    }

    private DocumentResourceReference createDocumentResourceReference(EntityReference reference,
        String pageTitle, String urlAnchor)
    {
        if (reference == null) {
            return null;
        }

        DocumentResourceReference resourceReference =
            new DocumentResourceReference(serialize(reference));

        // Anchor
        if (StringUtils.isNotBlank(urlAnchor)) {
            resourceReference.setAnchor(convertAnchor("", pageTitle, urlAnchor));
        }

        return resourceReference;
    }

    private String decode(String encoded)
    {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    private ResourceReference tryPattern(Pattern pattern, String p, Function<Matcher, ResourceReference> f)
    {
        Matcher matcher = pattern.matcher(p);
        if (matcher.matches()) {
            return f.apply(matcher);
        }

        return null;
    }

    private DocumentResourceReference simpleDocRef(Matcher m, String urlAnchor)
    {
        String spaceKey = decode(m.group(1));
        String pageTitle = decode(m.group(2));
        EntityReference documentReference = toDocumentReference(spaceKey, pageTitle);

        return createDocumentResourceReference(documentReference, pageTitle, urlAnchor);
    }

    private long tinyPartToPageId(String part)
    {
        // FIXME copy-pasted from ConfluenceShortURLMapper
        // Reverse-engineered and inspired by https://confluence.atlassian.com/x/2EkGOQ
        // not sure the replaceChars part is necessary, but it shouldn't hurt
        String base64WithoutPadding = StringUtils.replaceChars(part, "-_/", "/+\n");

        byte[] decoded = new byte[8];
        Base64.getUrlDecoder().decode(base64WithoutPadding.getBytes(), decoded);
        return ByteBuffer.wrap(decoded).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    private DocumentResourceReference convertPageIdToResourceReference(List<String[]> urlParameters, String urlAnchor,
        long pageId)
    {
        EntityReference documentReference = getEntityReference(pageId);
        if (documentReference == null) {
            return null;
        }

        // Clean id parameter
        urlParameters.removeIf(parameter -> parameter[0].equals("pageId"));

        String pageTitle = getPageTitleForAnchor(pageId);
        return createDocumentResourceReference(documentReference, pageTitle, urlAnchor);
    }

    private EntityReference getEntityReference(long pageId)
    {
        try {
            return fromPageId(pageId);
        } catch (NumberFormatException e) {
            this.logger.error("Failed to get page for id [{}]", pageId, e);
        }
        return null;
    }

    private String enforceSlash(String pattern)
    {
        if (pattern.isEmpty() || pattern.charAt(0) != '/') {
            return "/" + pattern;
        }
        return pattern;
    }

    private List<String[]> parseURLParameters(String queryString)
    {
        if (queryString == null) {
            return Collections.emptyList();
        }

        String[] elements = StringUtils.split(queryString, '&');

        List<String[]> parameters = new ArrayList<>(elements.length);

        for (String element : elements) {
            parameters.add(StringUtils.split(element, '='));
        }

        return parameters;
    }

    private ResourceReference fixReference(String path, List<String[]> urlParameters, String urlAnchor)
    {
        return ObjectUtils.firstNonNull(
            // Try /display
            tryPattern(PATTERN_URL_DISPLAY, path, matcher -> simpleDocRef(matcher, urlAnchor)),

            // Try /spaces
            tryPattern(PATTERN_URL_SPACES, path, matcher -> simpleDocRef(matcher, urlAnchor)),

            // Try viewpage.action
            tryPattern(PATTERN_URL_VIEWPAGE, path, matcher -> {
                long pageId = Long.parseLong(matcher.group(1));
                return convertPageIdToResourceReference(urlParameters, urlAnchor, pageId);
            }),

            // Try short URL
            tryPattern(PATTERN_TINY_LINK, path, matcher -> {
                long pageId;
                try {
                    pageId = tinyPartToPageId(matcher.group(1));
                } catch (IllegalArgumentException e) {
                    logger.warn("Failed to decode the short link [{}]", path, e);
                    return null;
                }

                return convertPageIdToResourceReference(urlParameters, urlAnchor, pageId);
            }),

            // Try attachments
            tryPattern(PATTERN_URL_ATTACHMENT, path, matcher -> {
                long pageId = Long.parseLong(matcher.group(1));
                EntityReference documentReference = getEntityReference(pageId);
                if (documentReference == null) {
                    return null;
                }

                EntityReference attachmentReference =
                    newEntityReference(decode(matcher.group(2)), EntityType.ATTACHMENT, documentReference);

                String pageTitle = getPageTitleForAnchor(pageId);
                return createAttachmentResourceReference(attachmentReference, pageTitle, urlAnchor);
            }),

            // emoticons
            tryPattern(PATTERN_URL_EMOTICON, path, m -> new ResourceReference(decode(m.group(1)), ResourceType.ICON))
        );
    }

    private ResourceReference convertURL(String url, ResourceReference baseReference)
    {
        for (URL baseURL : context.getProperties().getBaseURLs()) {
            String baseURLString = baseURL.toExternalForm();

            if (url.startsWith(baseURLString)) {
                // Fix the URL if the format is known

                URL urlObj;
                try {
                    urlObj = new URL(url);
                } catch (MalformedURLException e) {
                    // Should never happen
                    this.logger.error("Wrong URL [{}]", url, e);
                    continue;
                }

                String path = enforceSlash(url.substring(baseURLString.length()));

                List<String[]> urlParameters = parseURLParameters(urlObj.getQuery());
                String urlAnchor = urlObj.getRef();

                ResourceReference ref = fixReference(path, urlParameters, urlAnchor);

                if (ref != null) {
                    return ref;
                }
            }
        }

        if (baseReference == null) {
            return new ResourceReference(url, ResourceType.URL);
        }

        return baseReference;
    }

    private String serialize(EntityReference reference)
    {
        return this.compactWikiSerializer.serialize(reference, getRoot());
    }

    @Override
    public ResourceReference convertURL(String url)
    {
        return convertURL(url, null);
    }

    ResourceReference convertURL(ResourceReference reference)
    {
        return convertURL(reference.getReference(), reference);
    }
}
