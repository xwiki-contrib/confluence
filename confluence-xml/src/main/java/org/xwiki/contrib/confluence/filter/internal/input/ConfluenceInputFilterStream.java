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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.confluence.filter.PageIdentifier;
import org.xwiki.contrib.confluence.filter.event.ConfluenceFilteredEvent;
import org.xwiki.contrib.confluence.filter.event.ConfluenceFilteringEvent;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.contrib.confluence.filter.input.ConfluenceProperties;
import org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage;
import org.xwiki.contrib.confluence.filter.input.ContentPermissionType;
import org.xwiki.contrib.confluence.filter.input.SpacePermissionType;
import org.xwiki.contrib.confluence.filter.internal.ConfluenceFilter;
import org.xwiki.contrib.confluence.parser.confluence.internal.ConfluenceParser;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceXHTMLInputProperties;
import org.xwiki.contrib.confluence.parser.xhtml.internal.ConfluenceXHTMLParser;
import org.xwiki.contrib.confluence.parser.xhtml.internal.InternalConfluenceXHTMLInputProperties;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiAttachmentFilter;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.event.model.WikiObjectFilter;
import org.xwiki.filter.event.user.GroupFilter;
import org.xwiki.filter.event.user.UserFilter;
import org.xwiki.filter.input.AbstractBeanInputFilterStream;
import org.xwiki.filter.input.BeanInputFilterStream;
import org.xwiki.filter.input.BeanInputFilterStreamFactory;
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.input.StringInputSource;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.Right;

/**
 * @version $Id$
 * @since 9.0
 */
@Component
@Named(ConfluenceInputFilterStreamFactory.ROLEHINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ConfluenceInputFilterStream
    extends AbstractBeanInputFilterStream<ConfluenceInputProperties, ConfluenceFilter>
{
    private static final Pattern FORBIDDEN_USER_CHARACTERS = Pattern.compile("[. /]");

    private static final String CONFLUENCEPAGE_CLASSNAME = "Confluence.Code.ConfluencePageClass";

    private static final String TAGS_CLASSNAME = "XWiki.TagClass";

    private static final String COMMENTS_CLASSNAME = "XWiki.XWikiComments";

    private static final String BLOG_CLASSNAME = "Blog.BlogClass";

    private static final String BLOG_POST_CLASSNAME = "Blog.BlogPostClass";

    private static final String XWIKIRIGHTS_CLASSNAME = "XWiki.XWikiRights";

    @Inject
    @Named(ConfluenceParser.SYNTAX_STRING)
    private StreamParser confluenceWIKIParser;

    @Inject
    @Named(ConfluenceXHTMLParser.SYNTAX_STRING)
    private InputFilterStreamFactory confluenceXHTMLParserFactory;

    @Inject
    private Provider<ConfluenceConverterListener> converterProvider;

    @Inject
    private JobProgressManager progress;

    @Inject
    @Named("xwiki/2.1")
    private PrintRendererFactory xwiki21Factory;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private ConfluenceInputContext context;

    @Inject
    private XWikiConverter converter;

    @Inject
    private ConfluenceXMLPackage confluencePackage;

    @Inject
    @Named("relative")
    private EntityReferenceResolver<String> relativeResolver;

    @Inject
    private Logger logger;

    @Override
    public void close() throws IOException
    {
        this.properties.getSource().close();
    }

    @Override
    protected void read(Object filter, ConfluenceFilter proxyFilter) throws FilterException
    {
        if (this.context instanceof DefaultConfluenceInputContext) {
            ((DefaultConfluenceInputContext) this.context).set(this.properties);
        }

        try {
            readInternal(filter, proxyFilter);
        } finally {
            if (this.context instanceof DefaultConfluenceInputContext) {
                ((DefaultConfluenceInputContext) this.context).remove();
            }
        }
    }

    private void readInternal(Object filter, ConfluenceFilter proxyFilter) throws FilterException
    {
        // Prepare package
        try {
            this.confluencePackage.read(this.properties.getSource());
        } catch (Exception e) {
            throw new FilterException("Failed to read package", e);
        }
        ConfluenceFilteringEvent filteringEvent = new ConfluenceFilteringEvent();
        this.observationManager.notify(filteringEvent, this, this.confluencePackage);
        if (filteringEvent.isCanceled()) {
            closeConfluencePackage();
            return;
        }

        Map<Long, List<Long>> pages = this.confluencePackage.getPages();
        Map<Long, List<Long>> blogPages = this.confluencePackage.getBlogPages();

        int pagesCount = pages.size() + pages.entrySet().stream().mapToInt(e -> e.getValue().size()).sum();
        if (this.properties.isBlogsEnabled()) {
            pagesCount = pagesCount + blogPages.entrySet().stream().mapToInt(e -> e.getValue().size()).sum();
        }

        if (this.properties.isUsersEnabled()) {
            Collection<Long> users = this.confluencePackage.getInternalUsers();
            // TODO get users in new format (this.confluencePackage.getAllUsers())
            Collection<Long> groups = this.confluencePackage.getGroups();

            this.progress.pushLevelProgress(users.size() + groups.size() + pagesCount, this);

            sendUsers(users, groups, proxyFilter);
        } else {
            this.progress.pushLevelProgress(pagesCount, this);
        }

        // Generate documents events
        for (Map.Entry<Long, List<Long>> entry : pages.entrySet()) {
            long spaceId = entry.getKey();

            ConfluenceProperties spaceProperties;
            try {
                spaceProperties = this.confluencePackage.getSpaceProperties(spaceId);
            } catch (ConfigurationException e) {
                throw new FilterException("Failed to get space properties", e);
            }

            String spaceKey = toEntityName(ConfluenceXMLPackage.getSpaceKey(spaceProperties));

            FilterEventParameters spaceParameters = new FilterEventParameters();

            // > WikiSpace
            proxyFilter.beginWikiSpace(spaceKey, spaceParameters);

            if (this.properties.isRightsEnabled()) {
                sendSpaceRights(proxyFilter, spaceProperties, spaceKey, spaceId);
            }

            // Main page
            Long descriptionId = spaceProperties.getLong(ConfluenceXMLPackage.KEY_SPACE_DESCRIPTION, null);
            if (descriptionId != null) {
                sendPage(descriptionId, spaceKey, filter, proxyFilter, true);
            }

            // Other pages
            sendPages(filter, proxyFilter, entry, spaceKey);

            // < WikiSpace
            proxyFilter.endWikiSpace(spaceKey, spaceParameters);
        }

        // Generate Blog events
        if (this.properties.isBlogsEnabled()) {
            generateBlogEvents(blogPages, filter, proxyFilter);
        }

        this.progress.popLevelProgress(this);

        // Cleanup

        observationManager.notify(new ConfluenceFilteredEvent(), this, this.confluencePackage);

        closeConfluencePackage();
    }

    private void sendPages(Object filter, ConfluenceFilter proxyFilter, Map.Entry<Long, List<Long>> entry, String spaceKey)
    {
        for (long pageId : entry.getValue()) {
            sendPage(pageId, spaceKey, filter, proxyFilter, false);
        }
    }

    private void sendPage(long pageId, String spaceKey, Object filter, ConfluenceFilter proxyFilter, boolean isMain)
    {
        this.progress.startStep(this);
        if (this.properties.isIncluded(pageId)) {
            try {
                readPage(pageId, spaceKey, filter, proxyFilter);
            } catch (Exception e) {
                logger.error("Failed to filter the {}page with id [{}]. Cause: [{}].", isMain ? " main" : "",
                        createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
            }
        }
        this.progress.endStep(this);
    }

    private void generateBlogEvents(Map<Long, List<Long>> blogPages, Object filter, ConfluenceFilter proxyFilter)
        throws FilterException
    {
        for (Map.Entry<Long, List<Long>> entry : blogPages.entrySet()) {
            long spaceId = entry.getKey();

            ConfluenceProperties spaceProperties;
            try {
                spaceProperties = this.confluencePackage.getSpaceProperties(spaceId);
            } catch (ConfigurationException e) {
                throw new FilterException("Failed to get space properties", e);
            }

            String spaceKey = toEntityName(ConfluenceXMLPackage.getSpaceKey(spaceProperties));

            // > WikiSpace
            proxyFilter.beginWikiSpace(spaceKey, FilterEventParameters.EMPTY);

            // Blog space
            String blogSpaceKey = toEntityName(this.properties.getBlogSpaceName());

            // > WikiSpace
            proxyFilter.beginWikiSpace(blogSpaceKey, FilterEventParameters.EMPTY);

            // Blog Descriptor page
            addBlogDescriptorPage(proxyFilter);

            // Blog post pages
            sendPages(filter, proxyFilter, entry, spaceKey);

            // < WikiSpace
            proxyFilter.endWikiSpace(blogSpaceKey, FilterEventParameters.EMPTY);

            // < WikiSpace
            proxyFilter.endWikiSpace(spaceKey, FilterEventParameters.EMPTY);
        }
    }

    private void sendSpaceRights(ConfluenceFilter proxyFilter, ConfluenceProperties spaceProperties, String spaceKey, long spaceId)
        throws FilterException
    {
        Collection<Object> spacePermissions = spaceProperties.getList(ConfluenceXMLPackage.KEY_SPACE_PERMISSIONS);
        if (!spacePermissions.isEmpty()) {
            boolean webPreferencesStarted = false;
            String spaceWebPreferences = spaceKey + ".WebPreferences";

            // This lets us avoid duplicate XWiki right objects. For instance, REMOVEPAGE and REMOVEBLOG are both mapped
            // to DELETE, and EDITPAGE and EDITBLOG are both mapped to EDIT. In each of these cases, If both rights are
            // set, we need to deduplicate.
            Set<String> addedRights = new HashSet<>();

            for (Object spacePermissionObject : spacePermissions) {
                Long spacePermissionId = Long.parseLong((String) spacePermissionObject);

                ConfluenceProperties spacePermissionProperties;
                try {
                    spacePermissionProperties = this.confluencePackage.getSpacePermissionProperties(spaceId, spacePermissionId);
                } catch (ConfigurationException e) {
                    logger.warn("Failed to get space permission properties [{}] for the space [{}]. Cause: [{}].", spacePermissionId,
                            spaceKey, ExceptionUtils.getRootCauseMessage(e));
                    continue;
                }

                ConfluenceRightData confluenceRight = getConfluenceRightData(spacePermissionProperties);
                if (confluenceRight == null) {
                    continue;
                }

                SpacePermissionType type;
                try {
                    type = SpacePermissionType.valueOf(confluenceRight.type);
                } catch (IllegalArgumentException e) {
                    logger.warn("Failed to understand space permission type [{}] for the space [{}], permission id [{}].",
                            confluenceRight.type, spaceKey, spacePermissionId);
                    continue;
                }

                Right right = null;
                switch (type) {
                    case EXPORTSPACE:
                    case EXPORTPAGE:
                    case REMOVEMAIL:
                    case SETPAGEPERMISSIONS:
                    case SETSPACEPERMISSIONS:
                    case REMOVEOWNCONTENT:
                    case CREATEATTACHMENT:
                    case REMOVEATTACHMENT:
                    case REMOVECOMMENT:
                    case ADMINISTRATECONFLUENCE:
                    case SYSTEMADMINISTRATOR:
                    case PROFILEATTACHMENTS:
                    case UPDATEUSERSTATUS:
                    case ARCHIVEPAGE:
                    case USECONFLUENCE:
                        // These rights are irrelevant in XWiki or can't be represented as-is.
                        // EDITBLOG and REMOVEBLOG can be implemented when migrating blogs is supported.
                        continue;
                    case VIEWSPACE:
                        right = Right.VIEW;
                        break;
                    case EDITSPACE:
                    case EDITBLOG:
                        right = Right.EDIT;
                        break;
                    case CREATESPACE:
                        break;
                    case PERSONALSPACE:
                        break;
                    case REMOVEBLOG:
                    case REMOVEPAGE:
                        right = Right.DELETE;
                        break;
                    case COMMENT:
                        right = Right.COMMENT;
                        break;
                    default:
                        this.logger.warn("Unknown space permission right type [{}].", right);
                        continue;
                }

                String group = confluenceRight.group;
                if (group != null && !group.isEmpty()) {
                    String groupRightString = "g:" + group + ":" + right.toString();
                    if (addedRights.contains(groupRightString)) {
                        group = "";
                    } else {
                        addedRights.add(groupRightString);
                    }
                } else {
                    group = "";
                }

                String user = confluenceRight.user;
                if (user != null && !user.isEmpty()) {
                    String userRightString = "u:" + user + ":" + right.toString();
                    if (addedRights.contains(userRightString)) {
                        user = "";
                    } else {
                        addedRights.add(userRightString);
                    }
                } else {
                    user = "";
                }

                if (right != null && !(user.isEmpty() && group.isEmpty())) {
                    if (!webPreferencesStarted) {
                        proxyFilter.beginWikiDocument(spaceWebPreferences, new FilterEventParameters());
                        webPreferencesStarted = true;
                    }
                    sendRight(proxyFilter, group, right, user);
                }
            }

            if (webPreferencesStarted) {
                proxyFilter.endWikiDocument(spaceWebPreferences, new FilterEventParameters());
            }
        }
    }

    private ConfluenceRightData getConfluenceRightData(ConfluenceProperties permissionProperties)
        throws FilterException
    {
        String type = permissionProperties.getString(ConfluenceXMLPackage.KEY_PERMISSION_TYPE, "");
        String groupStr = permissionProperties.getString(ConfluenceXMLPackage.KEY_SPACEPERMISSION_GROUP, null);
        if (groupStr == null || groupStr.isEmpty()) {
            groupStr = permissionProperties.getString(ConfluenceXMLPackage.KEY_CONTENTPERMISSION_GROUP, null);
        }
        String group = (groupStr == null || groupStr.isEmpty())
                ? ""
                : (toUserReference(getConfluenceToXWikiGroupName(groupStr)));

        String user = "";
        String userName = permissionProperties.getString(ConfluenceXMLPackage.KEY_SPACEPERMISSION_USERNAME, null);
        if (userName == null || userName.isEmpty()) {
            String userSubjectStr = permissionProperties.getString(ConfluenceXMLPackage.KEY_PERMISSION_USERSUBJECT, null);
            if (userSubjectStr != null && !userSubjectStr.isEmpty()) {
                ConfluenceProperties userProperties;
                try {
                    userProperties = confluencePackage.getUserImplProperties(userSubjectStr);
                } catch (ConfigurationException e) {
                    throw new FilterException("Failed to get user properties", e);
                }

                userName = userProperties.getString(ConfluenceXMLPackage.KEY_USER_NAME, userSubjectStr);
            }
        }

        if (userName != null && !userName.isEmpty()) {
            user = toUserReference(userName);
        }

        return new ConfluenceRightData(type, group, user);
    }

    private static class ConfluenceRightData
    {
        public final String type;
        public final String group;
        public final String user;

        public ConfluenceRightData(String type, String group, String user) {
            this.type = type;
            this.group = group;
            this.user = user;
        }
    }

    private static void sendRight(ConfluenceFilter proxyFilter, String group, Right right, String user) throws FilterException
    {
        FilterEventParameters rightParameters = new FilterEventParameters();
        // Page report object
        rightParameters.put(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, XWIKIRIGHTS_CLASSNAME);
        proxyFilter.beginWikiObject(XWIKIRIGHTS_CLASSNAME, rightParameters);
        proxyFilter.onWikiObjectProperty("allow", "1", FilterEventParameters.EMPTY);
        proxyFilter.onWikiObjectProperty("groups", group, FilterEventParameters.EMPTY);
        proxyFilter.onWikiObjectProperty("levels", right.getName(), FilterEventParameters.EMPTY);
        proxyFilter.onWikiObjectProperty("users", user, FilterEventParameters.EMPTY);
        proxyFilter.endWikiObject(XWIKIRIGHTS_CLASSNAME, rightParameters);
    }

    private PageIdentifier createPageIdentifier(Long pageId, String spaceKey)
    {
        PageIdentifier page = new PageIdentifier(pageId);
        page.setSpaceTitle(spaceKey);
        try {
            ConfluenceProperties pageProperties = getPageProperties(pageId);
            if (pageProperties != null) {
                String documentName;
                if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_HOMEPAGE)) {
                    documentName = this.properties.getSpacePageName();
                } else {
                    documentName = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE);
                }
                page.setPageTitle(documentName);
                page.setPageRevision(pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION));
                if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_PARENT)) {
                    Long parentId = pageProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_PARENT);
                    ConfluenceProperties parentPageProperties = getPageProperties(parentId);
                    if (parentPageProperties != null) {
                        page.setParentTitle(parentPageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE));
                    }
                }
            }
        } catch (FilterException ignored) {

        }
        return page;
    }

    private void closeConfluencePackage() throws FilterException
    {
        try {
            this.confluencePackage.close();
        } catch (IOException e) {
            throw new FilterException("Failed to close package", e);
        }
    }

    private void sendUsers(Collection<Long> users, Collection<Long> groups, ConfluenceFilter proxyFilter)
        throws FilterException
    {
        // Switch the wiki if a specific one is forced
        if (this.properties.getUsersWiki() != null) {
            proxyFilter.beginWiki(this.properties.getUsersWiki(), FilterEventParameters.EMPTY);
        }

        // Generate users events
        for (Long userId : users) {
            this.progress.startStep(this);

            ConfluenceProperties userProperties;
            try {
                userProperties = this.confluencePackage.getInternalUserProperties(userId);
            } catch (ConfigurationException e) {
                throw new FilterException("Failed to get user properties", e);
            }

            String userName = toUserReferenceName(
                userProperties.getString(ConfluenceXMLPackage.KEY_USER_NAME, String.valueOf(userId)));

            FilterEventParameters userParameters = new FilterEventParameters();

            userParameters.put(UserFilter.PARAMETER_FIRSTNAME,
                userProperties.getString(ConfluenceXMLPackage.KEY_USER_FIRSTNAME));
            userParameters.put(UserFilter.PARAMETER_LASTNAME,
                userProperties.getString(ConfluenceXMLPackage.KEY_USER_LASTNAME));
            userParameters.put(UserFilter.PARAMETER_EMAIL,
                userProperties.getString(ConfluenceXMLPackage.KEY_USER_EMAIL));
            userParameters.put(UserFilter.PARAMETER_ACTIVE,
                userProperties.getBoolean(ConfluenceXMLPackage.KEY_USER_ACTIVE, true));

            try {
                userParameters.put(UserFilter.PARAMETER_REVISION_DATE,
                    this.confluencePackage.getDate(userProperties, ConfluenceXMLPackage.KEY_USER_REVISION_DATE));
                userParameters.put(UserFilter.PARAMETER_CREATION_DATE,
                    this.confluencePackage.getDate(userProperties, ConfluenceXMLPackage.KEY_USER_CREATION_DATE));
            } catch (Exception e) {
                if (this.properties.isVerbose()) {
                    this.logger.error("Failed to parse the user date", e);
                }
            }

            // TODO: no idea how to import/convert the password, probably salted with the Confluence instance id

            // > User
            proxyFilter.beginUser(userName, userParameters);

            // < User
            proxyFilter.endUser(userName, userParameters);

            this.progress.endStep(this);
        }

        // Generate groups events

        // First, group groups by XWiki group name. There can be several Confluence groups mapping to a unique XWiki group.
        Map<String, Collection<ConfluenceProperties>> groupsByXWikiName = new HashMap<>();
        for (long groupInt : groups) {
            this.progress.startStep(this);

            ConfluenceProperties groupProperties;
            try {
                groupProperties = this.confluencePackage.getGroupProperties(groupInt);
            } catch (ConfigurationException e) {
                throw new FilterException("Failed to get group properties", e);
            }

            String groupName = getConfluenceToXWikiGroupName(
                    groupProperties.getString(ConfluenceXMLPackage.KEY_GROUP_NAME, String.valueOf(groupInt)));

            if (!groupName.isEmpty()) {
                Collection<ConfluenceProperties> l = groupsByXWikiName.getOrDefault(groupName, new ArrayList<>());
                l.add(groupProperties);
                groupsByXWikiName.put(groupName, l);
            }
        }

        // Loop over the XWiki groups
        for (Map.Entry<String, Collection<ConfluenceProperties>> groupEntry: groupsByXWikiName.entrySet()) {
            String groupName = groupEntry.getKey();
            FilterEventParameters groupParameters = new FilterEventParameters();

            // We arbitrarily take the creation and revision date of the first Confluence group mapped to this XWiki group.
            try {
                ConfluenceProperties firstGroupProperties = groupEntry.getValue().iterator().next();
                groupParameters.put(GroupFilter.PARAMETER_REVISION_DATE,
                    this.confluencePackage.getDate(firstGroupProperties, ConfluenceXMLPackage.KEY_GROUP_REVISION_DATE));
                groupParameters.put(GroupFilter.PARAMETER_CREATION_DATE,
                    this.confluencePackage.getDate(firstGroupProperties, ConfluenceXMLPackage.KEY_GROUP_CREATION_DATE));
            } catch (Exception e) {
                if (this.properties.isVerbose()) {
                    this.logger.error("Failed to parse the group date", e);
                }
            }

            // > Group
            proxyFilter.beginGroupContainer(groupName, groupParameters);

            // We add members of all the Confluence groups mapped to this XWiki group to the XWiki group.
            Collection<String> alreadyAddedMembers = new HashSet<>();
            for (ConfluenceProperties groupProperties : groupEntry.getValue()) {
                // Members users
                if (groupProperties.containsKey(ConfluenceXMLPackage.KEY_GROUP_MEMBERUSERS)) {
                    List<Long> groupMembers =
                            this.confluencePackage.getLongList(groupProperties, ConfluenceXMLPackage.KEY_GROUP_MEMBERUSERS);
                    for (Long memberInt : groupMembers) {
                        FilterEventParameters memberParameters = new FilterEventParameters();

                        try {
                            String memberId = toUserReferenceName(this.confluencePackage.getInternalUserProperties(memberInt)
                                    .getString(ConfluenceXMLPackage.KEY_USER_NAME, String.valueOf(memberInt)));

                            if (!alreadyAddedMembers.contains(memberId)) {
                                proxyFilter.onGroupMemberGroup(memberId, memberParameters);
                                alreadyAddedMembers.add(memberId);
                            }
                        } catch (Exception e) {
                            this.logger.error("Failed to get user properties", e);
                        }
                    }
                }

                // Members groups
                if (groupProperties.containsKey(ConfluenceXMLPackage.KEY_GROUP_MEMBERGROUPS)) {
                    List<Long> groupMembers =
                            this.confluencePackage.getLongList(groupProperties, ConfluenceXMLPackage.KEY_GROUP_MEMBERGROUPS);
                    for (Long memberInt : groupMembers) {
                        FilterEventParameters memberParameters = new FilterEventParameters();

                        try {
                            String memberId = getConfluenceToXWikiGroupName(
                                    this.confluencePackage.getGroupProperties(memberInt)
                                            .getString(ConfluenceXMLPackage.KEY_GROUP_NAME, String.valueOf(memberInt)));

                            if (!alreadyAddedMembers.contains(memberId)) {
                                proxyFilter.onGroupMemberGroup(memberId, memberParameters);
                                alreadyAddedMembers.add(memberId);
                            }
                        } catch (Exception e) {
                            this.logger.error("Failed to get group properties", e);
                        }
                    }
                }
            }

            // < Group
            proxyFilter.endGroupContainer(groupName, groupParameters);

            this.progress.endStep(this);
        }

        // Get back to default wiki
        if (this.properties.getUsersWiki() != null) {
            proxyFilter.endWiki(this.properties.getUsersWiki(), FilterEventParameters.EMPTY);
        }
    }

    private String getConfluenceToXWikiGroupName(String groupName)
    {
        if (!this.properties.isConvertToXWiki() || this.properties.getGroupMapping() == null) {
            return groupName;
        }

        return this.properties.getGroupMapping().getOrDefault(groupName, groupName);
    }

    private void readPage(long pageId, String spaceKey, Object filter, ConfluenceFilter proxyFilter)
        throws FilterException
    {
        ConfluenceProperties pageProperties = getPageProperties(pageId);

        if (pageProperties == null) {
            this.logger.error("Can't find page with id [{}]", createPageIdentifier(pageId, spaceKey));

            return;
        }

        String documentName;
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_HOMEPAGE)) {
            documentName = this.properties.getSpacePageName();
        } else {
            documentName = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE);
        }

        // Skip pages with empty title
        if (StringUtils.isEmpty(documentName)) {
            this.logger.warn("Found a page without a name or title (id={}). Skipping it.",
                createPageIdentifier(pageId, spaceKey));

            return;
        }

        // Skip deleted, archived or draft pages
        String contentStatus = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_CONTENT_STATUS);
        if (contentStatus != null
            && (contentStatus.equals("deleted") || contentStatus.equals("archived") || contentStatus.equals("draft"))) {
            return;
        }

        FilterEventParameters documentParameters = new FilterEventParameters();
        if (this.properties.getDefaultLocale() != null) {
            documentParameters.put(WikiDocumentFilter.PARAMETER_LOCALE, this.properties.getDefaultLocale());
        }

        // Apply the standard entity name validator
        documentName = toEntityName(documentName);

        // > WikiDocument
        proxyFilter.beginWikiDocument(documentName, documentParameters);

        if (this.properties.isContentsEnabled()) {
            sendRevisions(pageId, spaceKey, filter, proxyFilter, pageProperties);
        }

        if (this.properties.isRightsEnabled()) {
            sendPageRights(pageId, spaceKey, proxyFilter, pageProperties);
        }

        // < WikiDocument
        proxyFilter.endWikiDocument(documentName, documentParameters);
    }

    private void sendRevisions(long pageId, String spaceKey, Object filter, ConfluenceFilter proxyFilter, ConfluenceProperties pageProperties)
        throws FilterException
    {
        Locale locale = Locale.ROOT;

        FilterEventParameters documentLocaleParameters = new FilterEventParameters();
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_CREATION_AUTHOR)) {
            documentLocaleParameters.put(WikiDocumentFilter.PARAMETER_CREATION_AUTHOR,
                toUserReference(pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_CREATION_AUTHOR)));
        } else if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_CREATION_AUTHOR_KEY)) {
            String authorKey = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_CREATION_AUTHOR_KEY);
            String authorName = toUserReference(resolveUserName(authorKey, authorKey));
            documentLocaleParameters.put(WikiDocumentFilter.PARAMETER_CREATION_AUTHOR, authorName);
        }

        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_CREATION_DATE)) {
            try {
                documentLocaleParameters.put(WikiDocumentFilter.PARAMETER_CREATION_DATE,
                    this.confluencePackage.getDate(pageProperties, ConfluenceXMLPackage.KEY_PAGE_CREATION_DATE));
            } catch (Exception e) {
                this.logger.warn("Failed to parse creation date of the document with id [{}]. Cause [{}].",
                    createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
            }
        }

        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISION)) {
            documentLocaleParameters.put(WikiDocumentFilter.PARAMETER_LASTREVISION,
                pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION));
        }

        // > WikiDocumentLocale
        proxyFilter.beginWikiDocumentLocale(locale, documentLocaleParameters);

        // Revisions
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISIONS)) {
            List<Long> revisions =
                this.confluencePackage.getLongList(pageProperties, ConfluenceXMLPackage.KEY_PAGE_REVISIONS);
            for (Long revisionId : revisions) {
                readPageRevision(revisionId, spaceKey, filter, proxyFilter);
            }
        }

        // Current version
        readPageRevision(pageId, spaceKey, filter, proxyFilter);

        // < WikiDocumentLocale
        proxyFilter.endWikiDocumentLocale(locale, documentLocaleParameters);
    }

    private void sendPageRights(long pageId, String spaceKey, ConfluenceFilter proxyFilter, ConfluenceProperties pageProperties) throws FilterException {
        for (Object permissionSetIdStr : pageProperties.getList("contentPermissionSets")) {
            long permissionSetId = Long.parseLong((String) permissionSetIdStr);
            ConfluenceProperties permissionSetProperties = null;
            try {
                permissionSetProperties = confluencePackage.getContentPermissionSetProperties(permissionSetId);
            } catch (ConfigurationException e) {
                logger.warn("Could not get permission set [{}] for page [{}]. Cause: [{}].",
                        permissionSetId, createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
                continue;
            }

            if (permissionSetProperties == null) {
                logger.warn("Could not find permission set [{}] for page [{}].", permissionSetId, createPageIdentifier(pageId, spaceKey));
                continue;
            }

            for (Object permissionIdStr : permissionSetProperties.getList("contentPermissions")) {
                long permissionId = Long.parseLong((String) permissionIdStr);
                ConfluenceProperties permissionProperties = null;
                try {
                    permissionProperties = confluencePackage.getContentPermissionProperties(permissionSetId, permissionId);
                } catch (ConfigurationException e) {
                    logger.warn("Could not get permission [{}] for page [{}]. Cause: [{}].",
                            permissionId, createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
                    continue;
                }

                if (permissionProperties == null) {
                    logger.warn("Could not find permission [{}] for page [{}].", permissionId, createPageIdentifier(pageId, spaceKey));
                    continue;
                }

                ConfluenceRightData confluenceRight = getConfluenceRightData(permissionProperties);

                ContentPermissionType type;
                try {
                    type = ContentPermissionType.valueOf(confluenceRight.type.toUpperCase());
                } catch (IllegalArgumentException e) {
                    logger.warn("Failed to understand content permission type [{}] for page [{}], permission id [{}].",
                            confluenceRight.type, createPageIdentifier(pageId, spaceKey), permissionId);
                    continue;
                }

                Right right = null;
                switch (type) {
                    case VIEW:
                        right = Right.VIEW;
                        break;
                    case EDIT:
                        right = Right.EDIT;
                        break;
                    case SHARE:
                        // Sharing is not represented in XWiki rights
                        continue;
                    default:
                        this.logger.warn("Unknown content permission right type [{}].", right);
                        continue;
                }

                if (right != null) {
                    sendRight(proxyFilter, confluenceRight.group, right, confluenceRight.user);
                }
            }
        }
    }

    String resolveUserName(String key, String def)
    {
        try {
            ConfluenceProperties userProperties = this.confluencePackage.getUserProperties(key);

            if (userProperties != null) {
                String userName = userProperties.getString(ConfluenceXMLPackage.KEY_USER_NAME);

                if (userName != null) {
                    return userName;
                }
            }
        } catch (ConfigurationException e) {
            this.logger.warn("Failed to retrieve properties of user with key [{}]: {}", key,
                ExceptionUtils.getRootCauseMessage(e));
        }

        return def;
    }

    String toMappedUser(String confluenceUser)
    {
        if (this.properties.getUserIdMapping() != null) {
            String mappedName = this.properties.getUserIdMapping().get(confluenceUser);

            if (mappedName != null) {
                mappedName = mappedName.trim();

                if (!mappedName.isEmpty()) {
                    return mappedName;
                }
            }
        }

        return confluenceUser;
    }

    String toUserReferenceName(String userName)
    {
        if (userName == null || !this.properties.isConvertToXWiki()) {
            // Apply the configured mapping
            return toMappedUser(userName);
        }

        // Translate the usual default admin user in Confluence to it's XWiki counterpart
        if (userName.equals("admin")) {
            return "Admin";
        }

        // Apply the configured mapping
        userName = toMappedUser(userName);

        // Protected from characters not well supported in user page name depending on the version of XWiki
        userName = FORBIDDEN_USER_CHARACTERS.matcher(userName).replaceAll("_");

        return userName;
    }

    String toUserReference(String userName)
    {
        if (userName == null || !this.properties.isConvertToXWiki()) {
            return userName;
        }

        // Transform user name according to configuration
        userName = toUserReferenceName(userName);

        // Add the "XWiki" space and the wiki if configured. Ideally this should probably be done on XWiki Instance
        // Output filter side
        EntityReference reference;
        if (this.properties.getUsersWiki() != null) {
            reference = new DocumentReference(this.properties.getUsersWiki(), "XWiki", userName);
        } else {
            reference = new LocalDocumentReference("XWiki", userName);
        }

        return this.serializer.serialize(reference);
    }

    private ConfluenceProperties getPageProperties(Long pageId) throws FilterException
    {
        try {
            return this.confluencePackage.getPageProperties(pageId, false);
        } catch (ConfigurationException e) {
            throw new FilterException("Failed to get page properties", e);
        }
    }

    private void readPageRevision(Long pageId, String spaceKey, Object filter, ConfluenceFilter proxyFilter)
        throws FilterException
    {
        ConfluenceProperties pageProperties = getPageProperties(pageId);

        if (pageProperties == null) {
            this.logger.warn("Can't find page revision with id [{}]", createPageIdentifier(pageId, spaceKey));
            return;
        }

        readPageRevision(pageId, spaceKey, pageProperties, filter, proxyFilter);
    }

    private void readPageRevision(long pageId, String spaceKey, ConfluenceProperties pageProperties, Object filter,
        ConfluenceFilter proxyFilter) throws FilterException
    {
        String revision = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION);

        FilterEventParameters documentRevisionParameters = new FilterEventParameters();
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_PARENT)) {
            try {
                documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_PARENT,
                    getReferenceFromId(pageProperties, ConfluenceXMLPackage.KEY_PAGE_PARENT));
            } catch (Exception e) {
                this.logger.warn("Failed to parse parent for the document with id [{}]. Cause: [{}].",
                    createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
            }
        }
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISION_AUTHOR)) {
            documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_EFFECTIVEMETADATA_AUTHOR,
                toUserReference(pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION_AUTHOR)));
        } else if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISION_AUTHOR_KEY)) {
            String authorKey = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION_AUTHOR_KEY);
            String authorName = toUserReference(resolveUserName(authorKey, authorKey));
            documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_EFFECTIVEMETADATA_AUTHOR, authorName);
        }
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISION_DATE)) {
            try {
                documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_DATE,
                    this.confluencePackage.getDate(pageProperties, ConfluenceXMLPackage.KEY_PAGE_REVISION_DATE));
            } catch (Exception e) {
                this.logger.warn("Failed to parse the revision date of the document with id [{}]. Cause: [{}].",
                    createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
            }
        }
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISION_COMMENT)) {
            documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_COMMENT,
                pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION_COMMENT));
        }
        documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_TITLE,
            pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE));

        String bodyContent = null;
        Syntax bodySyntax = null;
        int bodyType = -1;

        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_BODY)) {
            bodyContent = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_BODY);
            bodyType = pageProperties.getInt(ConfluenceXMLPackage.KEY_PAGE_BODY_TYPE, -1);

            switch (bodyType) {
                // Not bodyType means old Confluence syntax
                case -1:
                    bodyType = 0;
                case 0:
                    bodySyntax = ConfluenceParser.SYNTAX;
                    break;
                case 2:
                    bodySyntax = Syntax.CONFLUENCEXHTML_1_0;
                    break;
                default:
                    this.logger.warn("Unknown body type [{}] for the content of the document with id [{}].", bodyType,
                        createPageIdentifier(pageId, spaceKey));
                    break;
            }
        }

        // Generate page content when the page is a regular page or the value of the "content" property of the
        // "Blog.BlogPostClass" object if the page is a blog post.
        if (!pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_BLOGPOST)) {
            if (bodyContent != null) {
                if (this.properties.isContentEvents() && filter instanceof Listener) {
                    // > WikiDocumentRevision
                    proxyFilter.beginWikiDocumentRevision(revision, documentRevisionParameters);

                    try {
                        parse(bodyContent, bodyType, this.properties.getMacroContentSyntax(), proxyFilter);
                    } catch (Exception e) {
                        this.logger.warn("Failed to parse content of page with id [{}]. Cause: [{}].",
                            createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
                    }
                } else if (this.properties.isConvertToXWiki()) {
                    // Convert content to XWiki syntax
                    try {
                        documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_CONTENT,
                            convertToXWiki21(bodyContent, bodyType));
                        documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_SYNTAX, Syntax.XWIKI_2_1);
                    } catch (Exception e) {
                        this.logger.warn("Failed to convert content of the page with id [{}]. Cause: [{}].",
                            createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
                    }

                    // > WikiDocumentRevision
                    proxyFilter.beginWikiDocumentRevision(revision, documentRevisionParameters);
                } else {
                    // Keep Confluence syntax
                    documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_CONTENT, bodyContent);
                    documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_SYNTAX, bodySyntax);

                    // > WikiDocumentRevision
                    proxyFilter.beginWikiDocumentRevision(revision, documentRevisionParameters);
                }
            } else {
                // > WikiDocumentRevision
                proxyFilter.beginWikiDocumentRevision(revision, documentRevisionParameters);
            }
        } else {
            String blogPostContent = bodyContent;

            if (bodyContent != null) {
                if (this.properties.isConvertToXWiki()) {
                    // Convert content to XWiki syntax
                    try {
                        blogPostContent = convertToXWiki21(bodyContent, bodyType);
                        documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_SYNTAX, Syntax.XWIKI_2_1);
                    } catch (Exception e) {
                        this.logger.warn("Failed to convert content of the page with id [{}]. Cause: [{}].",
                            createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
                    }
                } else {
                    // Keep Confluence syntax
                    documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_SYNTAX, bodySyntax);
                }
            }

            // > WikiDocumentRevision
            proxyFilter.beginWikiDocumentRevision(revision, documentRevisionParameters);

            // Add the Blog post object
            Date publishDate = null;
            try {
                publishDate =
                    this.confluencePackage.getDate(pageProperties, ConfluenceXMLPackage.KEY_PAGE_REVISION_DATE);
            } catch (Exception e) {
                this.logger.warn(
                    "Failed to parse the publish date of the blog post document with id [{}]. Cause: [{}].",
                    createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
            }

            addBlogPostObject(pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE), blogPostContent,
                publishDate, proxyFilter);
        }

        // Attachments
        Map<String, ConfluenceProperties> pageAttachments = new LinkedHashMap<>();
        for (long attachmentId : this.confluencePackage.getAttachments(pageId)) {
            ConfluenceProperties attachmentProperties;
            try {
                attachmentProperties = this.confluencePackage.getAttachmentProperties(pageId, attachmentId);
            } catch (ConfigurationException e) {
                logger.warn(
                    "Failed to get the properties of the attachments from the document identified by [{}]. Cause: [{}].",
                    createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
                continue;
            }

            String attachmentName = this.confluencePackage.getAttachmentName(attachmentProperties);

            ConfluenceProperties currentAttachmentProperties = pageAttachments.get(attachmentName);
            if (currentAttachmentProperties != null) {
                try {
                    Date date = this.confluencePackage.getDate(attachmentProperties,
                        ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_DATE);
                    Date currentDate = this.confluencePackage.getDate(currentAttachmentProperties,
                        ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_DATE);

                    if (date.after(currentDate)) {
                        pageAttachments.put(attachmentName, attachmentProperties);
                    }
                } catch (Exception e) {
                    this.logger.warn(
                        "Failed to parse the date of attachment [{}] from the page with id [{}], skipping it. Cause: [{}].",
                        createPageIdentifier(pageId, spaceKey), attachmentId, ExceptionUtils.getRootCauseMessage(e));
                }
            } else {
                pageAttachments.put(attachmentName, attachmentProperties);
            }
        }

        for (ConfluenceProperties attachmentProperties : pageAttachments.values()) {
            readAttachment(pageId, spaceKey, attachmentProperties, filter, proxyFilter);
        }

        // Tags
        Map<String, ConfluenceProperties> pageTags = new LinkedHashMap<>();
        for (Object tagIdStringObject : pageProperties.getList(ConfluenceXMLPackage.KEY_PAGE_LABELLINGS)) {
            long tagId = Long.parseLong((String) tagIdStringObject);
            ConfluenceProperties tagProperties;
            try {
                tagProperties = this.confluencePackage.getObjectProperties(tagId);
            } catch (ConfigurationException e) {
                logger.warn("Failed to get tag properties [{}] for the page with id [{}]. Cause: [{}].", tagId,
                    createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
                continue;
            }

            String tagName = this.confluencePackage.getTagName(tagProperties);
            pageTags.put(tagName, tagProperties);
        }

        if (!pageTags.isEmpty()) {
            readPageTags(proxyFilter, pageTags);
        }

        // Comments
        Map<Long, ConfluenceProperties> pageComments = new LinkedHashMap<>();
        Map<Long, Integer> commentIndeces = new LinkedHashMap<>();
        int commentIndex = 0;
        for (Object commentIdStringObject : pageProperties.getList(ConfluenceXMLPackage.KEY_PAGE_COMMENTS)) {
            long commentId = Long.parseLong((String) commentIdStringObject);
            ConfluenceProperties commentProperties;
            try {
                commentProperties = this.confluencePackage.getObjectProperties(commentId);
            } catch (ConfigurationException e) {
                logger.warn("Failed to get the comment properties [{}] for the page with id [{}]. Cause: [{}].",
                    commentId, createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
                continue;
            }

            pageComments.put(commentId, commentProperties);
            commentIndeces.put(commentId, commentIndex);
            commentIndex++;
        }

        for (Long commentId : pageComments.keySet()) {
            readPageComment(pageId, spaceKey, proxyFilter, commentId, pageComments, commentIndeces);
        }

        if (this.properties.isStoreConfluenceDetailsEnabled()) {
            storeConfluenceDetails(pageId, spaceKey, pageProperties, proxyFilter);
        }

        // < WikiDocumentRevision
        proxyFilter.endWikiDocumentRevision(revision, documentRevisionParameters);
    }

    /**
     * @param currentProperties the properties where to find the page identifier
     * @param key the key to find the page identifier
     * @return the reference of the page
     * @throws ConfigurationException when failing to get page properties
     * @throws FilterException when failing to create the reference
     */
    public EntityReference getReferenceFromId(ConfluenceProperties currentProperties, String key)
        throws ConfigurationException, FilterException
    {
        Long pageId = currentProperties.getLong(key, null);
        if (pageId != null) {
            ConfluenceProperties pageProperties = this.confluencePackage.getPageProperties(pageId, true);

            long spaceId = pageProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_SPACE);
            String pageTitle = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE);

            if (StringUtils.isNotEmpty(pageTitle)) {
                long currentSpaceId = currentProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_SPACE);

                EntityReference spaceReference = null;
                if (spaceId != currentSpaceId) {
                    String spaceName = this.confluencePackage.getSpaceKey(spaceId);
                    if (spaceName != null) {
                        spaceReference = new EntityReference(toEntityName(spaceName), EntityType.SPACE);
                    }
                }

                return new EntityReference(toEntityName(pageTitle), EntityType.DOCUMENT, spaceReference);
            } else {
                throw new FilterException("Cannot create a reference to the page with id [" + pageId
                    + "] because it does not have any title");
            }
        }

        return null;
    }

    /**
     * @param name the name to validate
     * @return the validated name
     * @since 9.16.1
     */
    public String toEntityName(String name)
    {
        if (this.properties.isConvertToXWiki() && this.properties.isEntityNameValidation()) {
            return this.converter.convert(name);
        }

        return name;
    }

    /**
     * @param entityReference the reference to convert
     * @return the converted reference
     * @since 9.22.1
     */
    public EntityReference convert(EntityReference entityReference)
    {
        if (this.properties.isConvertToXWiki() && this.properties.isEntityNameValidation()) {
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
     * @since 9.22.1
     */
    public String convert(String entityReference, EntityType entityType)
    {
        if (StringUtils.isNotEmpty(entityReference) && this.properties.isConvertToXWiki()
            && this.properties.isEntityNameValidation()) {
            // Parse the reference
            EntityReference documentReference = this.relativeResolver.resolve(entityReference, entityType);

            // Fix the reference according to entity conversion rules
            documentReference = convert(documentReference);

            // Serialize the fixed reference
            return this.serializer.serialize(documentReference);
        }

        return entityReference;
    }

    /**
     * @since 9.13
     */
    private void storeConfluenceDetails(long pageId, String spaceKey, ConfluenceProperties pageProperties,
        ConfluenceFilter proxyFilter) throws FilterException
    {
        FilterEventParameters pageReportParameters = new FilterEventParameters();

        // Page report object
        pageReportParameters.put(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, CONFLUENCEPAGE_CLASSNAME);
        proxyFilter.beginWikiObject(CONFLUENCEPAGE_CLASSNAME, pageReportParameters);

        StringBuilder pageURLBuilder = new StringBuilder();
        if (this.properties.getBaseURLs() != null) {
            pageURLBuilder.append(this.properties.getBaseURLs().get(0).toString());
            pageURLBuilder.append("/wiki/spaces/").append(spaceKey);
            if (!pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_HOMEPAGE)) {
                String pageName = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE);
                pageURLBuilder.append("/pages/").append(pageId).append("/").append(pageName);
            }
        }

        proxyFilter.onWikiObjectProperty("id", pageId, FilterEventParameters.EMPTY);
        proxyFilter.onWikiObjectProperty("url", pageURLBuilder.toString(), FilterEventParameters.EMPTY);
        proxyFilter.onWikiObjectProperty("space", spaceKey, FilterEventParameters.EMPTY);

        proxyFilter.endWikiObject(CONFLUENCEPAGE_CLASSNAME, pageReportParameters);
    }

    private String convertToXWiki21(String bodyContent, int bodyType) throws FilterException, ParseException
    {
        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        PrintRenderer renderer = this.xwiki21Factory.createRenderer(printer);

        parse(bodyContent, bodyType, Syntax.XWIKI_2_1, renderer);

        return printer.toString();
    }

    private ConfluenceConverterListener createConverter(Listener listener)
    {
        ConfluenceConverterListener converterListener = this.converterProvider.get();
        converterListener.initialize(this.confluencePackage, this, this.properties);
        converterListener.setWrappedListener(listener);

        return converterListener;
    }

    private Listener wrap(Listener listener)
    {
        if (this.properties.isConvertToXWiki()) {
            return createConverter(listener);
        }

        return listener;
    }

    private void parse(String bodyContent, int bodyType, Syntax macroContentSyntax, Listener listener)
        throws FilterException, ParseException
    {
        switch (bodyType) {
            case 0:
                this.confluenceWIKIParser.parse(new StringReader(bodyContent), wrap(listener));
                break;
            case 2:
                createSyntaxFilter(bodyContent, macroContentSyntax).read(listener);
                break;
            default:
                break;
        }
    }

    private BeanInputFilterStream<ConfluenceXHTMLInputProperties> createSyntaxFilter(String bodyContent,
        Syntax macroContentSyntax) throws FilterException
    {
        InternalConfluenceXHTMLInputProperties filterProperties = new InternalConfluenceXHTMLInputProperties();
        filterProperties.setSource(new StringInputSource(bodyContent));
        filterProperties.setMacroContentSyntax(macroContentSyntax);

        if (this.properties.isConvertToXWiki()) {
            filterProperties.setConverter(createConverter(null));
        }

        BeanInputFilterStreamFactory<ConfluenceXHTMLInputProperties> syntaxFilterFactory =
            ((BeanInputFilterStreamFactory<ConfluenceXHTMLInputProperties>) this.confluenceXHTMLParserFactory);

        return syntaxFilterFactory.createInputFilterStream(filterProperties);
    }

    private void readAttachment(long pageId, String spaceKey, ConfluenceProperties attachmentProperties, Object filter,
        ConfluenceFilter proxyFilter) throws FilterException
    {
        String contentStatus = attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENTSTATUS, null);
        if (StringUtils.equals(contentStatus, "deleted")) {
            // The actual deleted attachment is not in the exported package so we can't really do anything with it
            return;
        }

        long attachmentId = attachmentProperties.getLong("id");

        String attachmentName = this.confluencePackage.getAttachmentName(attachmentProperties);

        long attachmentSize;
        String mediaType = null;
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENTPROPERTIES)) {
            ConfluenceProperties attachmentContentProperties =
                getContentProperties(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENTPROPERTIES);

            attachmentSize =
                attachmentContentProperties.getLong(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENT_FILESIZE, -1);
            if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENTTYPE)) {
                mediaType =
                    attachmentContentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENT_MEDIA_TYPE);
            }
        } else {
            attachmentSize = attachmentProperties.getLong(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENT_SIZE, -1);
            if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENTTYPE)) {
                mediaType = attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENTTYPE);
            }
        }

        Long version = this.confluencePackage.getAttachementVersion(attachmentProperties);

        long originalRevisionId =
            this.confluencePackage.getAttachmentOriginalVersionId(attachmentProperties, attachmentId);
        File contentFile;
        try {
            contentFile = this.confluencePackage.getAttachmentFile(pageId, originalRevisionId, version);
        } catch (Exception e) {
            this.logger.warn("Failed to find file corresponding to version [{}] attachment [{}] in page [{}]: {}",
                version, attachmentName, createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
            return;
        }

        FilterEventParameters attachmentParameters = new FilterEventParameters();
        if (mediaType != null) {
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_CONTENT_TYPE, mediaType);
        }
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_AUTHOR)) {
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_CREATION_AUTHOR,
                attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_AUTHOR));
        }
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_DATE)) {
            try {
                attachmentParameters.put(WikiAttachmentFilter.PARAMETER_CREATION_DATE, this.confluencePackage
                    .getDate(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_DATE));
            } catch (Exception e) {
                this.logger.warn("Failed to parse the creation date of the attachment [{}] in page [{}]. Cause: [{}].",
                    attachmentId, createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
            }
        }

        attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION, String.valueOf(version));
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_AUTHOR)) {
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION_AUTHOR,
                attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_AUTHOR));
        }
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_DATE)) {
            try {
                attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION_DATE, this.confluencePackage
                    .getDate(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_DATE));
            } catch (Exception e) {
                this.logger.warn("Failed to parse the revision date of the attachment [{}] in page [{}]. Cause: [{}].",
                    attachmentId, createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
            }
        }
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_COMMENT)) {
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION_COMMENT,
                attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_COMMENT));
        }

        // WikiAttachment

        try (FileInputStream fis = new FileInputStream(contentFile)) {
            proxyFilter.onWikiAttachment(attachmentName, fis,
                attachmentSize != -1 ? attachmentSize : contentFile.length(), attachmentParameters);
        } catch (Exception e) {
            logger.warn("Failed to read attachment [{}] for the page [{}]. Cause: [{}].", attachmentId,
                createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private void readPageTags(ConfluenceFilter proxyFilter, Map<String, ConfluenceProperties> pageTags)
        throws FilterException
    {
        FilterEventParameters pageTagsParameters = new FilterEventParameters();

        // Tag object
        pageTagsParameters.put(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, TAGS_CLASSNAME);
        proxyFilter.beginWikiObject(TAGS_CLASSNAME, pageTagsParameters);

        // get page tags separated by | as string
        StringBuilder tagBuilder = new StringBuilder();
        String prefix = "";
        for (String tag : pageTags.keySet()) {
            tagBuilder.append(prefix);
            tagBuilder.append(tag);
            prefix = "|";
        }

        // <tags> object property
        proxyFilter.onWikiObjectProperty("tags", tagBuilder.toString(), FilterEventParameters.EMPTY);

        proxyFilter.endWikiObject(TAGS_CLASSNAME, pageTagsParameters);
    }

    private void readPageComment(Long pageId, String spaceKey, ConfluenceFilter proxyFilter, Long commentId,
        Map<Long, ConfluenceProperties> pageComments, Map<Long, Integer> commentIndeces) throws FilterException
    {
        FilterEventParameters commentParameters = new FilterEventParameters();

        // Comment object
        commentParameters.put(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, COMMENTS_CLASSNAME);
        proxyFilter.beginWikiObject(COMMENTS_CLASSNAME, commentParameters);

        // object properties
        ConfluenceProperties commentProperties = pageComments.get(commentId);

        // creator
        String commentCreator;
        if (commentProperties.containsKey("creatorName")) {
            // old creator reference by name
            commentCreator = commentProperties.getString("creatorName");
        } else {
            // new creator reference by key
            commentCreator = commentProperties.getString("creator");
            commentCreator = resolveUserName(commentCreator, commentCreator);
        }
        String commentCreatorReference = toUserReference(commentCreator);

        // content
        String commentBodyContent = this.confluencePackage.getCommentText(commentId);
        int commentBodyType = this.confluencePackage.getCommentBodyType(commentId);
        String commentText = commentBodyContent;
        if (commentBodyContent != null && this.properties.isConvertToXWiki()) {
            try {
                commentText = convertToXWiki21(commentBodyContent, commentBodyType);
            } catch (Exception e) {
                this.logger.warn("Failed to convert content of the comment with id [{}] for page [{}]. Cause: [{}].",
                    commentId, createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
            }
        }

        // creation date
        Date commentDate = null;
        try {
            commentDate = this.confluencePackage.getDate(commentProperties, "creationDate");
        } catch (Exception e) {
            this.logger.warn("Failed to parse the creation date of the comment [{}] in page [{}]. Cause: [{}].",
                commentId, createPageIdentifier(pageId, spaceKey), ExceptionUtils.getRootCauseMessage(e));
        }

        // parent (replyto)
        Integer parentIndex = null;
        if (commentProperties.containsKey("parent")) {
            Long parentId = commentProperties.getLong("parent");
            parentIndex = commentIndeces.get(parentId);
        }

        proxyFilter.onWikiObjectProperty("author", commentCreatorReference, FilterEventParameters.EMPTY);
        proxyFilter.onWikiObjectProperty("comment", commentText, FilterEventParameters.EMPTY);
        proxyFilter.onWikiObjectProperty("date", commentDate, FilterEventParameters.EMPTY);
        proxyFilter.onWikiObjectProperty("replyto", parentIndex, FilterEventParameters.EMPTY);

        proxyFilter.endWikiObject(COMMENTS_CLASSNAME, commentParameters);
    }

    private void addBlogDescriptorPage(ConfluenceFilter proxyFilter) throws FilterException
    {
        // Apply the standard entity name validator
        String documentName = toEntityName(this.properties.getSpacePageName());

        // > WikiDocument
        proxyFilter.beginWikiDocument(documentName, FilterEventParameters.EMPTY);

        FilterEventParameters blogParameters = new FilterEventParameters();

        // Blog Object
        blogParameters.put(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, BLOG_CLASSNAME);

        proxyFilter.beginWikiObject(BLOG_CLASSNAME, blogParameters);

        // Object properties
        proxyFilter.onWikiObjectProperty("title", this.properties.getBlogSpaceName(), FilterEventParameters.EMPTY);
        proxyFilter.onWikiObjectProperty("postsLayout", "image", FilterEventParameters.EMPTY);
        proxyFilter.onWikiObjectProperty("displayType", "paginated", FilterEventParameters.EMPTY);

        proxyFilter.endWikiObject(BLOG_CLASSNAME, blogParameters);

        // < WikiDocument
        proxyFilter.endWikiDocument(documentName, FilterEventParameters.EMPTY);
    }

    private void addBlogPostObject(String title, String content, Date publishDate, ConfluenceFilter proxyFilter)
        throws FilterException
    {
        FilterEventParameters blogPostParameters = new FilterEventParameters();

        // Blog Post Object
        blogPostParameters.put(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, BLOG_POST_CLASSNAME);

        proxyFilter.beginWikiObject(BLOG_POST_CLASSNAME, blogPostParameters);

        // Object properties
        proxyFilter.onWikiObjectProperty("title", title, FilterEventParameters.EMPTY);
        proxyFilter.onWikiObjectProperty("content", content, FilterEventParameters.EMPTY);
        proxyFilter.onWikiObjectProperty("publishDate", publishDate, FilterEventParameters.EMPTY);

        // The blog post 'published' property is always set to true because unpublished blog posts are draft pages and
        // draft pages are skipped during the import.
        proxyFilter.onWikiObjectProperty("published", 1, FilterEventParameters.EMPTY);
        proxyFilter.onWikiObjectProperty("hidden", 0, FilterEventParameters.EMPTY);

        proxyFilter.endWikiObject(BLOG_POST_CLASSNAME, blogPostParameters);
    }

    private ConfluenceProperties getContentProperties(ConfluenceProperties properties, String key)
        throws FilterException
    {
        try {
            return this.confluencePackage.getContentProperties(properties, key);
        } catch (Exception e) {
            throw new FilterException("Failed to parse content properties", e);
        }
    }
}
