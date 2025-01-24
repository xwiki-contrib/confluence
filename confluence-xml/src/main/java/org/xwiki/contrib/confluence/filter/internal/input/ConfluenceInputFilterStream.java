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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

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
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.input.StringInputSource;
import org.xwiki.job.Job;
import org.xwiki.job.JobContext;
import org.xwiki.job.event.status.CancelableJobStatus;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
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

    private static final String ID = "id";

    private  static final String FAILED_TO_GET_GROUP_PROPERTIES = "Failed to get group properties";
    private static final String PAGE_IDENTIFIER_ERROR =
        "Configuration error while creating page identifier for page [{}]";

    private static final Marker SEND_PAGE_MARKER = MarkerFactory.getMarker("ConfluenceSendingPage");
    private static final String FAILED_TO_PARSE_ATTACHMENT_REV = "For attachment [{}] in page [{}], failed to parse "
        + "the revision of attachment id [{}], will keep attachment id [{}]";

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
    private Logger logger;

    @Inject
    private JobContext jobContext;

    private final Map<String, Integer> macrosIds = new HashMap<>();

    private ConfluenceIdRangeList objectIdRanges;

    private List<Long> nextIdsForObjectIdRanges;

    private int remainingPages = -1;

    private CancelableJobStatus jobStatus;

    private FilterEventParameters webPreferenceParameters;

    private static final class MaxPageCountReachedException extends ConfluenceInterruptedException
    {
        private static final long serialVersionUID = 1L;
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

    private void pushLevelProgress(int steps)
    {
        try {
            this.progress.pushLevelProgress(steps, this);
        } catch (Exception e) {
            logger.error("Could not push level progress", e);
        }
    }

    private void popLevelProgress()
    {
        try {
            this.progress.popLevelProgress(this);
        } catch (Exception e) {
            logger.error("Could not pop level progress", e);
        }
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

        pushLevelProgress(progressCount);
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
            popLevelProgress();
            observationManager.notify(new ConfluenceFilteredEvent(), this, this.confluencePackage);
            closeConfluencePackage();
            popLevelProgress();
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
            pushLevelProgress(restored ? 1 : 2);
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

        maybeRemoveArchivedSpaces();

        ConfluenceFilteringEvent filteringEvent = new ConfluenceFilteringEvent();
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
                    sendConfluenceRootSpace(spaceId, filter, proxyFilter, blogPageIds);
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

    private void maybeRemoveArchivedSpaces() throws FilterException
    {
        // Yes, this is a bit hacky, I know. It would be better to not even create objects related to spaces that should
        // not be there. This is harder to do. If you find a cleaner way, don't hesitate do change this.
        if (!properties.isArchivedSpacesEnabled()) {
            try {
                for (Iterator<Long> it = confluencePackage.getPages().keySet().iterator(); it.hasNext();) {
                    Long spaceId = it.next();
                    if (spaceId != null && confluencePackage.isSpaceArchived(spaceId)) {
                        confluencePackage.getBlogPages().remove(spaceId);
                        confluencePackage.getSpacesByKey().remove(confluencePackage.getSpaceKey(spaceId));
                        it.remove();
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
        List<Long> blogPages) throws FilterException, ConfluenceInterruptedException
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

        FilterEventParameters spaceParameters = new FilterEventParameters();

        if (this.properties.isVerbose()) {
            this.logger.info("Sending Confluence space [{}], id=[{}]", spaceKey, spaceId);
        }

        sendSpace(spaceId, filter, proxyFilter, blogPages, spaceKey, spaceParameters, spaceProperties);
    }

    private void sendSpace(long spaceId, Object filter, ConfluenceFilter proxyFilter, List<Long> blogPages,
        String spaceKey, FilterEventParameters spaceParameters, ConfluenceProperties spaceProperties)
        throws FilterException, ConfluenceInterruptedException
    {
        String spaceEntityName = confluenceConverter.toEntityName(spaceKey);
        proxyFilter.beginWikiSpace(spaceEntityName, spaceParameters);
        try {
            Collection<ConfluenceRight> inheritedRights = null;
            ConfluenceProperties homePageProperties = null;
            try {
                List<Long> orphans = confluencePackage.getOrphans(spaceId);
                Long homePageId = confluencePackage.getHomePage(spaceId);
                if (this.properties.isContentsEnabled() || this.properties.isRightsEnabled()) {
                    if (homePageId != null) {
                        inheritedRights = sendPage(homePageId, spaceKey, false, filter, proxyFilter);
                        homePageProperties = getPageProperties(homePageId);
                    }

                    sendPages(spaceKey, false, orphans, filter, proxyFilter);
                    sendBlogs(spaceKey, blogPages, filter, proxyFilter);
                }

                if (this.properties.isPageOrderEnabled()) {
                    List<Long> children = confluencePackage.getPageChildren(homePageId);
                    Collection<String> orderedTitles = getOrderedDocumentTitles(
                        IterableUtils.chainedIterable(children, blogPages));
                    sendPinnedPages(proxyFilter, orderedTitles);
                }
            } catch (ConfluenceInterruptedException e) {
                // Even if we reached the maximum page count, we want to send the space rights.
                if (this.properties.isRightsEnabled()) {
                    sendSpaceRights(proxyFilter, spaceProperties, spaceKey, spaceId,
                        inheritedRights, homePageProperties);
                }
                throw e;
            }
            if (this.properties.isRightsEnabled()) {
                sendSpaceRights(proxyFilter, spaceProperties, spaceKey, spaceId,
                    inheritedRights, homePageProperties);
            }
        } finally {
            endWebPreferences(proxyFilter);
            // < WikiSpace
            proxyFilter.endWikiSpace(spaceEntityName, spaceParameters);
            if (this.properties.isVerbose()) {
                this.logger.info("Finished sending Confluence space [{}], id=[{}]", spaceKey, spaceId);
            }
        }
    }

    private void checkCanceled() throws ConfluenceCanceledException
    {
        if (jobStatus != null && jobStatus.isCanceled()) {
            throw new ConfluenceCanceledException();
        }
    }

    private Collection<ConfluenceRight> sendPage(long pageId, String spaceKey, boolean blog, Object filter,
        ConfluenceFilter proxyFilter) throws ConfluenceInterruptedException
    {
        if (this.remainingPages == 0) {
            throw new MaxPageCountReachedException();
        }

        checkCanceled();

        Collection<ConfluenceRight> inheritedRights = null;

        if (this.properties.isIncluded(pageId)) {
            ((DefaultConfluenceInputContext) this.context).setCurrentPage(pageId);
            try {
                inheritedRights = readPage(pageId, spaceKey, blog, filter, proxyFilter);
            } catch (MaxPageCountReachedException e) {
                // ignore
            } catch (ConfluenceCanceledException e) {
                throw e;
            } catch (Exception e) {
                logger.error("Failed to filter the page with id [{}]", createPageIdentifier(pageId, spaceKey), e);
            }
        }

        return inheritedRights;
    }

    private void sendBlogs(String spaceKey, List<Long> blogPages, Object filter, ConfluenceFilter proxyFilter)
        throws FilterException, ConfluenceInterruptedException
    {
        if (!this.properties.isBlogsEnabled() || blogPages == null || blogPages.isEmpty()) {
            return;
        }

        // Blog space
        String blogSpaceKey = confluenceConverter.toEntityName(this.properties.getBlogSpaceName());

        // > WikiSpace
        proxyFilter.beginWikiSpace(blogSpaceKey, FilterEventParameters.EMPTY);
        try {
            if (this.properties.isPageOrderEnabled()) {
                Collection<String> orderedTitles = getOrderedDocumentTitles(blogPages);
                sendPinnedPages(proxyFilter, orderedTitles);
                endWebPreferences(proxyFilter);
            }
            // Blog Descriptor page
            addBlogDescriptorPage(proxyFilter);

            // Blog post pages
            sendPages(spaceKey, true, blogPages, filter, proxyFilter);
        } finally {
            // < WikiSpace
            proxyFilter.endWikiSpace(blogSpaceKey, FilterEventParameters.EMPTY);
        }
    }

    private void sendPages(String spaceKey, boolean blog, List<Long> pages, Object filter, ConfluenceFilter proxyFilter)
        throws ConfluenceInterruptedException
    {
        Long homePageId = confluencePackage.getHomePage(confluencePackage.getSpacesByKey().get(spaceKey));
        for (Long pageId : pages) {
            if (Objects.equals(pageId, homePageId)) {
                logger.warn("The home page (id: [{}]) of space [{}] is a child of another page, "
                        + "not sending it a second time", pageId, spaceKey);
                continue;
            }
            sendPage(pageId, spaceKey, blog, filter, proxyFilter);
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
            userName = confluenceConverter.resolveUserName(userKey);
        }

        if (!StringUtils.isEmpty(userName)) {
            users = (users.isEmpty() ? "" : users + ",") + confluenceConverter.toUserReference(userName);
        }
        return users;
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
            proxyFilter.onWikiObjectProperty("allow", "1", FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty("groups", group, FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty("levels", right.getName(), FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty("users", users, FilterEventParameters.EMPTY);
        } finally {
            proxyFilter.endWikiObject(rightClassName, rightParameters);
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

        pageIdentifier.setPageTitle(pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE));
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

    private Map<String, Object> createPageIdentifier(ConfluenceProperties pageProperties)
    {
        String spaceKey = null;
        Long spaceId = pageProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_SPACE, null);
        if (spaceId != null) {
            try {
                spaceKey = confluencePackage.getSpaceKey(spaceId);
            } catch (ConfigurationException e) {
                this.logger.error(PAGE_IDENTIFIER_ERROR, pageProperties.getLong(ID), e);
            }
        }
        return createPageIdentifier(pageProperties, spaceKey);
    }

    private Map<String, Object> createPageIdentifier(ConfluenceProperties pageProperties, String spaceKey)
    {
        Long pageId = pageProperties.getLong(ID);
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
        ConfluenceFilter proxyFilter) throws FilterException, ConfluenceInterruptedException
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

        try {
            Collection<ConfluenceRight> inheritedRights = sendTerminalDoc(blog, filter, proxyFilter, documentName,
                documentParameters, pageProperties, spaceKey, isHomePage, children);

            if (isHomePage) {
                // We only send inherited rights of the home page so they are added to the space's WebPreference page
                homePageInheritedRights = inheritedRights;
            }

            if (!children.isEmpty()) {
                sendPages(spaceKey, false, children, filter, proxyFilter);
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
        String documentName, FilterEventParameters documentParameters, ConfluenceProperties pageProperties,
        String spaceKey, boolean isHomePage, List<Long> children) throws FilterException, ConfluenceCanceledException
    {
        this.progress.startStep(this);
        // > WikiDocument
        proxyFilter.beginWikiDocument(documentName, documentParameters);

        Collection<ConfluenceRight> inheritedRights = null;
        try {
            if (this.properties.isContentsEnabled() || this.properties.isRightsEnabled()) {
                inheritedRights = sendRevisions(blog, filter, proxyFilter, pageProperties, spaceKey);
            }
        } finally {
            // < WikiDocument
            proxyFilter.endWikiDocument(documentName, documentParameters);

            if (!isHomePage) {
                sendWebPreference(proxyFilter, pageProperties, children, inheritedRights);
            }

            if (!macrosIds.isEmpty()) {
                if (properties.isVerbose()) {
                    logger.info(ConfluenceFilter.LOG_MACROS_FOUND, "The following macros [{}] were found on page [{}].",
                        macrosIds, createPageIdentifier(pageProperties));
                }
                macrosIds.clear();
            }
            if (this.remainingPages > 0) {
                this.remainingPages--;
            }
            this.progress.endStep(this);
        }

        return isHomePage ? inheritedRights : null;
    }

    private void sendWebPreference(ConfluenceFilter proxyFilter, ConfluenceProperties pageProperties,
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

    private Collection<ConfluenceRight> sendRevisions(boolean blog, Object filter, ConfluenceFilter proxyFilter,
        ConfluenceProperties pageProperties, String spaceKey) throws FilterException, ConfluenceCanceledException
    {
        Locale locale = Locale.ROOT;

        FilterEventParameters documentLocaleParameters = getDocumentLocaleParameters(pageProperties);

        // > WikiDocumentLocale
        proxyFilter.beginWikiDocumentLocale(locale, documentLocaleParameters);

        Collection<ConfluenceRight> inheritedRights;
        try {
            // Revisions
            if (properties.isHistoryEnabled() && pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISIONS)) {
                List<Long> revisions =
                    this.confluencePackage.getLongList(pageProperties, ConfluenceXMLPackage.KEY_PAGE_REVISIONS);
                Collections.sort(revisions);
                for (Long revisionId : revisions) {
                    if (shouldSendObject(revisionId)) {
                        ConfluenceProperties revisionProperties = getPageProperties(revisionId);
                        if (revisionProperties == null) {
                            this.logger.warn("Can't find page revision with id [{}]", revisionId);
                            continue;
                        }

                        try {
                            readPageRevision(revisionProperties, blog, filter, proxyFilter, spaceKey);
                        } catch (Exception e) {
                            logger.error("Failed to filter the page revision with id [{}]",
                                createPageIdentifier(revisionId, spaceKey), e);
                        }
                        checkCanceled();
                    }
                }
            }

            // Current version
            // Note: no need to check whether the object should be sent. Indeed, this is already checked by an upper
            // function
            inheritedRights = readPageRevision(pageProperties, blog, filter, proxyFilter, spaceKey);
        } finally {
            // < WikiDocumentLocale
            proxyFilter.endWikiDocumentLocale(locale, documentLocaleParameters);
        }

        return inheritedRights;
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
            String authorName = confluenceConverter.getReferenceFromUserKey(authorKey);
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

    private Collection<ConfluenceRight> sendPageRights(ConfluenceFilter proxyFilter,
        ConfluenceProperties pageProperties) throws FilterException
    {
        Collection<ConfluenceRight> inheritedRights = new ArrayList<>();
        for (Object permissionSetIdObject : ConfluenceXMLPackage.getContentPermissionSets(pageProperties)) {
            long permissionSetId = toLong(permissionSetIdObject);
            ConfluenceProperties permissionSetProperties = getPermissionSetProperties(pageProperties, permissionSetId);
            if (permissionSetProperties == null) {
                continue;
            }

            sendPageRight(proxyFilter, pageProperties, permissionSetProperties, permissionSetId, inheritedRights);
        }
        return inheritedRights;
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
                if (Right.VIEW.equals(right)) {
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
            ConfluenceProperties props = this.confluencePackage.getPageProperties(pageId, false);
            if (props == null || props.getLong(ConfluenceXMLPackage.KEY_ID, null) == null) {
                // Null ID can happen when the home page is missing. ConfluenceXMLPackage has set the homePage property
                // when parsing the space, but the page id and any other property is missing. This means the page
                // isn't actually there.
                return null;
            }
            return props;
        } catch (ConfigurationException e) {
            throw new FilterException("Failed to get page properties", e);
        }
    }

    private Collection<ConfluenceRight> readPageRevision(ConfluenceProperties pageProperties, boolean blog,
        Object filter, ConfluenceFilter proxyFilter, String spaceKey) throws FilterException
    {
        // beware. Here, pageProperties might not have a space key. You need to use the one passed in parameters
        // FIXME we could ensure it though with some work

        Long pageId = pageProperties.getLong(ID, null);
        if (pageId == null) {
            throw new FilterException("Found a null revision id in space [" + spaceKey + "], this should not happen.");
        }

        if (this.properties.isVerbose()) {
            this.logger.info(SEND_PAGE_MARKER, "Sending page [{}]", createPageIdentifier(pageProperties, spaceKey));
        }

        // pageId is used as a fallback, an empty revision would prevent the revision from going through.
        String revision = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION, pageId.toString());

        FilterEventParameters docRevisionParameters = new FilterEventParameters();

        prepareRevisionMetadata(pageProperties, docRevisionParameters);

        beginPageRevision(blog, pageProperties, filter, proxyFilter, revision, docRevisionParameters);

        Collection<ConfluenceRight> inheritedRights = null;

        if (this.properties.isRightsEnabled()) {
            inheritedRights = sendPageRights(proxyFilter, pageProperties);
        }

        try {
            readAttachments(pageId, pageProperties, proxyFilter);
            readTags(pageProperties, proxyFilter);
            readComments(pageProperties, proxyFilter);
            storeConfluenceDetails(spaceKey, pageId, pageProperties, proxyFilter);
        } finally {
            // < WikiDocumentRevision
            proxyFilter.endWikiDocumentRevision(revision, docRevisionParameters);
        }
        return inheritedRights;
    }

    private void beginPageRevision(boolean isBlog, ConfluenceProperties pageProperties,
        Object filter, ConfluenceFilter proxyFilter, String revision, FilterEventParameters docRevisionParameters)
        throws FilterException
    {
        String bodyContent = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_BODY, null);
        if (bodyContent != null && this.properties.isContentsEnabled()) {
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
                    this.confluencePackage.getDate(pageProperties, ConfluenceXMLPackage.KEY_PAGE_REVISION_DATE);
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
            String authorName = confluenceConverter.getReferenceFromUserKey(authorKey);
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

    private void readComments(ConfluenceProperties pageProperties, ConfluenceFilter proxyFilter) throws FilterException
    {
        Map<Long, ConfluenceProperties> pageComments = new LinkedHashMap<>();
        Map<Long, Integer> commentIndices = new LinkedHashMap<>();
        int commentIndex = 0;
        for (Long commentId : confluencePackage.getPageComments(pageProperties)) {
            ConfluenceProperties commentProperties = getCommentProperties(pageProperties, commentId);
            if (commentProperties == null) {
                continue;
            }

            pageComments.put(commentId, commentProperties);
            commentIndices.put(commentId, commentIndex);
            commentIndex++;
        }

        Set<String> resolvedComments = new HashSet<>();
        for (Long commentId : pageComments.keySet()) {
            readPageComment(pageProperties, proxyFilter, commentId, pageComments, commentIndices, resolvedComments);
        }
    }

    private void readTags(ConfluenceProperties pageProperties, ConfluenceFilter proxyFilter) throws FilterException
    {
        if (!this.properties.isTagsEnabled()) {
            return;
        }

        Map<String, ConfluenceProperties> pageTags = new LinkedHashMap<>();
        for (Object tagIdStringObject : pageProperties.getList(ConfluenceXMLPackage.KEY_PAGE_LABELLINGS)) {
            Long tagId = Long.parseLong((String) tagIdStringObject);
            ConfluenceProperties tagProperties = getTagProperties(pageProperties, tagId);
            if (tagProperties == null) {
                continue;
            }

            String tagName = this.confluencePackage.getTagName(tagProperties);
            if (tagName == null) {
                logger.warn("Failed to get the name of label id [{}] for the page with id [{}].", tagId,
                    createPageIdentifier(pageProperties));
            } else {
                pageTags.put(tagName, tagProperties);
            }
        }

        if (!pageTags.isEmpty()) {
            readPageTags(proxyFilter, pageTags);
        }
    }

    private ConfluenceProperties getTagProperties(ConfluenceProperties pageProperties, Long tagId)
        throws FilterException
    {
        if (!shouldSendObject(tagId)) {
            return null;
        }

        try {
            return this.confluencePackage.getObjectProperties(tagId);
        } catch (ConfigurationException e) {
            logger.error("Failed to get tag properties [{}] for the page with id [{}].", tagId,
                createPageIdentifier(pageProperties), e);
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

    private void readAttachments(Long pageId, ConfluenceProperties pageProperties, ConfluenceFilter proxyFilter)
        throws FilterException
    {
        if (!this.properties.isAttachmentsEnabled()) {
            return;
        }

        Map<String, ConfluenceProperties> pageAttachments = new LinkedHashMap<>();
        for (Long attachmentId : this.confluencePackage.getAttachments(pageId)) {
            ConfluenceProperties attachmentProperties = getAttachmentProperties(pageId, attachmentId, pageProperties);
            if (attachmentProperties == null
                || "deleted".equalsIgnoreCase(
                    attachmentProperties.getString(ConfluenceXMLPackage.KEY_PAGE_CONTENT_STATUS))
                || StringUtils.isNotEmpty(
                    attachmentProperties.getString(ConfluenceXMLPackage.KEY_PAGE_ORIGINAL_VERSION))
            ) {
                continue;
            }

            String attachmentName = this.confluencePackage.getAttachmentName(attachmentProperties);

            ConfluenceProperties currentAttachmentProperties = pageAttachments.get(attachmentName);
            if (currentAttachmentProperties == null) {
                pageAttachments.put(attachmentName, attachmentProperties);
            } else {
                keepMostRecentAttachment(pageId, pageProperties, attachmentId, currentAttachmentProperties,
                    attachmentProperties, attachmentName, pageAttachments);
            }
        }

        for (ConfluenceProperties attachmentProperties : pageAttachments.values()) {
            readAttachment(pageId, pageProperties, attachmentProperties, proxyFilter);
        }
    }

    private void keepMostRecentAttachment(Long pageId, ConfluenceProperties pageProperties, Long attachmentId,
        ConfluenceProperties currentAttachmentProperties, ConfluenceProperties attachmentProperties,
        String attachmentName, Map<String, ConfluenceProperties> pageAttachments)
    {
        if (keepOriginalVersion(currentAttachmentProperties, attachmentProperties, attachmentName, pageAttachments)) {
            return;
        }

        Long currentAttachmentId = currentAttachmentProperties
            .getLong(ConfluenceXMLPackage.KEY_ID, null);

        if (keepAttachmentWithHighestRevision(pageId, currentAttachmentProperties, attachmentProperties,
            attachmentName, pageAttachments, currentAttachmentId, attachmentId)
        ) {
            return;
        }

        Date date = getDate(pageProperties, attachmentId, attachmentProperties);
        Date currentDate = getDate(pageProperties, currentAttachmentId, currentAttachmentProperties);

        if (date == null) {
            if (currentDate == null) {
                logger.warn(
                    "For attachment [{}] in page [{}], failed to get the date of both attachment id [{}] "
                        + " and [{}] to determine which one is more recent, will try using the revision string",
                    attachmentName, pageId, attachmentId, currentAttachmentId);

            } else {
                logger.warn(
                    "For attachment [{}] in page [{}], failed to get the date of attachment id [{}] "
                        + " to determine if it is more recent than [{}], will keep the latter.",
                    attachmentName, pageId, attachmentId, currentAttachmentId);
                return;
            }
            if (currentAttachmentId == null || attachmentId > currentAttachmentId) {
                pageAttachments.put(attachmentName, attachmentProperties);
            }
        } else if (date.after(currentDate)) {
            pageAttachments.put(attachmentName, attachmentProperties);
        }
    }

    private static boolean keepOriginalVersion(ConfluenceProperties currentAttachmentProperties,
        ConfluenceProperties attachmentProperties, String attachmentName,
        Map<String, ConfluenceProperties> pageAttachments)
    {
        if (currentAttachmentProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISIONS)
            || attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_ORIGINAL_VERSION)) {
            return true;
        }

        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISIONS)
            || currentAttachmentProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_ORIGINAL_VERSION)
        ) {
            pageAttachments.put(attachmentName, attachmentProperties);
            return true;
        }
        return false;
    }

    private boolean keepAttachmentWithHighestRevision(Long pageId, ConfluenceProperties currentAttachmentProperties,
        ConfluenceProperties attachmentProperties, String attachmentName,
        Map<String, ConfluenceProperties> pageAttachments, Long currentAttachmentId, Long attachmentId)
    {
        String revision = attachmentProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION);
        String currentRevision = currentAttachmentProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION);
        if (!StringUtils.equals(revision, currentRevision)) {
            if (StringUtils.isEmpty(revision)) {
                if (StringUtils.isEmpty(currentRevision)) {
                    logger.warn(
                        "For attachment [{}] in page [{}], could not parse the revision of both attachment id [{}] and "
                        + "[{}], will try comparing dates", attachmentName, pageId, attachmentId, currentAttachmentId);
                    return false;
                }

                logger.warn(FAILED_TO_PARSE_ATTACHMENT_REV, attachmentName, pageId, attachmentId, currentAttachmentId);
                return true;
            }

            if (StringUtils.isEmpty(currentRevision)) {
                logger.warn(FAILED_TO_PARSE_ATTACHMENT_REV, attachmentName, pageId, currentAttachmentId, attachmentId);
                pageAttachments.put(attachmentName, attachmentProperties);
                return true;
            }

            try {
                float revisionNumber = Float.parseFloat(revision);
                float currentRevisionNumber = Float.parseFloat(currentRevision);
                if (revisionNumber > currentRevisionNumber) {
                    pageAttachments.put(attachmentName, attachmentProperties);
                }
                return true;
            } catch (NumberFormatException ignore) {
                logger.warn("Could not parse the revision of one of the attachments as float, "
                    + "will keep the highest id");
            }
        }
        return false;
    }

    private Date getDate(ConfluenceProperties pageProperties, Long attachmentId,
        ConfluenceProperties attachmentProperties)
    {
        try {
            return this.confluencePackage.getDate(attachmentProperties,
                ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_DATE);
        } catch (Exception e) {
            this.logger.error("Failed to parse the date of attachment [{}] from page [{}]",
                attachmentId, createPageIdentifier(pageProperties), e);
        }
        return null;
    }

    /**
     * @since 9.13
     */
    private void storeConfluenceDetails(String spaceKey, Long pageId, ConfluenceProperties pageProperties,
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
            proxyFilter.onWikiObjectProperty(ID, pageId, FilterEventParameters.EMPTY);
            long stableId = pageProperties.getLong(ConfluenceXMLPackage.KEY_PAGE_ORIGINAL_VERSION, pageId);
            proxyFilter.onWikiObjectProperty("stableId", stableId, FilterEventParameters.EMPTY);
            StringBuilder pageURLBuilder = new StringBuilder();
            if (!this.properties.getBaseURLs().isEmpty()) {
                pageURLBuilder.append(this.properties.getBaseURLs().get(0).toString());
                pageURLBuilder.append("/spaces/").append(spaceKey);
                if (!pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_HOMEPAGE)) {
                    String pageName = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE);
                    pageURLBuilder.append("/pages/").append(pageId).append('/').append(pageName);
                }
            }
            String title = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE, null);
            proxyFilter.onWikiObjectProperty(TITLE, title, FilterEventParameters.EMPTY);
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

    private void readAttachment(Long pageId, ConfluenceProperties pageProperties,
        ConfluenceProperties attachmentProperties, ConfluenceFilter proxyFilter) throws FilterException
    {
        Long attachmentId = attachmentProperties.getLong(ID);
        // no need to check shouldSendObject(attachmentId), already done by the caller.

        String attachmentName = this.confluencePackage.getAttachmentName(attachmentProperties);

        ConfluenceProperties attachmentContentProperties = attachmentProperties;

        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENTPROPERTIES)) {
            attachmentContentProperties =
                getContentProperties(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENTPROPERTIES);
        }

        long attachmentSize = attachmentContentProperties.getLong(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENT_FILESIZE,
            -1);
        String mediaType = null;
        if (attachmentContentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENT_MEDIA_TYPE)) {
            mediaType = attachmentContentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENT_MEDIA_TYPE);
        } else if (attachmentContentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENTTYPE)) {
            mediaType = attachmentContentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENTTYPE);
        }

        Long version = this.confluencePackage.getAttachementVersion(attachmentProperties);

        long originalRevisionId =
            this.confluencePackage.getAttachmentOriginalVersionId(attachmentProperties, attachmentId);
        File contentFile;
        try {
            contentFile = this.confluencePackage.getAttachmentFile(pageId, originalRevisionId, version);
        } catch (FileNotFoundException e) {
            this.logger.warn("Failed to find file corresponding to version [{}] attachment [{}] in page [{}]",
                version, attachmentName, createPageIdentifier(pageProperties));
            return;
        }

        FilterEventParameters attachmentParameters = getAttachmentParameters(pageProperties, attachmentProperties,
            mediaType, attachmentId, version);

        // WikiAttachment

        try (FileInputStream fis = new FileInputStream(contentFile)) {
            proxyFilter.onWikiAttachment(attachmentName, fis,
                attachmentSize != -1 ? attachmentSize : contentFile.length(), attachmentParameters);
        } catch (Exception e) {
            this.logger.error("Failed to read attachment [{}] for the page [{}].", attachmentId,
                createPageIdentifier(pageProperties), e);
        }
    }

    private FilterEventParameters getAttachmentParameters(ConfluenceProperties pageProperties,
        ConfluenceProperties attachmentProperties, String mediaType,
        Long attachmentId, Long version)
    {
        FilterEventParameters attachmentParameters = new FilterEventParameters();
        if (mediaType != null) {
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_CONTENT_TYPE, mediaType);
        }
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_AUTHOR)) {
            String userName = attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_AUTHOR);
            String userReference = confluenceConverter.toUserReference(userName);
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_CREATION_AUTHOR, userReference);
        }
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_DATE)) {
            try {
                attachmentParameters.put(WikiAttachmentFilter.PARAMETER_CREATION_DATE, this.confluencePackage
                    .getDate(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_DATE));
            } catch (Exception e) {
                this.logger.error("Failed to parse the creation date of the attachment [{}] in page [{}]",
                    attachmentId, createPageIdentifier(pageProperties), e);
            }
        }

        attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION, String.valueOf(version));
        fillAttachmentAuthor(attachmentProperties, attachmentParameters);
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_DATE)) {
            try {
                attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION_DATE, this.confluencePackage
                    .getDate(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_DATE));
            } catch (Exception e) {
                this.logger.error("Failed to parse the revision date of the attachment [{}] in page [{}]",
                    attachmentId, createPageIdentifier(pageProperties), e);
            }
        }
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_COMMENT)) {
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION_COMMENT,
                attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_COMMENT));
        }
        return attachmentParameters;
    }

    private void fillAttachmentAuthor(ConfluenceProperties attachmentProperties,
        FilterEventParameters attachmentParameters)
    {
        String creatorName = null;
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_AUTHOR_KEY)) {
            String creatorKey = attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_AUTHOR_KEY);
            creatorName = confluenceConverter.resolveUserName(creatorKey);
        } else if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_AUTHOR)) {
            creatorName = attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_AUTHOR);
        }

        if (creatorName != null) {
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION_AUTHOR,
                confluenceConverter.toUserReference(creatorName));
        }
    }

    private void readPageTags(ConfluenceFilter proxyFilter, Map<String, ConfluenceProperties> pageTags)
        throws FilterException
    {
        FilterEventParameters pageTagsParameters = new FilterEventParameters();

        // Tag object
        pageTagsParameters.put(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, TAGS_CLASSNAME);
        proxyFilter.beginWikiObject(TAGS_CLASSNAME, pageTagsParameters);
        try {
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
        } finally {
            proxyFilter.endWikiObject(TAGS_CLASSNAME, pageTagsParameters);
        }
    }

    private void readPageComment(ConfluenceProperties pageProperties, ConfluenceFilter proxyFilter, Long commentId,
        Map<Long, ConfluenceProperties> pageComments, Map<Long, Integer> commentIndices, Set<String> resolvedComments)
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
            String commentCreatorReference = commentProperties.containsKey(CREATOR_NAME)
                ? confluenceConverter.toUserReference(commentProperties.getString(CREATOR_NAME))
                : confluenceConverter.getReferenceFromUserKey(commentProperties.getString("creator"));

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
            Date commentDate = null;
            try {
                commentDate = this.confluencePackage.getDate(commentProperties, "creationDate");
            } catch (Exception e) {
                this.logger.error("Failed to parse the creation date of the comment [{}] in page [{}]",
                    commentId, createPageIdentifier(pageProperties), e);
            }

            // parent (replyto)
            Integer parentIndex = null;
            if (commentProperties.containsKey(PARENT)) {
                Long parentId = commentProperties.getLong(PARENT);
                parentIndex = commentIndices.get(parentId);
            }

            proxyFilter.onWikiObjectProperty("author", commentCreatorReference, FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty("comment", commentText, FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty("date", commentDate, FilterEventParameters.EMPTY);
            proxyFilter.onWikiObjectProperty("replyto", parentIndex, FilterEventParameters.EMPTY);
        } finally {
            proxyFilter.endWikiObject(COMMENTS_CLASSNAME, commentParameters);
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

        if (!commentProperties.containsKey(ConfluenceXMLPackage.KEY_COMMENT_CONTENTPROPERTIES)) {
            return false;
        }

        ConfluenceProperties contentProps =
            getContentProperties(commentProperties, ConfluenceXMLPackage.KEY_COMMENT_CONTENTPROPERTIES);

        if (contentProps == null || (!contentProps.containsKey("inline-comment")
            && !contentProps.getString("actualCommentType", "").equals("inline")))
        {
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
}
