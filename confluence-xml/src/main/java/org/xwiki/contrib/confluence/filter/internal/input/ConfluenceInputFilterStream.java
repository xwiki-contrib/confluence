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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.confluence.filter.PageIdentifier;
import org.xwiki.contrib.confluence.filter.event.ConfluenceFilteredEvent;
import org.xwiki.contrib.confluence.filter.event.ConfluenceFilteringEvent;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.contrib.confluence.filter.input.OverwriteProtectionMode;
import org.xwiki.contrib.confluence.filter.input.ConfluenceProperties;
import org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage;
import org.xwiki.contrib.confluence.filter.input.ContentPermissionType;
import org.xwiki.contrib.confluence.filter.input.SpacePermissionType;
import org.xwiki.contrib.confluence.filter.internal.ConfluenceFilter;
import org.xwiki.contrib.confluence.filter.internal.idrange.ConfluenceIdRangeList;
import org.xwiki.contrib.confluence.parser.confluence.internal.ConfluenceParser;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceURLConverter;
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
import org.xwiki.filter.input.DefaultFileInputSource;
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.StringInputSource;
import org.xwiki.job.Job;
import org.xwiki.job.JobContext;
import org.xwiki.job.event.status.CancelableJobStatus;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
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
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)*+");

    private static final String TEMPLATE_PROVIDER_CLASS = "XWiki.TemplateProviderClass";

    private static final String CONFLUENCEPAGE_CLASSNAME = "Confluence.Code.ConfluencePageClass";

    private static final String TAGS_CLASSNAME = "XWiki.TagClass";

    private static final String COMMENTS_CLASSNAME = "XWiki.XWikiComments";

    private static final String BLOG_CLASSNAME = "Blog.BlogClass";

    private static final String BLOG_POST_CLASSNAME = "Blog.BlogPostClass";

    private static final String XWIKIRIGHTS_CLASSNAME = "XWiki.XWikiRights";

    private static final String XWIKIGLOBALRIGHTS_CLASSNAME = "XWiki.XWikiGlobalRights";

    private static final String WEB_PREFERENCES = "WebPreferences";

    private static final String FAILED_TO_GET_USER_PROPERTIES = "Failed to get user properties";

    private static final String WEB_HOME = "WebHome";

    private static final String XWIKI_PREFERENCES_CLASS = "XWiki.XWikiPreferences";

    private static final String FAILED_TO_READ_PACKAGE = "Failed to read package";

    private static final String DESCRIPTOR_SOURCE_FIELD = "source";

    private static final String PINNED_CHILD_PAGES_CLASS = "XWiki.PinnedChildPagesClass";

    private static final String TITLE = "title";

    private static final String CREATOR_NAME = "creatorName";

    private static final String PARENT = "parent";

    private static final String INLINE_MARKER_REF = "inline-marker-ref";

    private  static final String FAILED_TO_GET_GROUP_PROPERTIES = "Failed to get group properties";

    private static final String ERROR_SPACE_KEY_RESOLUTION =
        "Failed to resolve space key for id [{}] referenced from object with id [{}]";

    private static final String PAGE_IDENTIFIER_ERROR =
        "Configuration error while creating page identifier for page [{}]";

    private static final Marker SEND_PAGE_MARKER = MarkerFactory.getMarker("ConfluenceSendingPage");

    private static final Marker SEND_TEMPLATE_MARKER = MarkerFactory.getMarker("ConfluenceSendingTemplate");

    private static final String NAME = "name";

    private static final String DESCRIPTION = "description";

    private static final String COMMENT = "comment";

    private static final String ATTACHMENT = "attachment";

    private static final String REVISION = "revision";

    private static final String ONE = "1";

    @Inject
    @Named(ConfluenceInputStreamParser.COMPONENT_NAME)
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
    private ObservationManager observationManager;

    @Inject
    private ConfluenceInputContext context;

    @Inject
    private ConfluenceConverter confluenceConverter;

    @Inject
    private ConfluenceURLConverter urlConverter;

    @Inject
    private ConfluenceXMLMacroSupport macroSupport;

    @Inject
    private ConfluenceXMLPackage confluencePackage;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Inject
    private Logger logger;

    @Inject
    private JobContext jobContext;

    @Inject
    private ConfluenceSpaceHelpers spaceHelpers;

    private final Map<String, Integer> macrosIds = new HashMap<>();

    private final Map<String, String> inlineComments = new HashMap<>();

    private final Map<String, String> spaceTargets = new HashMap<>();

    private ConfluenceIdRangeList objectIdRanges;

    private List<Long> nextIdsForObjectIdRanges;

    private int remainingPages = -1;

    private CancelableJobStatus jobStatus;

    private FilterEventParameters webPreferenceParameters;

    private static final class MaxPageCountReachedException extends ConfluenceInterruptedException
    {
        private static final long serialVersionUID = 1L;
    }

    private static final class AttachmentInfo
    {
        private final long attachmentId;
        private final long size;
        private final File contentFile;
        private final long revision;
        private final FilterEventParameters parameters;

        private AttachmentInfo(long attachmentId, long size, File contentFile, long revision,
            FilterEventParameters parameters)
        {
            this.attachmentId = attachmentId;
            this.size = size;
            this.contentFile = contentFile;
            this.revision = revision;
            this.parameters = parameters;
        }
    }

    @Override
    public void close() throws IOException
    {
        this.properties.getSource().close();
    }

    @Override
    protected void read(Object filter, ConfluenceFilter proxyFilter) throws FilterException
    {
        if (this.context instanceof DefaultConfluenceInputContext) {
            ((DefaultConfluenceInputContext) this.context).set(this.confluencePackage, this.properties);
        }

        try {
            readInternal(filter, proxyFilter);
        } finally {
            if (this.context instanceof DefaultConfluenceInputContext) {
                ((DefaultConfluenceInputContext) this.context).remove();
            }
        }
    }

    /**
     *  @return if the object should be sent and not ignored, given the object id ranges provided in the properties.
     *  It is very important that each object is only checked once.
     */
    private boolean shouldSendObject(Long id) throws FilterException
    {
        if (id == null || this.objectIdRanges == null) {
            // by default, we ignore no objects.
            return true;
        }

        if (this.nextIdsForObjectIdRanges == null || this.nextIdsForObjectIdRanges.isEmpty()) {
            if (this.objectIdRanges.pushId(id)) {
                this.nextIdsForObjectIdRanges = null;
                return true;
            }

            if (this.nextIdsForObjectIdRanges  == null) {
                // we only prepare for the next range id if it has not been already asked.
                prepareNextObjectRangeId();
            }
            return false;
        }

        if (id.equals(this.nextIdsForObjectIdRanges.get(0))) {
            this.nextIdsForObjectIdRanges.remove(0);
            return true;
        }

        return false;
    }

    private int countPages(Map<Long, List<Long>> pagesBySpace, Collection<Long> disabledSpaces)
    {
        int n = 0;
        for (Map.Entry<Long, List<Long>> pagesEntry : pagesBySpace.entrySet()) {
            if (!disabledSpaces.contains(pagesEntry.getKey())) {
                n += pagesEntry.getValue().size();
            }
        }
        return n;
    }

    private void beginSpace(EntityReference space, ConfluenceFilter proxyFilter) throws FilterException
    {
        if (space == null || !EntityType.SPACE.equals(space.getType())) {
            return;
        }

        beginSpace(space.getParent(), proxyFilter);
        proxyFilter.beginWikiSpace(space.getName(), FilterEventParameters.EMPTY);
    }

    private void endSpace(EntityReference space, ConfluenceFilter proxyFilter) throws FilterException
    {
        if (space == null || !EntityType.SPACE.equals(space.getType())) {
            return;
        }

        proxyFilter.endWikiSpace(space.getName(), FilterEventParameters.EMPTY);
        endSpace(space.getParent(), proxyFilter);
    }

    private void getJobStatus()
    {
        Job job = this.jobContext.getCurrentJob();
        if (job != null) {
            JobStatus status = job.getStatus();
            if (status instanceof CancelableJobStatus) {
                this.jobStatus = (CancelableJobStatus) status;
            }
        }
    }

    private void readInternal(Object filter, ConfluenceFilter proxyFilter) throws FilterException
    {
        ConfluenceFilteringEvent filteringEvent = preparePackage();
        if (filteringEvent == null) {
            return;
        }

        Map<Long, List<Long>> pages = this.confluencePackage.getPages();
        Map<Long, List<Long>> blogPages = this.confluencePackage.getBlogPages();

        // Only count pages if we are going to send them
        boolean willSendPages = this.properties.isContentsEnabled() || this.properties.isRightsEnabled();

        Collection<Long> disabledSpaces = filteringEvent.getDisabledSpaces();

        Collection<Long> users = this.properties.isUsersEnabled()
            ? this.confluencePackage.getInternalUsers()
            : Collections.emptyList();

        Collection<Long> groups = this.properties.isGroupsEnabled()
            ? this.confluencePackage.getGroups()
            : Collections.emptyList();

        int progressCount = countPagesToSend(willSendPages, pages, blogPages, disabledSpaces, users, groups);

        this.progress.pushLevelProgress(progressCount, this);
        try {
            sendUsersAndGroups(users, groups, proxyFilter);
            if (this.properties.isContentsEnabled()
                || this.properties.isRightsEnabled()
                || this.properties.isPageOrderEnabled()
            ) {
                sendSpaces(filter, proxyFilter, pages, blogPages, disabledSpaces);
            }
        } catch (MaxPageCountReachedException e) {
            logger.info("The maximum of pages to read has been reached.");
        } catch (ConfluenceInterruptedException e) {
            logger.warn("The job was canceled.");
        } finally {
            this.progress.popLevelProgress(this);
            observationManager.notify(new ConfluenceFilteredEvent(), this, this.confluencePackage);
            closeConfluencePackage();
            this.progress.popLevelProgress(this);
        }
    }

    private ConfluenceFilteringEvent preparePackage() throws FilterException
    {
        this.confluencePackage.setSource(this.properties.getSource());
        if (!this.properties.isExtraneousSpacesEnabled()) {
            this.confluencePackage.ignoreExtraneousSpaces();
        }

        boolean restored = false;
        String wd = this.properties.getWorkingDirectory();
        if (StringUtils.isNotEmpty(wd)) {
            restored = this.confluencePackage.restoreState(wd);
        }

        try {
            int steps = restored ? 1 : 2;
            this.progress.pushLevelProgress(steps, this);
            if (!restored) {
                this.confluencePackage.read(wd);
            }
        } catch (Exception e) {
            if (e.getCause() instanceof ConfluenceCanceledException) {
                this.logger.warn("The job was canceled", e);
                closeConfluencePackage();
            } else {
                this.logger.error(FAILED_TO_READ_PACKAGE, e);
                closeConfluencePackage();
                throw new FilterException(FAILED_TO_READ_PACKAGE, e);
            }
            return null;
        }

        if (StringUtils.isEmpty(this.properties.getConfluenceInstanceType())) {
            // Attempt to auto-detect the source Confluence instance type (cloud or server)
            this.properties.setConfluenceInstanceType(confluencePackage.getDescriptorField(DESCRIPTOR_SOURCE_FIELD));
        }

        getJobStatus();

        return handleFilteringEvent();
    }

    private ConfluenceFilteringEvent handleFilteringEvent() throws FilterException
    {
        ConfluenceFilteringEvent filteringEvent = new ConfluenceFilteringEvent();
        maybeRemoveArchivedSpaces(filteringEvent);
        computeSpaceTargets(filteringEvent);
        this.observationManager.notify(filteringEvent, this, this.confluencePackage);
        if (filteringEvent.isCanceled()) {
            closeConfluencePackage();
            return null;
        }

        this.objectIdRanges = this.properties.getObjectIdRanges();
        if (this.objectIdRanges != null) {
            prepareNextObjectRangeId();
        }

        return filteringEvent;
    }

    private int countPagesToSend(boolean willSendPages, Map<Long, List<Long>> pages, Map<Long, List<Long>> blogPages,
        Collection<Long> disabledSpaces, Collection<Long> users, Collection<Long> groups)
    {
        int pagesCount = 0;
        if (willSendPages) {
            if (properties.isNonBlogContentEnabled()) {
                pagesCount += countPages(pages, disabledSpaces);
            }

            if (properties.isBlogsEnabled()) {
                pagesCount += countPages(blogPages, disabledSpaces);
            }
        }

        this.remainingPages = this.properties.getMaxPageCount();
        if (this.remainingPages != -1) {
            pagesCount = Integer.min(this.remainingPages, pagesCount);
        }

        return pagesCount + users.size() + groups.size();
    }

    private void sendSpaces(Object filter, ConfluenceFilter proxyFilter, Map<Long, List<Long>> pages,
        Map<Long, List<Long>> blogPages, Collection<Long> disabledSpaces)
        throws FilterException, ConfluenceInterruptedException
    {
        EntityReference root = properties.getRoot();
        String wiki = null;
        FilterEventParameters wikiParameters = null;
        if (root != null) {
            wiki = root.getRoot().getType() == EntityType.WIKI ? root.getRoot().getName() : null;
        }
        if (wiki != null) {
            wikiParameters = new FilterEventParameters();
            proxyFilter.beginWiki(wiki, wikiParameters);
        }
        beginSpace(root, proxyFilter);
        try {
            Set<Long> rootSpaces = new LinkedHashSet<>();
            rootSpaces.addAll(pages.keySet());
            rootSpaces.addAll(blogPages.keySet());
            rootSpaces.removeAll(disabledSpaces);

            for (Long spaceId : rootSpaces) {
                if (spaceId == null) {
                    this.logger.error("A null space has been found. This likely means that there is a bug. Skipping.");
                }

                if (spaceId == null || !shouldSendObject(spaceId)) {
                    continue;
                }

                List<Long> regularPageIds = pages.getOrDefault(spaceId, Collections.emptyList());
                List<Long> blogPageIds = blogPages.getOrDefault(spaceId, Collections.emptyList());
                if (!regularPageIds.isEmpty() || !blogPageIds.isEmpty()) {
                    sendConfluenceRootSpace(spaceId, filter, proxyFilter, blogPageIds, root);
                }
            }
        } finally {
            endSpace(root, proxyFilter);
            if (wiki != null) {
                proxyFilter.endWiki(wiki, wikiParameters);
            }
        }
    }

    private void prepareNextObjectRangeId() throws FilterException
    {
        Long nextIdForObjectIdRanges = this.objectIdRanges.getNextId();
        if (nextIdForObjectIdRanges != null) {
            try {
                this.nextIdsForObjectIdRanges = this.confluencePackage.getAncestors(nextIdForObjectIdRanges);
            } catch (ConfigurationException e) {
                throw new FilterException(e);
            }
        }
    }

    private void maybeRemoveArchivedSpaces(ConfluenceFilteringEvent event) throws FilterException
    {
        // Yes, this is a bit hacky, I know. It would be better to not even create objects related to spaces that should
        // not be there. This is harder to do. If you find a cleaner way, don't hesitate do change this.
        if (!properties.isArchivedSpacesEnabled()) {
            try {
                for (Long spaceId : confluencePackage.getSpaces()) {
                    if (confluencePackage.isSpaceArchived(spaceId)) {
                        event.disableSpace(spaceId);
                    }
                }
            } catch (ConfigurationException e) {
                throw new FilterException("Failed to determine if the space is archived", e);
            }
        }
    }

    /**
     * @return the titles of the documents sorted by position
     * @param pages the pages to order
     */
    private Collection<String> getOrderedDocumentTitles(Iterable<Long> pages)
    {
        // TreeMap makes sure the elements are sorted by key at insertion time
        Map<Long, String> titleByPosition = new TreeMap<>();
        for (long page : pages) {
            try {
                ConfluenceProperties pageProperties = confluencePackage.getPageProperties(page, false);
                String pos = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_POSITION, "");
                if (!pos.isEmpty()) {
                    addDocumentPosition(pos, titleByPosition, pageProperties);
                }
            } catch (ConfigurationException e) {
                logger.warn("Could not get the page properties of page id [{}] to get its order", page, e);
            }
        }
        return titleByPosition.values();
    }

    private void addDocumentPosition(String pos, Map<Long, String> titleByPosition, ConfluenceProperties properties)
    {
        try {
            titleByPosition.put(Long.parseLong(pos), properties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE));
        } catch (NumberFormatException e) {
            logger.error("Could not understand position [{}], expected a long", pos, e);
        }
    }

    /**
     * Sends the ordered page titles as pinned pages.
     * WARNING: will open a WebPreferences page if necessary if not already open, it must be closed afterward
     * @param proxyFilter the filter to send to
     * @param orderedTitles the titles to send
     * @throws FilterException if something wrongs happen
     */
    private void sendPinnedPages(ConfluenceFilter proxyFilter, Collection<String> orderedTitles)
        throws FilterException
    {
        if (orderedTitles == null || orderedTitles.size() < 2) {
            // Nothing to send if there's no titles.
            // if there's only one title, it doesn't seem worth sending an order.
            return;
        }
        beginWebPreferences(proxyFilter);
        FilterEventParameters parameters = new FilterEventParameters();
        parameters.put(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, PINNED_CHILD_PAGES_CLASS);
        proxyFilter.beginWikiObject(PINNED_CHILD_PAGES_CLASS, parameters);
        try {
            List<String> pages = orderedTitles.stream().map(title ->
                confluenceConverter.toEntityName(title)
                    .replace("%", "%25")
                    .replace("/", "%2F")
                    + '/'
            ).collect(Collectors.toList());
            proxyFilter.onWikiObjectProperty("pinnedChildPages", pages, FilterEventParameters.EMPTY);
        } finally {
            proxyFilter.endWikiObject(PINNED_CHILD_PAGES_CLASS, parameters);
        }
    }

    private void sendConfluenceRootSpace(long spaceId, Object filter, ConfluenceFilter proxyFilter,
        List<Long> blogPages, EntityReference rootSpace) throws FilterException, ConfluenceInterruptedException
    {
        ConfluenceProperties spaceProperties;
        try {
            spaceProperties = this.confluencePackage.getSpaceProperties(spaceId);
        } catch (ConfigurationException e) {
            throw new FilterException("Failed to get space properties", e);
        }

        if (spaceProperties == null) {
            this.logger.error("Could not get the properties of space id=[{}]. Skipping.", spaceId);
            return;
        }
        String spaceKey = ConfluenceXMLPackage.getSpaceKey(spaceProperties);
        if (StringUtils.isEmpty(spaceKey)) {
            this.logger.error("Could not determine the key of space id=[{}]. Skipping.", spaceId);
            return;
        }
        ((DefaultConfluenceInputContext) this.context).setCurrentSpace(spaceKey);

        if (this.properties.isVerbose()) {
            this.logger.info("Sending Confluence space [{}], id=[{}]", spaceKey, spaceId);
        }

        sendSpace(spaceId, filter, proxyFilter, blogPages, spaceKey, spaceProperties, rootSpace);
    }

    private void sendSpace(long spaceId, Object filter, ConfluenceFilter proxyFilter, List<Long> blogPages,
        String spaceKey, ConfluenceProperties spaceProperties, EntityReference rootSpace)
        throws FilterException, ConfluenceInterruptedException
    {
        String spaceEntityName = this.spaceTargets.get(spaceKey);
        proxyFilter.beginWikiSpace(spaceEntityName, FilterEventParameters.EMPTY);
        try {
            Collection<ConfluenceRight> inheritedRights = null;
            ConfluenceProperties homePageProperties = null;
            Long homePageId = confluencePackage.getHomePage(spaceId);
            try {
                EntityReference spaceRef = new EntityReference(spaceEntityName, EntityType.SPACE, rootSpace);
                List<Long> orphans = confluencePackage.getOrphans(spaceId);
                if (this.properties.isContentsEnabled() || this.properties.isRightsEnabled()) {

                    if (homePageId == null) {
                        if (CollectionUtils.isEmpty(properties.getIncludedPages())) {
                            // no home page, we send a minimal one to avoid overly confusing space trees
                            // but only if we are not sending a specific list of pages
                            sendSyntheticWebHomePageListingChildren(spaceKey, spaceKey, proxyFilter);
                        }
                    } else {
                        inheritedRights = sendPage(homePageId, spaceKey, false, filter, proxyFilter, false,
                            spaceRef);
                        homePageProperties = getPageProperties(homePageId);
                    }

                    String orphanMode = properties.getOrphanMode();
                    if (!"discard".equalsIgnoreCase(orphanMode)) {
                        boolean hide = "hide".equalsIgnoreCase(orphanMode);
                        sendPages(spaceKey, false, orphans, filter, proxyFilter, hide, spaceRef);
                    }
                    sendBlogs(spaceKey, blogPages, spaceRef, filter, proxyFilter);
                }

                sendSpaceTemplates(spaceProperties, spaceKey, spaceId, filter, proxyFilter);
                if (CollectionUtils.isEmpty(properties.getIncludedPages()) && this.properties.isPageOrderEnabled()) {
                    // We don't send templates and pinned pages if we are sending a specific list of pages
                    List<Long> children = confluencePackage.getPageChildren(homePageId);
                    Collection<String> orderedTitles = getOrderedDocumentTitles(
                        IterableUtils.chainedIterable(children, blogPages));
                    sendPinnedPages(proxyFilter, orderedTitles);
                }
            } catch (ConfluenceInterruptedException e) {
                // Even if we reached the maximum page count, we want to send the space rights.
                if (shouldSendSpaceRights(homePageId)) {
                    sendSpaceRights(proxyFilter, spaceProperties, spaceKey, spaceId,
                        inheritedRights, homePageProperties);
                }
                throw e;
            }
            if (shouldSendSpaceRights(homePageId)) {
                sendSpaceRights(proxyFilter, spaceProperties, spaceKey, spaceId,
                    inheritedRights, homePageProperties);
            }
        } finally {
            endWebPreferences(proxyFilter);
            // < WikiSpace
            proxyFilter.endWikiSpace(spaceEntityName, FilterEventParameters.EMPTY);
            if (this.properties.isVerbose()) {
                this.logger.info("Finished sending Confluence space [{}], id=[{}]", spaceKey, spaceId);
            }
        }
    }

    private void computeSpaceTargets(ConfluenceFilteringEvent event) throws FilterException
    {
        for (String spaceKey : confluencePackage.getSpaceKeys(false)) {
            String target = this.confluenceConverter.toEntityName(spaceKey);
            if (shouldSpaceTargetBeRenamed(target)) {
                target = this.confluenceConverter
                    .toEntityName(
                        this.properties.getSpaceRenamingFormat().replace("${spaceKey}", spaceKey)
                    );

                target = renameIfMoreRenamingIsRequired(target);
            }
            this.spaceTargets.put(spaceKey, target);
        }
        event.setSpaceTargets(spaceTargets);
    }

    private String renameIfMoreRenamingIsRequired(String target) throws FilterException
    {
        return shouldSpaceTargetBeRenamed(target)
            ? renameIfMoreRenamingIsRequired(this.confluenceConverter.toEntityName(target + "_")) : target;
    }

    private boolean shouldSendSpaceRights(Long homePageId)
    {
        // we only send space rights if rights are enabled and we are not sending a specific list of pages, unless
        // the home page is included
        if (!this.properties.isRightsEnabled()) {
            return false;
        }

        if (CollectionUtils.isEmpty(properties.getIncludedPages())) {
            // we always send space rights if we are not working with a specific list of pages and rights are enabled
            return true;
        }

        if (homePageId == null) {
            // if we send a specific list of pages and the home page is null, we don't send space rights
            return false;
        }

        // we only send space rights if the home page is in the specific list of included pages
        return properties.isIncluded(homePageId);
    }

    private void sendSyntheticWebHomePageListingChildren(String spaceKey, String title, ConfluenceFilter proxyFilter)
        throws FilterException
    {
        FilterEventParameters documentParameters = new FilterEventParameters();
        if (this.properties.getDefaultLocale() != null) {
            documentParameters.put(WikiDocumentFilter.PARAMETER_LOCALE, this.properties.getDefaultLocale());
        }
        proxyFilter.beginWikiDocument(WEB_HOME, documentParameters);
        try {
            if (this.properties.isContentsEnabled()) {
                FilterEventParameters documentLocaleParameters = new FilterEventParameters();
                proxyFilter.beginWikiDocumentLocale(Locale.ROOT, documentLocaleParameters);
                try {
                    FilterEventParameters docRevisionParameters = new FilterEventParameters();
                    if (title != null) {
                        docRevisionParameters.put(WikiDocumentFilter.PARAMETER_TITLE, title);
                    }
                    // TODO: we may want to make this body content customizable at some point
                    docRevisionParameters.put(WikiDocumentFilter.PARAMETER_SYNTAX, Syntax.XWIKI_2_1);
                    docRevisionParameters.put(WikiDocumentFilter.PARAMETER_CONTENT, "{{children/}}");
                    proxyFilter.beginWikiDocumentRevision(ONE, docRevisionParameters);
                    if (spaceKey != null) {
                        storeConfluenceDetails(spaceKey, null, null, null, true, proxyFilter);
                    }
                    proxyFilter.endWikiDocumentRevision(ONE, docRevisionParameters);
                } finally {
                    proxyFilter.endWikiDocumentLocale(Locale.ROOT, documentLocaleParameters);
                }
            }
        } finally {
            proxyFilter.endWikiDocument(WEB_HOME, documentParameters);
        }
    }

    private void sendSpaceTemplates(ConfluenceProperties spaceProperties, String spaceKey, long spaceId, Object filter,
        ConfluenceFilter proxyFilter) throws FilterException
    {
        String templateSpaceName = this.properties.getTemplateSpaceName();
        if (StringUtils.isEmpty(templateSpaceName)) {
            return;
        }

        templateSpaceName = confluenceConverter.toEntityName(templateSpaceName);
        Collection<Object> templates = spaceProperties.getList(ConfluenceXMLPackage.KEY_SPACE_PAGE_TEMPLATES);
        if (templates.isEmpty()) {
            return;
        }

        proxyFilter.beginWikiSpace(templateSpaceName, FilterEventParameters.EMPTY);
        if (CollectionUtils.isEmpty(properties.getIncludedPages())) {
            sendSyntheticWebHomePageListingChildren(null, null, proxyFilter);
        }
        try {
            for (Object templateObject : templates) {
                long templateId = toLong(templateObject);
                if (!shouldSendObject(templateId) || !properties.isIncluded(templateId)) {
                    continue;
                }

                ConfluenceProperties templateProperties = null;
                try {
                    templateProperties = this.confluencePackage.getSpacePageTemplateProperties(spaceId, templateId,
                        false);
                } catch (ConfigurationException e) {
                    logger.error("Failed to get template properties [{}] for the space [{}]", templateId, spaceKey, e);
                }

                if (templateProperties != null) {
                    sendSpaceTemplate(filter, proxyFilter, templateProperties, templateSpaceName);
                }
            }
        } finally {
            proxyFilter.endWikiSpace(templateSpaceName, FilterEventParameters.EMPTY);
        }
    }

    private void sendSpaceTemplate(Object filter, ConfluenceFilter proxyFilter, ConfluenceProperties templateProperties,
        String templateSpaceName) throws FilterException
    {
        String title = templateProperties.getString(NAME);
        String version = templateProperties.getString("version");

        if (this.properties.isVerbose()) {
            this.logger.info(SEND_TEMPLATE_MARKER, "Sending template [{}]", title);
        }

        String validatedName = confluenceConverter.toEntityName(title);
        proxyFilter.beginWikiSpace(validatedName, FilterEventParameters.EMPTY);
        try {
            proxyFilter.beginWikiDocument(WEB_HOME, FilterEventParameters.EMPTY);
            try {
                sendSpaceTemplateDocumentLocale(filter, proxyFilter, templateProperties, version);
            } finally {
                proxyFilter.endWikiDocument(WEB_HOME, FilterEventParameters.EMPTY);
            }
        } finally {
            proxyFilter.endWikiSpace(validatedName, FilterEventParameters.EMPTY);
        }

        if (this.properties.isTemplateProvidersEnabled()) {
            String description = templateProperties.getString(DESCRIPTION, null);
            // FIXME handle collisions?
            String templateProviderName = confluenceConverter.toEntityName(title + " Template Provider");
            FilterEventParameters providerDocParameters = new FilterEventParameters();
            providerDocParameters.put(WikiDocumentFilter.PARAMETER_HIDDEN, true);
            proxyFilter.beginWikiDocument(templateProviderName, providerDocParameters);
            sendTemplateProviderObject(proxyFilter, description, title, validatedName, templateSpaceName);
            proxyFilter.endWikiDocument(templateProviderName, providerDocParameters);
        }
    }

    private void sendSpaceTemplateDocumentLocale(Object filter, ConfluenceFilter proxyFilter,
        ConfluenceProperties templateProperties, String version) throws FilterException
    {
        // TODO handle localized template migration?
        Locale locale = Locale.ROOT;
        FilterEventParameters documentLocaleParameters = getDocumentLocaleParameters(templateProperties);
        proxyFilter.beginWikiDocumentLocale(locale, documentLocaleParameters);
        try {
            FilterEventParameters revisionParameters = new FilterEventParameters();
            revisionParameters.put(WikiDocumentFilter.PARAMETER_HIDDEN, true);
            prepareRevisionMetadata(templateProperties, revisionParameters);
            beginPageRevision(false, templateProperties, filter, proxyFilter, version,
                   revisionParameters, ConfluenceXMLPackage.KEY_CONTENT);
            readPageTags(templateProperties, proxyFilter);
            proxyFilter.endWikiDocumentRevision(version, revisionParameters);
        } finally {
            proxyFilter.endWikiDocumentLocale(locale, documentLocaleParameters);
        }
    }

    private void checkCanceled() throws ConfluenceCanceledException
    {
        if (jobStatus != null && jobStatus.isCanceled()) {
            throw new ConfluenceCanceledException();
        }
    }

    private Collection<ConfluenceRight> sendPage(long pageId, String spaceKey, boolean blog, Object filter,
        ConfluenceFilter proxyFilter, boolean hide, EntityReference spaceRef) throws ConfluenceInterruptedException
    {
        if (this.remainingPages == 0) {
            throw new MaxPageCountReachedException();
        }

        checkCanceled();

        Collection<ConfluenceRight> inheritedRights = null;

        ((DefaultConfluenceInputContext) this.context).setCurrentPage(pageId);
        try {
            inheritedRights = readPage(pageId, spaceKey, blog, filter, proxyFilter, hide, spaceRef);
        } catch (MaxPageCountReachedException e) {
            // ignore
        } catch (ConfluenceCanceledException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to filter the page with id [{}]", createPageIdentifier(pageId, spaceKey), e);
        }

        return inheritedRights;
    }

    private void sendBlogs(String spaceKey, List<Long> blogPages, EntityReference spaceRef, Object filter,
        ConfluenceFilter proxyFilter) throws FilterException, ConfluenceInterruptedException
    {
        if (!this.properties.isBlogsEnabled() || blogPages == null || blogPages.isEmpty()) {
            return;
        }

        String blogSpaceKey = confluenceConverter.toEntityName(this.properties.getBlogSpaceName());
        EntityReference blogSpaceRef = new EntityReference(blogSpaceKey, EntityType.SPACE, spaceRef);

        // > WikiSpace
        proxyFilter.beginWikiSpace(blogSpaceKey, FilterEventParameters.EMPTY);
        try {
            if (CollectionUtils.isEmpty(this.properties.getIncludedPages()) && this.properties.isPageOrderEnabled()) {
                // we only send the pinned pages and the pinned pages, the WebPreferences document and the blog
                // descriptor if we are not sending a specific list of pages.
                Collection<String> orderedTitles = getOrderedDocumentTitles(blogPages);
                sendPinnedPages(proxyFilter, orderedTitles);
                endWebPreferences(proxyFilter);
                addBlogDescriptorPage(proxyFilter);
            }
            // Blog post pages
            sendPages(spaceKey, true, blogPages, filter, proxyFilter, false, blogSpaceRef);
        } finally {
            proxyFilter.endWikiSpace(blogSpaceKey, FilterEventParameters.EMPTY);
        }
    }

    private void sendPages(String spaceKey, boolean blog, List<Long> pages, Object filter, ConfluenceFilter proxyFilter,
        boolean hide, EntityReference spaceRef) throws ConfluenceInterruptedException
    {
        Long homePageId = confluencePackage.getHomePage(confluencePackage.getSpaceId(spaceKey));
        for (Long pageId : pages) {
            if (Objects.equals(pageId, homePageId)) {
                logger.info("The home page (id: [{}]) of space [{}] is a child of another page, "
                        + "not sending it a second time", pageId, spaceKey);
                continue;
            }
            sendPage(pageId, spaceKey, blog, filter, proxyFilter, hide, spaceRef);
        }
    }

    private void sendSpaceRights(ConfluenceFilter proxyFilter, ConfluenceProperties spaceProperties,
        String spaceKey, long spaceId, Collection<ConfluenceRight> inheritedRights,
        ConfluenceProperties homePageProperties) throws FilterException
    {
        Collection<Object> spacePermissions = spaceProperties.getList(ConfluenceXMLPackage.KEY_SPACE_PERMISSIONS);
        if (spacePermissions.isEmpty()) {
            return;
        }

        // This lets us avoid duplicate XWiki right objects. For instance, REMOVEPAGE and REMOVEBLOG are both
        // mapped to DELETE, and EDITPAGE and EDITBLOG are both mapped to EDIT. In each of these cases,
        // if both rights are set, we need to deduplicate.
        Set<String> addedRights = new HashSet<>();

        for (Object spacePermissionObject : spacePermissions) {
            long spacePermissionId = toLong(spacePermissionObject);

            if (!shouldSendObject(spacePermissionId)) {
                continue;
            }

            ConfluenceProperties spacePermissionProperties = null;
            try {
                spacePermissionProperties = this.confluencePackage.getSpacePermissionProperties(spaceId,
                    spacePermissionId);
            } catch (ConfigurationException e) {
                logger.error("Failed to get space permission properties [{}] for the space [{}]",
                    spacePermissionId, spaceKey, e);
            }

            if (spacePermissionProperties != null) {
                sendPageRight(proxyFilter, spaceKey, spacePermissionProperties, spacePermissionId, addedRights);
            }
        }

        if (inheritedRights != null) {
            for (ConfluenceRight confluenceRight : inheritedRights) {
                sendInheritedPageRight(homePageProperties, proxyFilter, confluenceRight);
            }
        }
    }

    private void sendPageRight(ConfluenceFilter proxyFilter, String spaceKey,
        ConfluenceProperties spacePermissionProperties, long spacePermissionId, Set<String> addedRights)
        throws FilterException
    {
        ConfluenceRight confluenceRight = getConfluenceRightData(spacePermissionProperties);

        SpacePermissionType type = null;
        try {
            type = SpacePermissionType.valueOf(confluenceRight.type);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to understand space permission type [{}] for the space [{}], "
                    + "permission id [{}].", confluenceRight.type, spaceKey, spacePermissionId);
        }

        if (type != null) {
            Right right = type.toXWikiRight();
            if (right == null) {
                this.logger.warn("Unknown permission type for space permission id [{}].", spacePermissionId);
            } else if (right != Right.ILLEGAL) {
                sendSpaceRight(proxyFilter, right, confluenceRight, addedRights);
            }
        }
    }

    private void sendSpaceRight(ConfluenceFilter proxyFilter, Right right,
        ConfluenceRight confluenceRight, Set<String> addedRights) throws FilterException
    {
        String group = confluenceRight.group;
        if (group != null && !group.isEmpty()) {
            String groupRightString = "g:" + group + ':' + right;
            if (addedRights.contains(groupRightString)) {
                group = "";
            } else {
                addedRights.add(groupRightString);
            }
        } else {
            group = "";
        }

        String users = confluenceRight.users;
        if (users != null && !users.isEmpty()) {
            String userRightString = "u:" + users + ':' + right;
            if (addedRights.contains(userRightString)) {
                users = "";
            } else {
                addedRights.add(userRightString);
            }
        } else {
            users = "";
        }

        if (!(users.isEmpty() && group.isEmpty())) {
            beginWebPreferences(proxyFilter);
            sendRight(proxyFilter, group, right, users, true);
        }
    }

    private void beginWebPreferences(ConfluenceFilter proxyFilter) throws FilterException
    {
        if (isInWebPreferences()) {
            return;
        }
        FilterEventParameters webPreferencesParams = new FilterEventParameters();
        webPreferencesParams.put(WikiDocumentFilter.PARAMETER_HIDDEN, true);
        proxyFilter.beginWikiDocument(WEB_PREFERENCES, webPreferencesParams);
        try {
            FilterEventParameters prefParameters = new FilterEventParameters();
            prefParameters.put(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, XWIKI_PREFERENCES_CLASS);
            proxyFilter.beginWikiObject(XWIKI_PREFERENCES_CLASS, prefParameters);
            proxyFilter.endWikiObject(XWIKI_PREFERENCES_CLASS, prefParameters);
            webPreferenceParameters = webPreferencesParams;
        } catch (FilterException e) {
            proxyFilter.endWikiDocument(WEB_PREFERENCES, webPreferencesParams);
            throw e;
        }
    }

    private boolean isInWebPreferences()
    {
        return webPreferenceParameters != null;
    }

    private void endWebPreferences(ConfluenceFilter proxyFilter) throws FilterException
    {
        if (isInWebPreferences()) {
            proxyFilter.endWikiDocument(WEB_PREFERENCES, webPreferenceParameters);
            webPreferenceParameters = null;
        }
    }

    private ConfluenceRight getConfluenceRightData(ConfluenceProperties permProperties)
    {
        String type = permProperties.getString(ConfluenceXMLPackage.KEY_PERMISSION_TYPE, "");
        String group = getConfluenceGroupRightData(permProperties);
        String users = getConfluenceUserRightData(permProperties);
        if (StringUtils.isEmpty(group) && StringUtils.isEmpty(users)) {
            users = confluenceConverter.getGuestUser();
        }

        return new ConfluenceRight(type, group, users);
    }

    private String getConfluenceGroupRightData(ConfluenceProperties permProperties)
    {
        String groupStr = permProperties.getString(ConfluenceXMLPackage.KEY_SPACEPERMISSION_GROUP, null);
        if (groupStr == null || groupStr.isEmpty()) {
            groupStr = permProperties.getString(ConfluenceXMLPackage.KEY_CONTENTPERMISSION_GROUP, null);
        }
        return (groupStr == null || groupStr.isEmpty())
            ? ""
            : (confluenceConverter.toGroupReference(groupStr));
    }

    private String getConfluenceUserRightData(ConfluenceProperties permProperties)
    {
        String users = "";

        String allUsersSubject = permProperties.getString(ConfluenceXMLPackage.KEY_PERMISSION_ALLUSERSSUBJECT, null);
        if ("anonymous-users".equals(allUsersSubject)) {
            users = confluenceConverter.getGuestUser();
        }

        String userName = permProperties.getString(ConfluenceXMLPackage.KEY_SPACEPERMISSION_USERNAME, null);
        if (StringUtils.isEmpty(userName)) {
            String userKey = permProperties.getString(ConfluenceXMLPackage.KEY_PERMISSION_USERSUBJECT, null);
            userName = resolveUserName(userKey);
        }

        if (!StringUtils.isEmpty(userName)) {
            users = (users.isEmpty() ? "" : users + ",") + confluenceConverter.toUserReference(userName);
        }
        return users;
    }

    private String resolveUserName(String userKey)
    {
        if (StringUtils.isEmpty(userKey)) {
            return "";
        }

        String userName = confluencePackage.resolveUserName(userKey, null);
        if (StringUtils.isEmpty(userName)) {
            logger.error("Failed to resolve user [{}]", userKey);
            return userKey;
        }
        return userName;
    }

    private static class ConfluenceRight
    {
        public final String type;
        public final String group;
        public final String users;

        ConfluenceRight(String type, String group, String users)
        {
            this.type = type;
            this.group = group;
            this.users = users;
        }
    }

    private void sendInheritedPageRight(ConfluenceProperties pageProperties, ConfluenceFilter proxyFilter,
        ConfluenceRight confluenceRight) throws FilterException
    {
        if (confluenceRight.users.isEmpty() && confluenceRight.group.isEmpty()) {
            return;
        }

        ContentPermissionType type = getContentPermissionType(pageProperties, confluenceRight, null);
        if (type == null) {
            return;
        }
        Right right;
        switch (type) {
            case VIEW:
                right = Right.VIEW;
                break;
            case EDIT:
                right = Right.EDIT;
                break;
            default:
                return;
        }
        sendRight(proxyFilter, confluenceRight.group, right, confluenceRight.users, true);
    }

    private static void sendRight(ConfluenceFilter proxyFilter, String group, Right right, String users, boolean global)
        throws FilterException
    {
        FilterEventParameters rightParameters = new FilterEventParameters();
        // Page report object
        String rightClassName = global ? XWIKIGLOBALRIGHTS_CLASSNAME : XWIKIRIGHTS_CLASSNAME;
        rightParameters.put(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, rightClassName);
        proxyFilter.beginWikiObject(rightClassName, rightParameters);
        try {
            proxyFilter.onWikiObjectProperty("allow", ONE, FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty("groups", group, FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty("levels", right.getName(), FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty("users", users, FilterEventParameters.EMPTY);
        } finally {
            proxyFilter.endWikiObject(rightClassName, rightParameters);
        }
    }

    private void sendTemplateProviderObject(ConfluenceFilter proxyFilter, String description, String name,
        String template, String templateSpaceName) throws FilterException
    {
        String spaceRef = confluenceConverter.convertSpaceReference("");
        String templateRef = spaceRef + '.' + templateSpaceName + '.' + template + ".WebHome";

        FilterEventParameters parameters = new FilterEventParameters();
        // Page report object
        parameters.put(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, TEMPLATE_PROVIDER_CLASS);
        proxyFilter.beginWikiObject(TEMPLATE_PROVIDER_CLASS, parameters);
        try {
            proxyFilter.onWikiObjectProperty(DESCRIPTION, description, FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty(NAME, name, FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty("template", templateRef, FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty("visibilityRestrictions", spaceRef, FilterEventParameters.EMPTY);
        } finally {
            proxyFilter.endWikiObject(TEMPLATE_PROVIDER_CLASS, parameters);
        }
    }

    private Map<String, Object> createPageIdentifier(Long pageId, String spaceKey)
    {
        PageIdentifier page = new PageIdentifier(pageId);
        page.setPageId(pageId);
        page.setSpaceKey(spaceKey);
        try {
            populatePageIdentifier(pageId, getPageProperties(pageId), page);
        } catch (FilterException e) {
            this.logger.error(PAGE_IDENTIFIER_ERROR, pageId, e);
        }
        return page.getMap();
    }

    private void populatePageIdentifier(Long pageId, ConfluenceProperties pageProperties, PageIdentifier pageIdentifier)
    {
        if (pageProperties == null) {
            return;
        }

        String title = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE, null);
        if (StringUtils.isEmpty(title)) {
            title = pageProperties.getString(NAME, null);
        }
        pageIdentifier.setPageTitle(title);
        pageIdentifier.setPageRevision(pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION));
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_PARENT)) {
            Long parentId = pageProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_PARENT);
            pageIdentifier.setParentId(parentId);
            try {
                ConfluenceProperties parentPageProperties = getPageProperties(parentId);
                if (parentPageProperties != null) {
                    pageIdentifier.setParentTitle(parentPageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE));
                }
            } catch (FilterException e) {
                this.logger.error("Failed to get the parent title when building the page identifier for page id [{}]",
                    pageId, e);
            }
        }
        Long originalVersion = pageProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_ORIGINAL_VERSION, null);
        if (originalVersion != null) {
            pageIdentifier.setOriginalVersion(originalVersion);
        }
    }

    private Map<String, Object> createObjectIdentifier(ConfluenceProperties parentProperties)
    {
        Map<String, Object> identifier;

        // Check if the parent is a page
        if (this.confluencePackage.isPage(parentProperties)) {
            // The object is a page
            identifier = createPageIdentifier(parentProperties);
        } else {
            identifier = new TreeMap<>();

            long id = parentProperties.getLong(ConfluenceXMLPackage.KEY_ID);
            identifier.put(ConfluenceXMLPackage.KEY_ID, id);
            String className = this.confluencePackage.getClass(parentProperties);
            if (className != null) {
                identifier.put(ConfluenceXMLPackage.KEY_CLASS, className);
            }

            Long spaceId = parentProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_SPACE, null);
            if (spaceId != null) {
                try {
                    identifier.put("spaceKey", this.confluencePackage.getSpaceKey(spaceId));
                } catch (ConfigurationException e) {
                    this.logger.error(ERROR_SPACE_KEY_RESOLUTION, spaceId, id, e);
                }
            }

            return identifier;
        }

        return identifier;
    }

    private Map<String, Object> createPageIdentifier(ConfluenceProperties pageProperties)
    {
        String spaceKey = null;
        Long spaceId = pageProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_SPACE, null);
        if (spaceId != null) {
            try {
                spaceKey = this.confluencePackage.getSpaceKey(spaceId);
            } catch (ConfigurationException e) {
                this.logger.error(PAGE_IDENTIFIER_ERROR, pageProperties.getLong(ConfluenceXMLPackage.KEY_ID), e);
            }
        }

        return createPageIdentifier(pageProperties, spaceKey);
    }

    private Map<String, Object> createPageIdentifier(ConfluenceProperties pageProperties, String spaceKey)
    {
        Long pageId = pageProperties.getLong(ConfluenceXMLPackage.KEY_ID);
        PageIdentifier pageIdentifier = new PageIdentifier(pageId);
        pageIdentifier.setSpaceKey(spaceKey);
        populatePageIdentifier(pageId, pageProperties, pageIdentifier);
        return pageIdentifier.getMap();
    }

    private void closeConfluencePackage() throws FilterException
    {
        if ("NO".equals(this.properties.getCleanup())) {
            return;
        }

        try {
            this.confluencePackage.close("ASYNC".equals(this.properties.getCleanup()));
        } catch (IOException e) {
            throw new FilterException("Failed to close package", e);
        }
    }

    private void sendUsersAndGroups(Collection<Long> users, Collection<Long> groups, ConfluenceFilter proxyFilter)
        throws FilterException, ConfluenceCanceledException
    {
        if ((users == null || users.isEmpty()) && (groups == null || groups.isEmpty())) {
            return;
        }

        // Switch the wiki if a specific one is forced
        if (this.properties.getUsersWiki() != null) {
            proxyFilter.beginWiki(this.properties.getUsersWiki(), FilterEventParameters.EMPTY);
        }

        if (users != null) {
            sendUsers(users, proxyFilter);
        }

        if (groups != null) {
            sendGroups(groups, proxyFilter);
        }

        // Get back to default wiki
        if (this.properties.getUsersWiki() != null) {
            proxyFilter.endWiki(this.properties.getUsersWiki(), FilterEventParameters.EMPTY);
        }
    }

    private void sendGroups(Collection<Long> groupIds, ConfluenceFilter proxyFilter)
        throws FilterException, ConfluenceCanceledException
    {
        // Group groups by XWiki group name. There can be several Confluence groups mapping to a unique XWiki group.
        Map<String, Collection<ConfluenceProperties>> groupsByXWikiName = getGroupsByXWikiName(groupIds);

        // Loop over the XWiki groups
        for (Map.Entry<String, Collection<ConfluenceProperties>> groupEntry: groupsByXWikiName.entrySet()) {
            checkCanceled();
            String groupName = groupEntry.getKey();
            if ("XWikiAllGroup".equals(groupName)) {
                continue;
            }

            this.progress.startStep(this);

            FilterEventParameters groupParameters = new FilterEventParameters();

            // We arbitrarily take the creation and revision date of the first Confluence group mapped to this
            // XWiki group.
            Collection<ConfluenceProperties> groups = groupEntry.getValue();
            try {
                ConfluenceProperties firstGroupProperties = groups.iterator().next();
                groupParameters.put(GroupFilter.PARAMETER_REVISION_DATE,
                    this.confluencePackage.getDate(firstGroupProperties, ConfluenceXMLPackage.KEY_GROUP_REVISION_DATE));
                groupParameters.put(GroupFilter.PARAMETER_CREATION_DATE,
                    this.confluencePackage.getDate(firstGroupProperties, ConfluenceXMLPackage.KEY_GROUP_CREATION_DATE));
            } catch (Exception e) {
                if (this.properties.isVerbose()) {
                    this.logger.error("Failed to parse the group date", e);
                }
            }

            if (properties.isVerbose()) {
                logger.info("Sending group [{}]", groupName);
            }

            proxyFilter.beginGroupContainer(groupName, groupParameters);
            try {
                // We add members of all the Confluence groups mapped to this XWiki group to the XWiki group.
                Collection<String> alreadyAddedMembers = new HashSet<>();
                for (ConfluenceProperties groupProperties : groups) {
                    sendUserMembers(proxyFilter, groupProperties, alreadyAddedMembers);
                    sendGroupMembers(proxyFilter, groupProperties, alreadyAddedMembers);
                }
            } finally {
                proxyFilter.endGroupContainer(groupName, groupParameters);
            }

            this.progress.endStep(this);
        }
    }

    private void sendGroupMembers(ConfluenceFilter proxyFilter, ConfluenceProperties groupProperties,
        Collection<String> alreadyAddedMembers) throws ConfluenceCanceledException
    {
        if (groupProperties.containsKey(ConfluenceXMLPackage.KEY_GROUP_MEMBERGROUPS)) {
            List<Long> groupMembers = this.confluencePackage.getLongList(groupProperties,
                ConfluenceXMLPackage.KEY_GROUP_MEMBERGROUPS);
            for (Long memberInt : groupMembers) {
                checkCanceled();
                FilterEventParameters memberParameters = new FilterEventParameters();

                try {
                    String memberId = confluenceConverter.toGroupReference(
                        this.confluencePackage.getGroupProperties(memberInt)
                            .getString(ConfluenceXMLPackage.KEY_GROUP_NAME, String.valueOf(memberInt)));

                    if (!alreadyAddedMembers.contains(memberId)) {
                        proxyFilter.onGroupMemberGroup(memberId, memberParameters);
                        alreadyAddedMembers.add(memberId);
                    }
                } catch (Exception e) {
                    this.logger.error(FAILED_TO_GET_GROUP_PROPERTIES, e);
                }
            }
        }
    }

    private void sendUserMembers(ConfluenceFilter proxyFilter, ConfluenceProperties groupProperties,
        Collection<String> alreadyAddedMembers) throws ConfluenceCanceledException
    {
        if (groupProperties.containsKey(ConfluenceXMLPackage.KEY_GROUP_MEMBERUSERS)) {
            List<Long> groupMembers =
                this.confluencePackage.getLongList(groupProperties, ConfluenceXMLPackage.KEY_GROUP_MEMBERUSERS);
            for (Long memberInt : groupMembers) {
                checkCanceled();
                FilterEventParameters memberParameters = new FilterEventParameters();

                try {
                    String userName = confluenceConverter.toUserReferenceName(
                        this.confluencePackage.getInternalUserProperties(memberInt)
                            .getString(ConfluenceXMLPackage.KEY_USER_NAME, String.valueOf(memberInt)));

                    if (!alreadyAddedMembers.contains(userName)) {
                        proxyFilter.onGroupMemberGroup(userName, memberParameters);
                        alreadyAddedMembers.add(userName);
                    }
                } catch (Exception e) {
                    this.logger.error(FAILED_TO_GET_USER_PROPERTIES, e);
                }
            }
        }
    }

    private Map<String, Collection<ConfluenceProperties>> getGroupsByXWikiName(Collection<Long> groups)
        throws FilterException
    {
        Map<String, Collection<ConfluenceProperties>> groupsByXWikiName = new HashMap<>();
        int i = 0;
        for (long groupId : groups) {
            this.progress.startStep(this);
            if (properties.isVerbose()) {
                logger.info("Reading group [{}] ({}/{})", groupId, ++i, groups.size());
            }
            ConfluenceProperties groupProperties;
            try {
                groupProperties = this.confluencePackage.getGroupProperties(groupId);
            } catch (ConfigurationException e) {
                throw new FilterException(FAILED_TO_GET_GROUP_PROPERTIES, e);
            }

            String groupName = confluenceConverter.toGroupReferenceName(
                groupProperties.getString(ConfluenceXMLPackage.KEY_GROUP_NAME, String.valueOf(groupId)));

            if (!groupName.isEmpty()) {
                Collection<ConfluenceProperties> l = groupsByXWikiName.getOrDefault(groupName, new ArrayList<>());
                l.add(groupProperties);
                groupsByXWikiName.put(groupName, l);
            }
        }
        return groupsByXWikiName;
    }

    private void sendUsers(Collection<Long> users, ConfluenceFilter proxyFilter)
        throws FilterException, ConfluenceCanceledException
    {
        for (Long userId : users) {
            checkCanceled();
            this.progress.startStep(this);

            if (shouldSendObject(userId)) {
                sendUser(proxyFilter, userId);
            }

            this.progress.endStep(this);
        }
    }

    private void sendUser(ConfluenceFilter proxyFilter, Long userId) throws FilterException
    {
        ConfluenceProperties userProperties;
        try {
            userProperties = this.confluencePackage.getInternalUserProperties(userId);
        } catch (ConfigurationException e) {
            throw new FilterException(FAILED_TO_GET_USER_PROPERTIES, e);
        }

        String userName = confluenceConverter.toUserReferenceName(
            userProperties.getString(ConfluenceXMLPackage.KEY_USER_NAME, String.valueOf(userId)));

        if (this.properties.isVerbose()) {
            this.logger.info("Sending user [{}] (id = [{}])", userName, userId);
        }

        FilterEventParameters userParameters = new FilterEventParameters();

        userParameters.put(UserFilter.PARAMETER_FIRSTNAME,
            userProperties.getString(ConfluenceXMLPackage.KEY_USER_FIRSTNAME, "").trim());
        userParameters.put(UserFilter.PARAMETER_LASTNAME,
            userProperties.getString(ConfluenceXMLPackage.KEY_USER_LASTNAME, "").trim());
        userParameters.put(UserFilter.PARAMETER_EMAIL,
            userProperties.getString(ConfluenceXMLPackage.KEY_USER_EMAIL, "").trim());
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
    }

    private String getSpaceTitle(Long spaceId)
    {
        try {
            ConfluenceProperties spaceProperties = confluencePackage.getSpaceProperties(spaceId);
            if (spaceProperties != null) {
                return spaceProperties.getString(ConfluenceXMLPackage.KEY_SPACE_NAME, null);
            }
        } catch (ConfigurationException e) {
            this.logger.warn("Could not get the title of space id=[{}]", spaceId, e);
        }

        return null;
    }

    private ConfluenceProperties readPageGetPageProperties(long pageId, String spaceKey) throws FilterException
    {
        if (!shouldSendObject(pageId)) {
            return null;
        }

        ConfluenceProperties pageProperties = getPageProperties(pageId);

        if (pageProperties == null) {
            this.logger.error("Can't find page with id [{}]", createPageIdentifier(pageId, spaceKey));
            return null;
        }

        // Skip archived pages
        String status = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_CONTENT_STATUS);
        if ("archived".equals(status) && !this.properties.isArchivedDocumentsEnabled()) {
            return null;
        }

        return pageProperties;
    }

    private Collection<ConfluenceRight> readPage(long pageId, String spaceKey, boolean blog, Object filter,
        ConfluenceFilter proxyFilter, boolean hide, EntityReference spaceRef)
        throws FilterException, ConfluenceInterruptedException
    {
        ConfluenceProperties pageProperties = readPageGetPageProperties(pageId, spaceKey);
        if (pageProperties == null) {
            emptyStep();
            return Collections.emptyList();
        }

        String title = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE);

        boolean isHomePage = pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_HOMEPAGE);
        String documentName = isHomePage ? WEB_HOME : title;

        // Skip pages with empty title
        if (StringUtils.isEmpty(documentName)) {
            this.logger.warn("Found a page without a name or title (id={}). Skipping it.",
                createPageIdentifier(pageId, spaceKey));

            emptyStep();
            return Collections.emptyList();
        }

        FilterEventParameters documentParameters = new FilterEventParameters();
        if (this.properties.getDefaultLocale() != null) {
            documentParameters.put(WikiDocumentFilter.PARAMETER_LOCALE, this.properties.getDefaultLocale());
        }

        String spaceName = confluenceConverter.toEntityName(title);

        List<Long> children = blog ? Collections.emptyList() : confluencePackage.getPageChildren(pageId);
        if (blog) {
            // Apply the standard entity name validator
            documentName = confluenceConverter.toEntityName(documentName);
        } else {
            if (!isHomePage) {
                proxyFilter.beginWikiSpace(spaceName, FilterEventParameters.EMPTY);
            }
            documentName = WEB_HOME;
        }

        Collection<ConfluenceRight> homePageInheritedRights = null;

        EntityReference docRef = new EntityReference(documentName, EntityType.DOCUMENT, spaceRef);
        try {
            Collection<ConfluenceRight> inheritedRights = sendTerminalDoc(blog, filter, proxyFilter, docRef,
                documentParameters, pageProperties, spaceKey, isHomePage, children, hide, pageId);

            if (isHomePage) {
                // We only send inherited rights of the home page so they are added to the space's WebPreference page
                homePageInheritedRights = inheritedRights;
            }

            if (!children.isEmpty()) {
                sendPages(spaceKey, false, children, filter, proxyFilter, hide, spaceRef);
            }
        } finally {
            if (!blog && !isHomePage) {
                proxyFilter.endWikiSpace(spaceName, FilterEventParameters.EMPTY);
            }
        }

        return homePageInheritedRights;
    }

    private void emptyStep()
    {
        this.progress.startStep(this);
        this.progress.endStep(this);
    }

    private Collection<ConfluenceRight> sendTerminalDoc(boolean blog, Object filter, ConfluenceFilter proxyFilter,
        EntityReference docRef, FilterEventParameters documentParameters, ConfluenceProperties pageProperties,
        String spaceKey, boolean isHomePage, List<Long> children, boolean hide, long pageId)
            throws FilterException, ConfluenceCanceledException
    {
        Collection<ConfluenceRight> inheritedRights = blog ? null : new ArrayList<>();
        this.progress.startStep(this);
        try {
            if (properties.isIncluded(pageId)) {
                String documentName = docRef.getName();
                proxyFilter.beginWikiDocument(documentName, documentParameters);

                try {
                    if (this.properties.isContentsEnabled() || this.properties.isRightsEnabled()) {
                        sendRevisions(blog, filter, proxyFilter, pageProperties, spaceKey, inheritedRights, hide,
                            Locale.ROOT, docRef);
                    }
                } finally {
                    sendTranslations(blog, filter, proxyFilter, pageProperties, spaceKey, hide, docRef,
                        inheritedRights);
                    proxyFilter.endWikiDocument(documentName, documentParameters);

                    if (!blog && !isHomePage) {
                        sendWebPreferences(proxyFilter, pageProperties, children, inheritedRights);
                    }

                    if (this.remainingPages > 0) {
                        this.remainingPages--;
                    }
                }
            }
        } finally {
            this.progress.endStep(this);
        }

        return isHomePage ? inheritedRights : null;
    }

    private void maybeLogMacroUsage(ConfluenceProperties pageProperties, Locale locale)
    {
        if (!macrosIds.isEmpty() && properties.isVerbose()) {
            logger.info(ConfluenceFilter.LOG_MACROS_FOUND,
                "The following macros [{}] were found on page [{}], locale [{}].",
                macrosIds, createPageIdentifier(pageProperties), locale);
        }
    }

    private void sendTranslations(boolean blog, Object filter, ConfluenceFilter proxyFilter,
        ConfluenceProperties pageProperties, String spaceKey, boolean hide, EntityReference docRef,
        Collection<ConfluenceRight> inheritedRights) throws FilterException, ConfluenceCanceledException
    {
        Collection<Locale> usedLocales = context.getCurrentlyUsedLocales();
        if (!CollectionUtils.isEmpty(usedLocales)) {
            for (Locale locale : usedLocales) {
                if (locale.equals(context.getDefaultLocale())) {
                    // TODO should we use Locale.filter here instead of .equals?
                    continue;
                }
                context.setCurrentLocale(locale);
                sendRevisions(blog, filter, proxyFilter, pageProperties, spaceKey, inheritedRights, hide, locale,
                    docRef);
            }
        }
    }

    private void sendWebPreferences(ConfluenceFilter proxyFilter, ConfluenceProperties pageProperties,
        List<Long> children, Collection<ConfluenceRight> inheritedRights) throws FilterException
    {
        try {
            if (inheritedRights != null && !inheritedRights.isEmpty()) {
                // inherited rights from the home page are put in the space WebPreferences page
                beginWebPreferences(proxyFilter);
                for (ConfluenceRight right : inheritedRights) {
                    sendInheritedPageRight(pageProperties, proxyFilter, right);
                }
            }

            if (this.properties.isPageOrderEnabled()) {
                Collection<String> orderedTitles = getOrderedDocumentTitles(children);
                sendPinnedPages(proxyFilter, orderedTitles);
            }
        } finally {
            endWebPreferences(proxyFilter);
        }
    }

    private void sendRevisions(boolean blog, Object filter, ConfluenceFilter proxyFilter,
        ConfluenceProperties pageProperties, String spaceKey, Collection<ConfluenceRight> inheritedRights,
        boolean hide, Locale locale, EntityReference docRef)
        throws FilterException, ConfluenceCanceledException
    {
        FilterEventParameters documentLocaleParameters = getDocumentLocaleParameters(pageProperties);
        proxyFilter.beginWikiDocumentLocale(locale, documentLocaleParameters);

        try {
            if (properties.isHistoryEnabled() && pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISIONS)) {
                Map<Long, ConfluenceProperties> revisionsById;
                try {
                    revisionsById = confluencePackage.getRevisionsById(pageProperties, false, false);
                } catch (ConfigurationException e) {
                    logger.error("Failed to get revisions of page [{}]. This should not happen.",
                        createPageIdentifier(pageProperties), e);
                    return;
                }

                boolean buggyVersions = areThereBuggyVersions(pageProperties, revisionsById);

                Iterable<Map.Entry<Long, ConfluenceProperties>> sortedRevisionEntries = revisionsById
                    .entrySet()
                    .stream()
                    .sorted(buggyVersions
                        ? getDateComparator(ConfluenceXMLPackage.KEY_PAGE_REVISION_DATE, REVISION, null)
                        : getVersionComparator(pageProperties))
                    ::iterator;

                int version = 0;
                for (Map.Entry<Long, ConfluenceProperties> entry : sortedRevisionEntries) {
                    Long revisionId = entry.getKey();
                    ConfluenceProperties revisionProperties = entry.getValue();
                    if (buggyVersions) {
                        revisionProperties.setProperty(ConfluenceXMLPackage.KEY_PAGE_REVISION,
                            Integer.toString(++version));
                    }
                    try {
                        if (shouldSendObject(revisionId)) {
                            readPageRevision(revisionProperties, blog, Collections.emptyMap(), filter, proxyFilter,
                                spaceKey, inheritedRights, hide, locale, docRef);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to filter the page revision with id [{}]",
                            createPageIdentifier(revisionId, spaceKey), e);
                    }
                    checkCanceled();
                }
            }

            // Current version
            // Note: no need to check whether the object should be sent. Indeed, this is already checked by an upper
            // function
            Map<String, List<AttachmentInfo>> attachments = getAttachments(pageProperties);
            readPageRevision(pageProperties, blog, attachments, filter, proxyFilter, spaceKey,
                inheritedRights, hide, locale, docRef);
            maybeLogMacroUsage(pageProperties, locale);
        } finally {
            proxyFilter.endWikiDocumentLocale(locale, documentLocaleParameters);
        }
    }

    private boolean areThereBuggyVersions(ConfluenceProperties pageProperties,
        Map<Long, ConfluenceProperties> revisionsById)
    {
        Map<String, Long> knownVersions = new HashMap<>(revisionsById.size());
        for (Map.Entry<Long, ConfluenceProperties> entry : new ArrayList<>(revisionsById.entrySet())) {
            Long revisionId = entry.getKey();
            ConfluenceProperties revision = entry.getValue();
            String version = revision.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION, "");
            Long existing = knownVersions.get(version);
            final long dropped;
            final long kept;
            if (existing == null) {
                kept = revisionId;
            } else {
                // We have duplicate versions. The following lines check that they have the same date. if so, we drop
                // the lowest id. From what we saw in Confluence packages having this, objects are duplicated but
                // identical. If not, we consider the history buggy and revert to using dates.
                Date dExisting = getDate(revisionsById.get(existing), existing, REVISION,
                    ConfluenceXMLPackage.KEY_PAGE_REVISION_DATE, pageProperties);
                Date dCurrent = getDate(revision, revisionId, REVISION, ConfluenceXMLPackage.KEY_PAGE_REVISION_DATE,
                    pageProperties);
                if (Objects.equals(dCurrent, dExisting)) {
                    // Both revisions have the same modification date, we keep the highest id
                    dropped = Math.min(revisionId, existing);
                    kept = Math.max(revisionId, existing);
                    this.logger.info("Duplicate version ([{}]) for page [{}]. Dropping id [{}] in favor of [{}]",
                        version, createPageIdentifier(pageProperties), dropped, kept);
                    revisionsById.remove(dropped);
                } else {
                    this.logger.error("Two revisions have the same version ([{}]). Will sort revisions by date as a"
                            + " fallback for page [{}] and rewrite the versions. Please double-check its history",
                        version, createPageIdentifier(pageProperties));
                    return true;
                }
            }

            if (!VERSION_PATTERN.matcher(version).matches()) {
                this.logger.error("Failed to get the version for page revision with id [{}]. Will use dates to sort "
                    + "revisions as a fallback for page [{}] and rewrite the versions. Please double-check its history",
                    revisionId, createPageIdentifier(pageProperties));
                return true;
            }

            knownVersions.put(version, kept);
        }
        return false;
    }

    private Comparator<Map.Entry<Long, ConfluenceProperties>> getVersionComparator(ConfluenceProperties pageProperties)
    {
        return (a, b) -> {
            String versionA = a.getValue().getString(ConfluenceXMLPackage.KEY_PAGE_REVISION);
            String versionB = b.getValue().getString(ConfluenceXMLPackage.KEY_PAGE_REVISION);
            if (StringUtils.isEmpty(versionA) || StringUtils.isEmpty(versionB)) {
                logger.error("One of the versions is empty while comparing revisions, this should not "
                    + "happen");
                return 0;
            }

            String[] splitVersionA = StringUtils.split(versionA, '.');
            String[] splitVersionB = StringUtils.split(versionB, '.');
            for (int i = 0; i < splitVersionA.length; i++) {
                if (i >= splitVersionB.length) {
                    return 1;
                }

                try {
                    int versionPartA = Integer.parseInt(splitVersionA[i]);
                    int versionPartB = Integer.parseInt(splitVersionB[i]);
                    if (versionPartA != versionPartB) {
                        return versionPartA - versionPartB;
                    }
                } catch (NumberFormatException e) {
                    logger.error("Failed to parse either [{}] or [{}] as a version, revisions might be "
                            + "in the wrong order for page [{}]. This should not happen.", versionA, versionB,
                        createPageIdentifier(pageProperties), e);
                    return versionA.compareTo(versionB);
                }
            }

            return versionA.compareTo(versionB);
        };
    }

    private FilterEventParameters getDocumentLocaleParameters(ConfluenceProperties pageProperties)
    {
        FilterEventParameters documentLocaleParameters = new FilterEventParameters();
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_CREATION_AUTHOR)) {
            documentLocaleParameters.put(WikiDocumentFilter.PARAMETER_CREATION_AUTHOR,
                confluenceConverter.toUserReference(
                    pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_CREATION_AUTHOR)));
        } else if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_CREATION_AUTHOR_KEY)) {
            String authorKey = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_CREATION_AUTHOR_KEY);
            String authorName = confluenceConverter.toUserReference(resolveUserName(authorKey));
            documentLocaleParameters.put(WikiDocumentFilter.PARAMETER_CREATION_AUTHOR, authorName);
        }

        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_CREATION_DATE)) {
            try {
                documentLocaleParameters.put(WikiDocumentFilter.PARAMETER_CREATION_DATE,
                    this.confluencePackage.getDate(pageProperties, ConfluenceXMLPackage.KEY_PAGE_CREATION_DATE));
            } catch (Exception e) {
                this.logger.error("Failed to parse creation date of the document with id [{}]",
                    createPageIdentifier(pageProperties), e);
            }
        }

        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISION)) {
            documentLocaleParameters.put(WikiDocumentFilter.PARAMETER_LASTREVISION,
                pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION));
        }
        return documentLocaleParameters;
    }

    private ConfluenceProperties getPermissionSetProperties(ConfluenceProperties pageProperties, long permissionSetId)
        throws FilterException
    {
        ConfluenceProperties permissionSetProperties;

        if (!shouldSendObject(permissionSetId)) {
            return null;
        }

        try {
            permissionSetProperties = confluencePackage.getContentPermissionSetProperties(permissionSetId);
        } catch (ConfigurationException e) {
            logger.error("Could not get permission set [{}] for page [{}]",
                permissionSetId, createPageIdentifier(pageProperties), e);
            return null;
        }

        if (permissionSetProperties == null) {
            logger.error("Could not find permission set [{}] for page [{}].",
                permissionSetId, createPageIdentifier(pageProperties));
            return null;
        }

        return permissionSetProperties;

    }

    private ConfluenceProperties getPermProperties(long permissionSetId, long permissionId,
        ConfluenceProperties pageProperties) throws FilterException
    {
        if (!shouldSendObject(permissionId)) {
            return null;
        }

        ConfluenceProperties permProperties;
        try {
            permProperties = confluencePackage.getContentPermissionProperties(permissionSetId, permissionId);
        } catch (ConfigurationException e) {
            logger.error("Could not get permission [{}] for page [{}]",
                permissionId, createPageIdentifier(pageProperties), e);
            return null;
        }

        if (permProperties == null) {
            logger.error("Could not find permission [{}] for page [{}].",
                permissionId, createPageIdentifier(pageProperties));
            return null;
        }
        return permProperties;

    }

    private void sendPageRights(ConfluenceFilter proxyFilter, ConfluenceProperties pageProperties,
        Collection<ConfluenceRight> inheritedRights) throws FilterException
    {
        for (Object permissionSetIdObject : ConfluenceXMLPackage.getContentPermissionSets(pageProperties)) {
            long permissionSetId = toLong(permissionSetIdObject);
            ConfluenceProperties permissionSetProperties = getPermissionSetProperties(pageProperties, permissionSetId);
            if (permissionSetProperties == null) {
                continue;
            }

            sendPageRight(proxyFilter, pageProperties, permissionSetProperties, permissionSetId, inheritedRights);
        }
    }

    private void sendPageRight(ConfluenceFilter proxyFilter, ConfluenceProperties pageProperties,
        ConfluenceProperties permissionSetProperties, long permissionSetId, Collection<ConfluenceRight> inheritedRights)
        throws FilterException
    {
        for (Object permissionIdObject : ConfluenceXMLPackage.getContentPermissions(permissionSetProperties)) {
            long permissionId = toLong(permissionIdObject);

            ConfluenceProperties permProperties = getPermProperties(permissionSetId, permissionId, pageProperties);
            ContentPermissionType type = null;
            ConfluenceRight confluenceRight = null;

            if (permProperties != null) {
                confluenceRight = getConfluenceRightData(permProperties);
                type = getContentPermissionType(pageProperties, confluenceRight, permissionId);
            }

            if (type == null) {
                continue;
            }

            Right right = convertPageRight(type);

            if (right != null && !(confluenceRight.users.isEmpty() && confluenceRight.group.isEmpty())) {
                if (inheritedRights != null && Right.VIEW.equals(right)) {
                    inheritedRights.add(confluenceRight);
                } else {
                    sendRight(proxyFilter, confluenceRight.group, right, confluenceRight.users, false);
                }
            }
        }
    }

    private Right convertPageRight(ContentPermissionType type)
    {
        Right right;
        switch (type) {
            case VIEW:
                right = Right.VIEW;
                break;
            case EDIT:
                right = Right.EDIT;
                break;
            case SHARE:
                // Sharing is not represented in XWiki rights
                right = null;
                break;
            default:
                this.logger.warn("Unknown content permission right type [{}].", type);
                right = null;
                break;
        }
        return right;
    }

    private ContentPermissionType getContentPermissionType(ConfluenceProperties pageProperties,
        ConfluenceRight confluenceRight, Long permissionId)
    {
        ContentPermissionType type;
        try {
            type = ContentPermissionType.valueOf(confluenceRight.type.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to understand content permission type [{}] for page [{}], permission id [{}].",
                confluenceRight.type, createPageIdentifier(pageProperties), permissionId == null);
            return null;
        }
        return type;
    }

    private static long toLong(Object permissionSetIdObject)
    {
        return permissionSetIdObject instanceof Long
            ? (Long) permissionSetIdObject
            : Long.parseLong((String) permissionSetIdObject);
    }

    private ConfluenceProperties getPageProperties(Long pageId) throws FilterException
    {
        try {
            return this.confluencePackage.getPageProperties(pageId, false);
        } catch (ConfigurationException e) {
            throw new FilterException("Failed to get page properties", e);
        }
    }

    private void readPageRevision(ConfluenceProperties pageProperties, boolean blog,
        Map<String, List<AttachmentInfo>> attachments, Object filter, ConfluenceFilter proxyFilter,
        String spaceKey, Collection<ConfluenceRight> inheritedRights, boolean hide, Locale locale,
        EntityReference docRef) throws FilterException
    {
        // beware. Here, pageProperties might not have a space key. You need to use the one passed in parameters
        // FIXME we could ensure it though with some work

        Long pageId = pageProperties.getLong(ConfluenceXMLPackage.KEY_ID, null);
        checkNonNullPageId(spaceKey, pageId);

        if (this.properties.isVerbose()) {
            this.logger.info(SEND_PAGE_MARKER, "Sending page [{}], locale [{}]", createPageIdentifier(pageProperties,
                spaceKey), locale);
        }

        macrosIds.clear();
        inlineComments.clear();

        // pageId is used as a fallback, an empty revision would prevent the revision from going through.
        String revision = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION, null);
        if (revision == null) {
            revision = pageId.toString();
        }

        FilterEventParameters docRevisionParameters = new FilterEventParameters();
        if (hide) {
            docRevisionParameters.put(WikiDocumentFilter.PARAMETER_HIDDEN, true);
        }

        prepareRevisionMetadata(pageProperties, docRevisionParameters);

        beginPageRevision(blog, pageProperties, filter, proxyFilter, revision, docRevisionParameters,
            ConfluenceXMLPackage.KEY_PAGE_BODY);

        if (this.properties.isRightsEnabled()) {
            sendPageRights(proxyFilter, pageProperties, inheritedRights);
        }

        try {
            readAttachments(pageProperties, attachments, proxyFilter);
            readPageTags(pageProperties, proxyFilter);
            readComments(pageProperties, docRef, proxyFilter);
            String title = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE, null);
            Long stableId = pageProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_ORIGINAL_VERSION, pageId);
            boolean home = pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_HOMEPAGE);
            storeConfluenceDetails(spaceKey, title, pageId, stableId, home, proxyFilter);
        } finally {
            // < WikiDocumentRevision
            proxyFilter.endWikiDocumentRevision(revision, docRevisionParameters);
        }
    }

    private static void checkNonNullPageId(String spaceKey, Long pageId) throws FilterException
    {
        if (pageId == null) {
            throw new FilterException("Found a null revision id in space [" + spaceKey + "], should not happen.");
        }
    }

    private ConfluenceProperties getSpacePropertiesFromPage(ConfluenceProperties pageProperties) throws FilterException
    {
        Long spaceId = pageProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_SPACE, null);

        if (spaceId != null) {
            try {
                return this.confluencePackage.getSpaceProperties(spaceId);
            } catch (ConfigurationException e) {
                throw new FilterException("Failed to get the space properties", e);
            }
        }

        return null;
    }


    private ConfluenceProperties getSpaceDescriptorPropertiesFromSpace(ConfluenceProperties spaceProperties)
        throws FilterException
    {
        Long spaceDescriptorId = spaceProperties.getLong(ConfluenceXMLPackage.KEY_SPACE_DESCRIPTION, null);

        if (spaceDescriptorId != null) {
            try {
                return this.confluencePackage.getSpaceDescriptorProperties(spaceDescriptorId, false);
            } catch (ConfigurationException e) {
                throw new FilterException("Failed to get the space description properties", e);
            }
        }

        return null;
    }

    private ConfluenceProperties getSpaceDescriptorPropertiesFromPage(ConfluenceProperties pageProperties)
        throws FilterException
    {
        ConfluenceProperties spaceProperties = getSpacePropertiesFromPage(pageProperties);

        if (spaceProperties != null) {
            return getSpaceDescriptorPropertiesFromSpace(spaceProperties);
        }

        return null;
    }

    private void beginPageRevision(boolean isBlog, ConfluenceProperties pageProperties,
        Object filter, ConfluenceFilter proxyFilter, String revision, FilterEventParameters docRevisionParameters,
        String keyPageBody) throws FilterException
    {
        String bodyContent = pageProperties.getString(keyPageBody, null);
        if (this.properties.isContentsEnabled()) {
            if (bodyContent == null) {
                logger.error("Content is missing for page with id [{}]", createPageIdentifier(pageProperties));
            } else {
                // No bodyType means old Confluence syntax
                int bodyType = pageProperties.getInt(ConfluenceXMLPackage.KEY_PAGE_BODY_TYPE, 0);

                if (!isBlog && this.properties.isContentEvents() && filter instanceof Listener) {
                    // > WikiDocumentRevision
                    proxyFilter.beginWikiDocumentRevision(revision, docRevisionParameters);

                    try {
                        parse(bodyContent, bodyType, this.properties.getMacroContentSyntax(), proxyFilter);
                    } catch (Exception e) {
                        this.logger.error("Failed to parse content of page with id [{}]",
                            createPageIdentifier(pageProperties), e);
                    }
                    return;
                }

                Syntax bodySyntax = getBodySyntax(pageProperties, bodyType);

                if (this.properties.isConvertToXWiki()) {
                    try {
                        bodyContent = convertToXWiki21(bodyContent, bodyType);
                        bodySyntax = Syntax.XWIKI_2_1;
                    } catch (Exception e) {
                        this.logger.error("Failed to convert content of the page with id [{}]",
                            createPageIdentifier(pageProperties), e);
                    }
                }

                if (!isBlog) {
                    docRevisionParameters.put(WikiDocumentFilter.PARAMETER_CONTENT, bodyContent);
                }

                docRevisionParameters.put(WikiDocumentFilter.PARAMETER_SYNTAX, bodySyntax);
            }
        }

        // > WikiDocumentRevision
        proxyFilter.beginWikiDocumentRevision(revision, docRevisionParameters);

        // Generate page content when the page is a regular page or the value of the "content" property of the
        // "Blog.BlogPostClass" object if the page is a blog post.
        maybeSendBlogObject(isBlog, pageProperties, proxyFilter, bodyContent);
    }

    private void maybeSendBlogObject(boolean isBlog, ConfluenceProperties pageProperties, ConfluenceFilter proxyFilter,
        String bodyContent) throws FilterException
    {
        if (isBlog) {
            // Add the Blog post object
            Date publishDate = null;
            try {
                publishDate =
                    this.confluencePackage.getDate(pageProperties, ConfluenceXMLPackage.KEY_PAGE_CREATION_DATE);
                if (publishDate == null) {
                    publishDate =
                        this.confluencePackage.getDate(pageProperties, ConfluenceXMLPackage.KEY_PAGE_REVISION_DATE);
                }
            } catch (Exception e) {
                this.logger.error(
                    "Failed to parse the publish date of the blog post document with id [{}]",
                    createPageIdentifier(pageProperties), e);
            }

            addBlogPostObject(pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE), bodyContent,
                publishDate, proxyFilter);
        }
    }

    private Syntax getBodySyntax(ConfluenceProperties pageProperties, int bodyType)
    {
        switch (bodyType) {
            case 0:
                return ConfluenceParser.SYNTAX;
            case 2:
                return Syntax.CONFLUENCEXHTML_1_0;
            default:
                this.logger.warn("Unknown body type [{}] for the content of the document with id [{}].", bodyType,
                    createPageIdentifier(pageProperties));
                return null;
        }
    }

    private void prepareRevisionMetadata(ConfluenceProperties pageProperties,
        FilterEventParameters documentRevisionParameters)
    {
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISION_AUTHOR)) {
            documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_EFFECTIVEMETADATA_AUTHOR,
                confluenceConverter.toUserReference(
                    pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION_AUTHOR)));
        } else if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISION_AUTHOR_KEY)) {
            String authorKey = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION_AUTHOR_KEY);
            String authorName = confluenceConverter.toUserReference(resolveUserName(authorKey));
            documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_EFFECTIVEMETADATA_AUTHOR, authorName);
        }
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISION_DATE)) {
            try {
                documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_DATE,
                    this.confluencePackage.getDate(pageProperties, ConfluenceXMLPackage.KEY_PAGE_REVISION_DATE));
            } catch (Exception e) {
                this.logger.error("Failed to parse the revision date of the document with id [{}]",
                    createPageIdentifier(pageProperties), e);
            }
        }
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISION_COMMENT)) {
            documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_COMMENT,
                pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION_COMMENT));
        }

        String title = (!this.properties.isSpaceTitleFromHomePage()
            && pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_HOMEPAGE))
                ? getSpaceTitle(pageProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_SPACE, null))
                : pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE, null);

        if (title == null) {
            title = pageProperties.getString(NAME, null);
        }

        if (title != null) {
            documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_TITLE, title);
        }
    }

    private ConfluenceProperties getCommentProperties(ConfluenceProperties pageProperties, long commentId)
        throws FilterException
    {
        if (!shouldSendObject(commentId)) {
            return null;
        }
        try {
            return this.confluencePackage.getObjectProperties(commentId);
        } catch (ConfigurationException e) {
            logger.error("Failed to get the comment properties [{}] for the page with id [{}]",
                commentId, createPageIdentifier(pageProperties), e);
        }
        return null;
    }

    private void readComments(ConfluenceProperties pageProperties, EntityReference docRef,
        ConfluenceFilter proxyFilter)
    {
        List<Long> commentIds = confluencePackage.getPageComments(pageProperties);
        Map<Long, ConfluenceProperties> commentsById = new HashMap<>(commentIds.size());
        for (long commentId : commentIds) {
            try {
                ConfluenceProperties commentProperties = getCommentProperties(pageProperties, commentId);
                commentsById.put(commentId, commentProperties);
            } catch (FilterException e) {
                logger.error("Failed to get properties of comment id [{}], skipping it", commentId, e);
            }
        }

        Set<String> resolvedComments = new HashSet<>();
        Map<Long, Integer> commentIndices = new HashMap<>();
        AtomicInteger i = new AtomicInteger();

        commentsById
            .entrySet()
            .stream()
            .sorted(getDateComparator(ConfluenceXMLPackage.KEY_PAGE_CREATION_DATE, COMMENT, pageProperties))
            .forEachOrdered(entry -> {
                Long commentId = entry.getKey();
                // The comment indices are used to fill the replyto field of comments.
                // By filling the comment indices in the foreach loop, we are only passing partial information to
                // readPageComment. We are assuming comments only reply to older comments, and so that readPageComment
                // will always ever only need to access indexes of older comments. Which feels quite reasonable an
                // assumption.
                commentIndices.put(commentId, i.getAndIncrement());
                try {
                    readPageComment(pageProperties, proxyFilter, commentId, commentsById, commentIndices,
                        resolvedComments, docRef);
                } catch (FilterException e) {
                    logger.error("Failed to read comment [{}] in page [{}]",
                        commentId, createPageIdentifier(pageProperties), e);
                }
            });
    }

    private Comparator<Map.Entry<Long, ConfluenceProperties>> getDateComparator(String dateField, String type,
        ConfluenceProperties pageProperties)
    {
        return (c1, c2) -> {
            Date d1 = getDate(c1.getValue(), c1.getKey(), type, dateField, pageProperties);
            Date d2 = getDate(c2.getValue(), c2.getKey(), type, dateField, pageProperties);
            if (d1 == null || d2 == null) {
                if (Objects.equals(c1.getKey(), c2.getKey())) {
                    return 0;
                }
                return c1.getKey() < c2.getKey() ? -1 : 1;
            }
            return d1.compareTo(d2);
        };
    }

    private void readPageTags(ConfluenceProperties pageProperties, ConfluenceFilter proxyFilter) throws FilterException
    {
        if (!this.properties.isTagsEnabled()) {
            return;
        }

        Map<String, ConfluenceProperties> tags = new LinkedHashMap<>();

        // If it's the home page, also include space tags
        if (this.confluencePackage.isHomePage(pageProperties)) {
            ConfluenceProperties spaceDescriptorProperties = getSpaceDescriptorPropertiesFromPage(pageProperties);

            if (spaceDescriptorProperties != null) {
                getTags(spaceDescriptorProperties, tags);
            }
        }

        // Gather page tags
        getTags(pageProperties, tags);

        // Send tags
        if (!tags.isEmpty()) {
            sendPageTags(proxyFilter, tags, pageProperties);
        }
    }

    private void getTags(ConfluenceProperties parentProperties, Map<String, ConfluenceProperties> tags)
        throws FilterException
    {
        List<Object> labels = parentProperties.getList(ConfluenceXMLPackage.KEY_LABELLINGS);

        for (Object tagIdStringObject : labels) {
            Long tagId = Long.parseLong((String) tagIdStringObject);
            ConfluenceProperties tagProperties = getTagProperties(parentProperties, tagId);
            if (tagProperties == null) {
                continue;
            }

            String tagName = this.confluencePackage.getTagName(tagProperties);
            if (tagName == null) {
                this.logger.warn("Failed to get the name of label with id [{}] referenced from object {}.", tagId,
                    createObjectIdentifier(parentProperties));
            } else {
                tags.put(tagName, tagProperties);
            }
        }
    }

    private ConfluenceProperties getTagProperties(ConfluenceProperties parentProperties, Long tagId)
        throws FilterException
    {
        if (!shouldSendObject(tagId)) {
            return null;
        }

        try {
            return this.confluencePackage.getObjectProperties(tagId);
        } catch (ConfigurationException e) {
            this.logger.error("Failed to get tag properties [{}] for the object {}.", tagId,
                createObjectIdentifier(parentProperties), e);
        }

        return null;
    }

    private ConfluenceProperties getAttachmentProperties(long pageId, long attachmentId,
        ConfluenceProperties pageProperties) throws FilterException
    {
        if (!shouldSendObject(attachmentId)) {
            return null;
        }

        try {
            return this.confluencePackage.getAttachmentProperties(pageId, attachmentId);
        } catch (ConfigurationException e) {
            logger.error(
                "Failed to get the properties of the attachments from the document identified by [{}]",
                createPageIdentifier(pageProperties), e);
        }

        return null;
    }

    private void readAttachments(ConfluenceProperties pageProperties, Map<String, List<AttachmentInfo>> attachments,
        ConfluenceFilter proxyFilter)
    {
        if (!this.properties.isAttachmentsEnabled()) {
            return;
        }

        for (Map.Entry<String, List<AttachmentInfo>> attachmentEntry : attachments.entrySet()) {
            String attachmentName = attachmentEntry.getKey();
            List<AttachmentInfo> attachmentsWithThisName = attachmentEntry.getValue();
            readAttachments(pageProperties, proxyFilter, attachmentsWithThisName, attachmentName);
        }
    }

    private void readAttachments(ConfluenceProperties pageProperties, ConfluenceFilter proxyFilter,
        List<AttachmentInfo> attachments, String attachmentName)
    {
        if (attachments.isEmpty()) {
            return;
        }

        AttachmentInfo a = attachments.get(attachments.size() - 1);
        if (attachments.size() == 1 || !properties.isHistoryEnabled()) {
            // if there is only one attachment to send, send it with wikiAttachment to match whatever confluence-xml
            // has always done things
            try (FileInputStream fis = new FileInputStream(a.contentFile)) {
                proxyFilter.onWikiAttachment(attachmentName, fis, a.size, a.parameters);
            } catch (Exception e) {
                this.logger.error("Failed to read attachment [{}] for the page [{}].", a.attachmentId,
                    createPageIdentifier(pageProperties), e);
            }
            return;
        }
        if (a == null) {
            // We skip the last attachment version which we failed to read
            List<AttachmentInfo> attachmentsWithoutLast = attachments.subList(0, attachments.size() - 1);
            readAttachments(pageProperties, proxyFilter, attachmentsWithoutLast, attachmentName);
            return;
        }

        try (InputSource fis = new DefaultFileInputSource(a.contentFile)) {
            proxyFilter.beginWikiDocumentAttachment(attachmentName, fis, a.size, a.parameters);
            try {
                readWikiAttachmentRevisions(pageProperties, proxyFilter, attachments);
            } finally {
                proxyFilter.endWikiDocumentAttachment(attachmentName, fis, a.size, a.parameters);
            }
        } catch (IOException e) {
            logger.error("Failed to read attachment content at [{}]", a.contentFile, e);
        } catch (FilterException e) {
            logger.error("Failed to send attachment [{}] in page [{}]", a.contentFile,
                createPageIdentifier(pageProperties), e);
        }
    }

    private void readWikiAttachmentRevisions(ConfluenceProperties pageProperties, ConfluenceFilter proxyFilter,
        List<AttachmentInfo> attachmentsWithoutLast) throws FilterException
    {
        proxyFilter.beginWikiAttachmentRevisions(FilterEventParameters.EMPTY);
        try {
            for (AttachmentInfo attachment : attachmentsWithoutLast) {
                readAttachmentRevision(pageProperties, attachment, proxyFilter);
            }
        } finally {
            proxyFilter.endWikiAttachmentRevisions(FilterEventParameters.EMPTY);
        }
    }

    private void readAttachmentRevision(ConfluenceProperties pageProperties, AttachmentInfo a,
        ConfluenceFilter proxyFilter)
    {
        try (InputSource fis = new DefaultFileInputSource(a.contentFile)) {
            proxyFilter.beginWikiAttachmentRevision(Long.toString(a.revision), fis, a.size, a.parameters);
            proxyFilter.endWikiAttachmentRevision(Long.toString(a.revision), fis, a.size, a.parameters);
        } catch (IOException e) {
            logger.error("Failed to read attachment revision content at [{}]", a.contentFile, e);
        } catch (FilterException e) {
            logger.error("Failed to send attachment revision [{}] in page [{}]", a.contentFile,
                createPageIdentifier(pageProperties), e);
        }
    }

    private Map<String, List<AttachmentInfo>> getAttachments(ConfluenceProperties pageProperties)
        throws FilterException
    {
        Map<String, List<AttachmentInfo>> pageAttachments = new LinkedHashMap<>();
        Long pageId = pageProperties.getLong(ConfluenceXMLPackage.KEY_ID, null);
        if (pageId == null) {
            logger.error("Failed to get the id of a page while trying to find its attachments, this is unexpected");
            return Collections.emptyMap();
        }

        for (Long attachmentId : this.confluencePackage.getAttachments(pageId)) {
            ConfluenceProperties attachmentProperties = getAttachmentProperties(pageId, attachmentId, pageProperties);
            if (attachmentProperties == null
                || this.confluencePackage.getAttachementVersion(attachmentProperties) == null
                || "deleted".equalsIgnoreCase(
                    attachmentProperties.getString(ConfluenceXMLPackage.KEY_PAGE_CONTENT_STATUS))
            ) {
                continue;
            }

            String name = this.confluencePackage.getAttachmentName(attachmentProperties);
            List<AttachmentInfo> attachments = pageAttachments.computeIfAbsent(name, k -> new ArrayList<>());
            AttachmentInfo a = getAttachmentInfo(pageId, name, pageProperties, attachmentProperties);
            if (a != null) {
                attachments.add(a);
            }
        }

        for (Map.Entry<String, List<AttachmentInfo>> attachmentEntry : pageAttachments.entrySet()) {
            attachmentEntry.getValue().sort(this::compareAttachments);
        }

        return pageAttachments;
    }

    private int compareAttachments(AttachmentInfo a1, AttachmentInfo a2)
    {
        if (a1.revision == a2.revision) {
            return 0;
        }
        return a1.revision < a2.revision ? -1 : 1;
    }

    /**
     * @since 9.13
     */
    private void storeConfluenceDetails(String spaceKey, String pageTitle, Long pageId, Long stableId, boolean home,
        ConfluenceFilter proxyFilter) throws FilterException
    {
        if (!this.properties.isStoreConfluenceDetailsEnabled()) {
            return;
        }

        FilterEventParameters pageReportParameters = new FilterEventParameters();

        // Page report object
        pageReportParameters.put(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, CONFLUENCEPAGE_CLASSNAME);
        proxyFilter.beginWikiObject(CONFLUENCEPAGE_CLASSNAME, pageReportParameters);
        try {
            if (pageId != null) {
                proxyFilter.onWikiObjectProperty(ConfluenceXMLPackage.KEY_ID, pageId, FilterEventParameters.EMPTY);
            }

            if (stableId != null) {
                proxyFilter.onWikiObjectProperty("stableId", stableId, FilterEventParameters.EMPTY);
            }

            StringBuilder pageURLBuilder = new StringBuilder();
            if (!this.properties.getBaseURLs().isEmpty()) {
                pageURLBuilder.append(this.properties.getBaseURLs().get(0).toString());
                pageURLBuilder.append("/spaces/").append(spaceKey);
                if (!home && pageTitle != null) {
                    pageURLBuilder.append("/pages/").append(pageId).append('/').append(pageTitle);
                }
            }
            if (pageTitle != null) {
                proxyFilter.onWikiObjectProperty(TITLE, pageTitle, FilterEventParameters.EMPTY);
            }
            proxyFilter.onWikiObjectProperty("url", pageURLBuilder.toString(), FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty("space", spaceKey, FilterEventParameters.EMPTY);
        } finally {
            proxyFilter.endWikiObject(CONFLUENCEPAGE_CLASSNAME, pageReportParameters);
        }
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
        converterListener.setWrappedListener(listener);
        converterListener.setMacroIds(macrosIds);
        converterListener.setInlineComments(inlineComments);

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
        filterProperties.setReferenceConverter(confluenceConverter);
        filterProperties.setURLConverter(urlConverter);
        filterProperties.setMacroSupport(macroSupport);

        if (this.properties.isConvertToXWiki()) {
            filterProperties.setConverter(createConverter(null));
        }

        BeanInputFilterStreamFactory<ConfluenceXHTMLInputProperties> syntaxFilterFactory =
            ((BeanInputFilterStreamFactory<ConfluenceXHTMLInputProperties>) this.confluenceXHTMLParserFactory);

        return syntaxFilterFactory.createInputFilterStream(filterProperties);
    }

    private AttachmentInfo getAttachmentInfo(Long stableId, String attachmentName, ConfluenceProperties pageProperties,
        ConfluenceProperties attachmentProperties)
    {
        long attachmentId = attachmentProperties.getLong(ConfluenceXMLPackage.KEY_ID);
        // no need to check shouldSendObject(attachmentId), already done by the caller.

        Long version = this.confluencePackage.getAttachementVersion(attachmentProperties);
        if (version == null) {
            this.logger.warn("Failed to find version of attachment [{}] in page [{}]", attachmentName,
                createPageIdentifier(pageProperties));
            return null;
        }

        Long stableAttachmentId = attachmentProperties.getLong(ConfluenceXMLPackage.KEY_ATTACHMENT_ORIGINALVERSION,
            null);
        if (stableAttachmentId == null) {
            stableAttachmentId = attachmentId;
        }
        File contentFile;
        try {
            contentFile = this.confluencePackage.getAttachmentFile(stableId, stableAttachmentId, version);
        } catch (FileNotFoundException e) {
            this.logger.warn("Failed to find file corresponding to version [{}] attachment [{}] in page [{}]",
                version, attachmentName, createPageIdentifier(pageProperties));
            return null;
        }

        ConfluenceProperties attachmentContentProperties = attachmentProperties;
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_CONTENTPROPERTIES)) {
            try {
                attachmentContentProperties =
                    getContentProperties(attachmentProperties, ConfluenceXMLPackage.KEY_CONTENTPROPERTIES);
            } catch (FilterException e) {
                logger.error("Failed to get attachment content properties for [{}] in page [{}]", attachmentName,
                    createPageIdentifier(pageProperties));
            }
        }

        long attachmentSize = attachmentContentProperties.getLong(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENT_FILESIZE,
            -1);
        if (attachmentSize == -1 && contentFile != null) {
            attachmentSize = contentFile.length();
        }

        FilterEventParameters attachmentParameters = new FilterEventParameters();
        Date date = fillAttachmentDates(pageProperties, attachmentProperties, attachmentId, attachmentParameters);
        if (date == null) {
            return null;
        }
        attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION, Long.toString(version));
        fillAttachmentMediaType(attachmentContentProperties, attachmentParameters);
        fillAttachmentAuthor(attachmentProperties, attachmentParameters);
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_COMMENT)) {
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION_COMMENT,
                attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_COMMENT));
        }

        return new AttachmentInfo(attachmentId, attachmentSize, contentFile, version, attachmentParameters);
    }

    private Date fillAttachmentDates(ConfluenceProperties pageProperties, ConfluenceProperties attachmentProperties,
        long attachmentId, FilterEventParameters attachmentParameters)
    {
        Date creationDate = getDate(attachmentProperties, attachmentId, ATTACHMENT,
            ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_DATE, pageProperties);

        Date revisionDate = getDate(attachmentProperties, attachmentId, ATTACHMENT,
            ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_DATE, pageProperties);
        if (revisionDate == null) {
            revisionDate = creationDate;
        }
        if (creationDate == null) {
            creationDate = revisionDate;
        }
        if (creationDate == null) {
            this.logger.error("Failed to get both the creation and the revision date of attachment [{}] in page [{}]",
                attachmentId, createPageIdentifier(pageProperties));
            return null;
        }
        attachmentParameters.put(WikiAttachmentFilter.PARAMETER_CREATION_DATE, creationDate);
        attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION_DATE, revisionDate);
        return creationDate.before(revisionDate) ? creationDate : revisionDate;
    }

    private static void fillAttachmentMediaType(ConfluenceProperties contentProperties,
        FilterEventParameters attachmentParameters)
    {
        String mediaType = contentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENT_MEDIA_TYPE, null);
        if (mediaType == null) {
            mediaType = contentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENTTYPE);
        }
        if (mediaType != null) {
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_CONTENT_TYPE, mediaType);
        }
    }

    private void fillAttachmentAuthor(ConfluenceProperties attachmentProperties,
        FilterEventParameters attachmentParameters)
    {
        String userName = attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_AUTHOR, null);
        if (userName != null) {
            String userReference = confluenceConverter.toUserReference(userName);
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_CREATION_AUTHOR, userReference);
        }

        String creatorName = null;
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_AUTHOR_KEY)) {
            String creatorKey = attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_AUTHOR_KEY);
            creatorName = resolveUserName(creatorKey);
        } else if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_AUTHOR)) {
            creatorName = attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_AUTHOR);
        }

        if (creatorName != null) {
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION_AUTHOR,
                confluenceConverter.toUserReference(creatorName));
        }
    }

    private void sendPageTags(ConfluenceFilter proxyFilter, Map<String, ConfluenceProperties> pageTags,
        ConfluenceProperties pageProperties)
        throws FilterException
    {
        Collection<ConfluenceProperties> favorites = properties.isFavoritesEnabled() ? new ArrayList<>() : null;
        // get page tags separated by | as string
        StringBuilder tagBuilder = new StringBuilder();
        String prefix = "";
        for (Map.Entry<String, ConfluenceProperties> tagEntry : pageTags.entrySet()) {
            String tag = tagEntry.getKey();
            if ("favourite".equals(tag)) {
                if (favorites != null) {
                    favorites.add(tagEntry.getValue());
                }
            } else {
                tagBuilder.append(prefix);
                tagBuilder.append(tag);
                prefix = "|";
            }
        }

        String tags = tagBuilder.toString();
        if (!tags.isEmpty()) {
            FilterEventParameters pageTagsParameters = new FilterEventParameters();

            pageTagsParameters.put(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, TAGS_CLASSNAME);
            proxyFilter.beginWikiObject(TAGS_CLASSNAME, pageTagsParameters);
            try {
                proxyFilter.onWikiObjectProperty("tags", tags, FilterEventParameters.EMPTY);
            } finally {
                proxyFilter.endWikiObject(TAGS_CLASSNAME, pageTagsParameters);
            }
        }

        if (favorites != null) {
            sendFavorites(proxyFilter, pageProperties, favorites);
        }
    }

    private void sendFavorites(ConfluenceFilter proxyFilter, ConfluenceProperties pageProperties,
        Collection<ConfluenceProperties> favorites)
    {
        for (ConfluenceProperties favorite : favorites) {
            String userName = getFavoriteUser(favorite);
            if (StringUtils.isEmpty(userName)) {
                logger.error("Could not find the owning user of the favourite on page [{}]",
                    createPageIdentifier(pageProperties));
            } else {
                String xwikiUserName = confluenceConverter.toUserReferenceName(userName);
                EntityReference userRef = confluenceConverter.getUserOrGroupReference(xwikiUserName);
                if (userRef == null) {
                    logger.error("Could not get an XWiki reference for the owner of the favourite [{}] on page [{}]",
                        userName, createPageIdentifier(pageProperties));
                }
                try {
                    proxyFilter.onUserFavorite(userRef, FilterEventParameters.EMPTY);
                } catch (FilterException e) {
                    logger.error("Failed to send favorite for user [{}] on document [{}]", userRef,
                        createPageIdentifier(pageProperties));
                }
            }
        }
    }

    private String getFavoriteUser(ConfluenceProperties favorite)
    {
        String userKey = favorite.getString(ConfluenceXMLPackage.KEY_LABEL_OWNINGUSER);
        if (userKey != null) {
            return confluenceConverter.toUserReferenceName(resolveUserName(userKey));
        }

        return favorite.getString(ConfluenceXMLPackage.KEY_LABEL_USER);
    }

    private void readPageComment(ConfluenceProperties pageProperties, ConfluenceFilter proxyFilter, Long commentId,
        Map<Long, ConfluenceProperties> pageComments, Map<Long, Integer> commentIndices, Set<String> resolvedComments,
        EntityReference docRef)
        throws FilterException
    {
        FilterEventParameters commentParameters = new FilterEventParameters();
        // object properties
        ConfluenceProperties commentProperties = pageComments.get(commentId);

        if (shouldSkipComment(commentProperties, resolvedComments)) {
            resolvedComments.add(commentId.toString());
            return;
        }

        // Comment object
        commentParameters.put(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, COMMENTS_CLASSNAME);
        proxyFilter.beginWikiObject(COMMENTS_CLASSNAME, commentParameters);

        try {
            // creator -
            String commentCreatorReference;
            if (commentProperties.containsKey(CREATOR_NAME)) {
                commentCreatorReference =
                    confluenceConverter.toUserReference(commentProperties.getString(CREATOR_NAME));
            } else {
                String userKey = commentProperties.getString("creator");
                commentCreatorReference = confluenceConverter.toUserReference(resolveUserName(userKey));
            }

            String commentBodyContent = this.confluencePackage.getCommentText(commentProperties);
            String commentText = commentBodyContent;
            if (commentBodyContent != null && this.properties.isConvertToXWiki()) {
                try {
                    int commentBodyType = this.confluencePackage.getCommentBodyType(commentProperties);
                    commentText = convertToXWiki21(commentBodyContent, commentBodyType);
                } catch (Exception e) {
                    this.logger.error("Failed to convert content of the comment with id [{}] for page [{}]",
                        commentId, createPageIdentifier(pageProperties), e);
                }
            }

            // creation date
            Date commentDate = getDate(commentProperties, commentId, COMMENT,
                ConfluenceXMLPackage.KEY_PAGE_CREATION_DATE, pageProperties);

            // parent (replyto)
            Integer parentIndex = null;
            if (commentProperties.containsKey(PARENT)) {
                Long parentId = commentProperties.getLong(PARENT);
                parentIndex = commentIndices.get(parentId);
            }

            proxyFilter.onWikiObjectProperty("author", commentCreatorReference, FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty(COMMENT, commentText, FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty("date", commentDate, FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty("replyto", parentIndex, FilterEventParameters.EMPTY);

            // selection
            readPageCommentSelection(commentProperties, docRef, proxyFilter);
        } finally {
            proxyFilter.endWikiObject(COMMENTS_CLASSNAME, commentParameters);
        }
    }

    private Date getDate(ConfluenceProperties properties, Long objectId, String type, String dateField,
        ConfluenceProperties pageProperties)
    {
        Date creationDate = null;
        try {
            creationDate = this.confluencePackage.getDate(properties, dateField);
        } catch (Exception e) {
            if (pageProperties == null) {
                this.logger.error("Failed to parse the {} of {} id [{}]", type, dateField, objectId, e);
            } else {
                this.logger.error("Failed to parse the {} of {} id [{}] in page [{}]", type, dateField,
                    objectId, createPageIdentifier(pageProperties), e);
            }
        }
        return creationDate;
    }

    private void readPageCommentSelection(ConfluenceProperties commentProperties, EntityReference docRef,
        ConfluenceFilter proxyFilter) throws FilterException
    {
        ConfluenceProperties commentObjectProperties =
            getContentProperties(commentProperties, ConfluenceXMLPackage.KEY_CONTENTPROPERTIES);
        if (commentObjectProperties != null) {
            String annotationRef = commentObjectProperties.getString(INLINE_MARKER_REF);
            if (annotationRef != null) {
                String annotation = this.inlineComments.get(annotationRef);

                if (annotation != null) {
                    proxyFilter.onWikiObjectProperty("selection", annotation, FilterEventParameters.EMPTY);
                    proxyFilter.onWikiObjectProperty("state", "SAFE", FilterEventParameters.EMPTY);
                    proxyFilter.onWikiObjectProperty("target", localSerializer.serialize(docRef),
                        FilterEventParameters.EMPTY);
                }
            }
        }
    }

    private boolean shouldSkipComment(ConfluenceProperties commentProperties, Set<String> resolvedComments)
        throws FilterException
    {
        if (!properties.isSkipResolvedInlineComments()) {
            return false;
        }

        if (resolvedComments.contains(commentProperties.getString(ConfluenceXMLPackage.KEY_PAGE_PARENT, ""))) {
            return true;
        }

        if (!commentProperties.containsKey(ConfluenceXMLPackage.KEY_CONTENTPROPERTIES)) {
            return false;
        }

        ConfluenceProperties contentProps =
            getContentProperties(commentProperties, ConfluenceXMLPackage.KEY_CONTENTPROPERTIES);

        if (contentProps == null || (!contentProps.containsKey("inline-comment")
            && !contentProps.getString("actualCommentType", "").equals("inline"))) {
            return false;
        }
        String commentStatus = contentProps.getString("status", "");
        return commentStatus.equals("resolved") || commentStatus.equals("dangling");
    }

    private void addBlogDescriptorPage(ConfluenceFilter proxyFilter) throws FilterException
    {
        // Apply the standard entity name validator
        String documentName = confluenceConverter.toEntityName(WEB_HOME);

        // > WikiDocument
        proxyFilter.beginWikiDocument(documentName, FilterEventParameters.EMPTY);
        try {
            FilterEventParameters blogParameters = new FilterEventParameters();

            // Blog Object
            blogParameters.put(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, BLOG_CLASSNAME);

            String blogSpaceName = this.properties.getBlogSpaceName();
            proxyFilter.beginWikiObject(BLOG_CLASSNAME, blogParameters);
            try {
                // Object properties
                proxyFilter.onWikiObjectProperty(TITLE, blogSpaceName, FilterEventParameters.EMPTY);
                proxyFilter.onWikiObjectProperty("postsLayout", "image", FilterEventParameters.EMPTY);
                proxyFilter.onWikiObjectProperty("displayType", "paginated", FilterEventParameters.EMPTY);
            } finally {
                proxyFilter.endWikiObject(BLOG_CLASSNAME, blogParameters);
            }
        } finally {
            // < WikiDocument
            proxyFilter.endWikiDocument(documentName, FilterEventParameters.EMPTY);
        }
    }

    private void addBlogPostObject(String title, String content, Date publishDate, ConfluenceFilter proxyFilter)
        throws FilterException
    {
        FilterEventParameters blogPostParameters = new FilterEventParameters();

        // Blog Post Object
        blogPostParameters.put(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, BLOG_POST_CLASSNAME);

        proxyFilter.beginWikiObject(BLOG_POST_CLASSNAME, blogPostParameters);
        try {
            // Object properties
            proxyFilter.onWikiObjectProperty(TITLE, title, FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty("content", content, FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty("publishDate", publishDate, FilterEventParameters.EMPTY);

            // The blog post 'published' property is always set to true because unpublished blog posts are draft pages
            // and draft pages are skipped during the import.
            proxyFilter.onWikiObjectProperty("published", 1, FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty("hidden", 0, FilterEventParameters.EMPTY);
        } finally {
            proxyFilter.endWikiObject(BLOG_POST_CLASSNAME, blogPostParameters);
        }
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

    private boolean shouldSpaceTargetBeRenamed(String target) throws FilterException
    {
        SpaceReference spaceReference = spaceHelpers.getSpaceReferenceWithRoot(target, this.properties.getRoot());

        if (spaceHelpers.isSpaceOverwriteProtected(spaceReference, this.properties.getOverwriteProtectedSpaces()))
        {
            return true;
        }

        OverwriteProtectionMode overwriteProtectionMode = this.properties.getOverwriteProtectionMode();

        switch (overwriteProtectionMode) {
            case NONCONFLUENCE:
                return spaceHelpers.isCollidingWithAProtectedSpace(spaceReference, false);
            case ANY:
                return spaceHelpers.isCollidingWithAProtectedSpace(spaceReference, true);
            case NONE:
            default:
                return false;
        }
    }
}
