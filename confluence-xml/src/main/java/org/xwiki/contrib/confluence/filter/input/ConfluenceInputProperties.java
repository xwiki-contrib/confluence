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
package org.xwiki.contrib.confluence.filter.input;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.contrib.confluence.filter.Mapping;
import org.xwiki.contrib.confluence.filter.internal.idrange.ConfluenceIdRangeList;
import org.xwiki.filter.DefaultFilterStreamProperties;
import org.xwiki.filter.input.InputSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyHidden;
import org.xwiki.properties.annotation.PropertyMandatory;
import org.xwiki.properties.annotation.PropertyName;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Confluence XML input properties.
 * 
 * @version $Id$
 * @since 9.0
 */
public class ConfluenceInputProperties extends DefaultFilterStreamProperties
{
    private static final String XWIKI_ADMIN_GROUP_NAME = "XWikiAdminGroup";
    private static final String XWIKI_ALL_GROUP_NAME = "XWikiAllGroup";
    private static final String CLEANUP_SYNC = "SYNC";
    private static final String WEB_HOME = "WebHome";
    private static final ConfluenceOverwriteProtectionModeType DEFAULT_OVERWRITE_PROTECTION_MODE =
        ConfluenceOverwriteProtectionModeType.NONCONFLUENCE;
    private static final String CONFLUENCE_UNDERSCORE = "confluence_";
    private static final String DEFAULT_GROUP_FORMAT = "${group._clean}";
    private static final String DEFAULT_SPACE_RENAMING_FORMAT = "${spaceKey}_";
    private static final String NONE = "NONE";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfluenceInputProperties.class);

    /**
     * @see #getSource()
     */
    private InputSource source;

    /**
     * @see #getDefaultLocale()
     */
    private Locale defaultLocale;

    /**
     * @see #isContentEvents()
     */
    private boolean contentEvents;

    /**
     * @see #isConvertToXWiki()
     */
    private boolean convertToXWiki = true;

    /**
     * @see #isEntityNameValidation()
     */
    private boolean entityNameValidation = true;

    /**
     * @see #getBaseURLs()
     */
    private List<URL> baseURLs;

    /**
     * @see #getMacroContentSyntax()
     */
    private Syntax macroContentSyntax;

    /**
     * @see #getIncludedPages()
     */
    private Set<Long> includedPages;

    /**
     * @see #getExcludedPages()
     */
    private Set<Long> excludedPages;

    /**
     * @see #isUsersEnabled()
     */
    private boolean usersEnabled = true;

    /**
     * @see #isGroupsEnabled()
     */
    private boolean groupsEnabled = true;

    /**
     * @see #isUserReferences()
     */
    private boolean userReferences;

    /**
     * @see #getUsersWiki()
     */
    private String usersWiki;

    /**
     * @see #getUnknownMacroPrefix()
     */
    private String unknownMacroPrefix = CONFLUENCE_UNDERSCORE;

    /**
     * @see #getUnknownMacroPrefix()
     */
    private String keptMacroParameterPrefix = CONFLUENCE_UNDERSCORE;

    private String macroParameterKeepingMode = NONE;

    private Set<String> prefixedMacros;

    private Set<String> unprefixedMacros;

    private Mapping userIdMapping;

    private ConfluenceIdRangeList objectIdRanges;

    private boolean spaceTitleFromHomePage;

    private boolean useConfluenceResolvers = true;

    private Mapping groupMapping = new Mapping(Map.of(
        "confluence-administrators", XWIKI_ADMIN_GROUP_NAME,
        "administrators", XWIKI_ADMIN_GROUP_NAME,
        "site-admins", XWIKI_ADMIN_GROUP_NAME,
        "system-administrators", XWIKI_ADMIN_GROUP_NAME,
        "confluence-users", XWIKI_ALL_GROUP_NAME,
        "users", XWIKI_ALL_GROUP_NAME,
        "_licensed-confluence", ""
    ));

    private Mapping groupIdMapping = new Mapping();

    /**
     * @see #isStoreConfluenceDetailsEnabled()
     */
    private boolean storeConfluenceDetailsEnabled = true;

    /**
     * @see #isBlogsEnabled()
     */
    private boolean blogsEnabled = true;

    /**
     * @see #isNonBlogContentEnabled()
     */
    private boolean nonBlogContentEnabled = true;

    /**
     * @see #isRightsEnabled()
     */
    private boolean rightsEnabled = true;

    /**
     * @see #isContentsEnabled()
     */
    private boolean contentsEnabled = true;

    /**
     * @see #isHistoryEnabled()
     */
    private boolean historyEnabled = true;

    /**
     * @see #isArchivedDocumentsEnabled()
     */
    private boolean archivedDocumentsEnabled;

    /**
     * @see #isArchivedSpacesEnabled()
     */
    private boolean archivedSpacesEnabled;

    /**
     * @see #getCleanup()
     */
    private String cleanup = CLEANUP_SYNC;

    /**
     * @see #getBlogSpaceName()
     */
    private String blogSpaceName = "Blog";

    /**
     * @see #getTemplateSpaceName()
     */
    private String templateSpaceName = "Templates";

    /**
     * @see #isTemplateProvidersEnabled()
     */
    private boolean templateProvidersEnabled = true;

    /**
     * @see #getRoot()
     */
    private EntityReference root;

    /**
     * @see #isAttachmentsEnabled()
     */
    private boolean attachmentsEnabled = true;

    /**
     * @see #isTagsEnabled()
     */
    private boolean tagsEnabled = true;

    /**
     * @see #getMaxPageCount()
     */
    private int maxPageCount = -1;

    /**
     * @see #getGroupFormat()
     */
    private String groupFormat = DEFAULT_GROUP_FORMAT;

    /**
     * @see #getUserFormat()
     */
    private String userFormat;

    /*
     * @see #getWorkingDirectory()
     */
    private String workingDirectory;

    /**
     * @see #getLinkMapping()
     */
    private Map<String, Map<String, EntityReference>> linkMapping = Collections.emptyMap();

    /**
     * @see #getConfluenceInstanceType()
     */
    private String confluenceInstanceType;

    /**
     * @see #isTitleAnchorGenerationEnabled()
     */
    private boolean generateTitleAnchors = true;

    /**
     * @see #isPageOrderEnabled()
     */
    private boolean pageOrderEnabled = true;

    /**
     * @see #isExtraneousSpacesEnabled()
     */
    private boolean extraneousSpacesEnabled;

    /**
     * @see #isSkipResolvedInlineComments()
     */
    private boolean skipResolvedInlineComments;

    /**
     * @see #isTranslationsEnabled()
     */
    private boolean translationsEnabled = true;

    /**
     * @see #getOrphanMode()
     */
    private String orphanMode = "NORMAL";

    private boolean favoritesEnabled = true;

    /**
     * @see #getOverwriteProtectedSpaces()
     */
    private Set<String> overwriteProtectedSpaces;

    /**
     * @see #getSpaceRenamingFormat()
     */
    private String spaceRenamingFormat = DEFAULT_SPACE_RENAMING_FORMAT;

    /**
     * @see #getOverwriteProtectionMode()
     */
    private ConfluenceOverwriteProtectionModeType overwriteProtectionMode =
        ConfluenceOverwriteProtectionModeType.NONCONFLUENCE;

    /**
     * @return The source to load the wiki from
     */
    @PropertyName("The source")
    @PropertyDescription("The source to load the wiki from")
    @PropertyMandatory
    public InputSource getSource()
    {
        return this.source;
    }

    /**
     * @param source The source to load the wiki from
     */
    public void setSource(InputSource source)
    {
        this.source = source;
    }

    /**
     * @return The locale of the documents
     */
    @PropertyName("Default locale")
    @PropertyDescription("The locale of the documents")
    public Locale getDefaultLocale()
    {
        return defaultLocale;
    }

    /**
     * @param defaultLocale The locale of the documents
     */
    public void setDefaultLocale(Locale defaultLocale)
    {
        this.defaultLocale = defaultLocale;
    }

    /**
     * @return if true, the content will be parsed to produce rendering events
     * @since 9.3
     */
    @PropertyName("Produce rendering events for the content")
    @PropertyDescription("Parse the content to produce rendering events (if the output filter supports them)")
    public boolean isContentEvents()
    {
        return this.contentEvents;
    }

    /**
     * @param contentEvents if true, the content will be parsed to produce rendering events
     * @since 9.3
     */
    public void setContentEvents(boolean contentEvents)
    {
        this.contentEvents = contentEvents;
    }

    /**
     * @return true to convert various Confluence standards to XWiki standard (the name of the admin group, etc.)
     */
    @PropertyName("XWiki conversion")
    @PropertyDescription("Convert various Confluence standards to XWiki standard (the name of the admin group, etc.)")
    public boolean isConvertToXWiki()
    {
        return this.convertToXWiki;
    }

    /**
     * @param convertToXWiki if true, convert various Confluence standards to XWiki standard (the name of the admin
     *            group, etc.)
     */
    public void setConvertToXWiki(boolean convertToXWiki)
    {
        this.convertToXWiki = convertToXWiki;
    }

    /**
     * @return true if the standard XWiki entity name validation should be applied
     * @since 9.15
     */
    @PropertyName("Page name validation")
    @PropertyDescription("Apply the standard page name validator (if XWiki conversion is enabled)")
    public boolean isEntityNameValidation()
    {
        return isConvertToXWiki() && this.entityNameValidation;
    }

    /**
     * @param entityNameValidation true if the standard XWiki entity name validation should be applied
     * @since 9.15
     */
    public void setEntityNameValidation(boolean entityNameValidation)
    {
        this.entityNameValidation = entityNameValidation;
    }

    /**
     * @return The name to use for space home page
     * Note: previously, this was incorrectly referring to the space description. This is fixed since version 9.35.0.
     * @deprecated since 9.46.0
     */
    @PropertyName("Space home page")
    @PropertyDescription("The name to use for space home page")
    @Deprecated (since = "9.46.0")
    @PropertyHidden
    public String getSpacePageName()
    {
        return WEB_HOME;
    }

    /**
     * @param ignored is ignored
     * @deprecated since 9.46.0
     */
    @Deprecated (since = "9.46.0")
    public void setSpacePageName(String ignored)
    {
        // ignore
    }

    /**
     * @return whether redirect documents should be output for home pages.
     * @since 9.35.0
     * @deprecated since 9.46.0
     */
    @PropertyName("Home redirects")
    @PropertyDescription("Produce redirects for home pages")
    @Deprecated (since = "9.46.0")
    @PropertyHidden
    public boolean isHomeRedirectEnabled()
    {
        return false;
    }

    /**
     * @param ignored is ignored
     * @since 9.35.0
     * @deprecated since 9.46.0
     */
    @Deprecated (since = "9.46.0")
    public void setHomeRedirectEnabled(boolean ignored)
    {
        // ignore
    }

    /**
     * @return the base URLs
     * @since 9.1
     */
    @PropertyName("Base URLs")
    @PropertyDescription("The list of base URLs leading to the Confluence instance."
        + " They are used to convert wrongly entered absolute URLs into wiki links."
        + " The first URL in the list will be used to compute page URLs used in the conversion report if"
        + " the 'Store Confluence details' property is used."
        + " URLs should contain the particle which leads to confluence, if any, ofter /wiki."
        + " Ending slashes don't matter, URLs will be normalized."
        + " Example: https://example.com/wiki,https://confluence.example.com")
    public List<URL> getBaseURLs()
    {
        return Objects.requireNonNullElse(this.baseURLs, Collections.emptyList());
    }

    /**
     * @param baseURLs the base URLs
     * @since 9.1
     */
    public void setBaseURLs(List<URL> baseURLs)
    {
        this.baseURLs = baseURLs.stream().map(u -> {
            try {
                return new URL(StringUtils.stripEnd(u.toString(), "/"));
            } catch (MalformedURLException e) {
                LOGGER.warn("Could not normalize base URL [{}]", u, e);
                return u;
            }
        }).collect(Collectors.toList());
    }

    /**
     * @return the macroContentSyntax the syntax to use to convert rich macro content
     * @since 9.3
     */
    @PropertyName("Macro content syntax")
    @PropertyDescription("The syntax to use to convert rich macro content. The default is current default syntax.")
    public Syntax getMacroContentSyntax()
    {
        return this.macroContentSyntax;
    }

    /**
     * @param macroContentSyntax the syntax to use to convert rich macro content
     * @since 9.3
     */
    public void setMacroContentSyntax(Syntax macroContentSyntax)
    {
        this.macroContentSyntax = macroContentSyntax;
    }

    /**
     * @return the Confluence identifiers of the pages to read from the input package
     * @since 9.4
     */
    @PropertyName("Included pages")
    @PropertyDescription("The Confluence identifiers of the pages and/or templates to import from the input package.")
    public Set<Long> getIncludedPages()
    {
        return this.includedPages;
    }

    /**
     * @param includedPages the Confluence identifiers of the pages to read from the input package
     * @since 9.4
     */
    public void setIncludedPages(Set<Long> includedPages)
    {
        this.includedPages = includedPages;
    }

    /**
     * @return the Confluence identifiers of the pages to skip from the input package
     * @since 9.4
     */
    @PropertyName("Excluded pages")
    @PropertyDescription("The Confluence identifiers of the pages and/or templates to skip from the input package.")
    public Set<Long> getExcludedPages()
    {
        return this.excludedPages;
    }

    /**
     * @param excludedPages the Confluence identifiers of the pages to skip from the input package
     * @since 9.4
     */
    public void setExcludedPages(Set<Long> excludedPages)
    {
        this.excludedPages = excludedPages;
    }

    /**
     * @param pageId the Confluence identifier of the page
     * @return true if the page should be read, false otherwise
     * @since 9.4
     */
    public boolean isIncluded(long pageId)
    {
        if (CollectionUtils.isNotEmpty(this.includedPages)) {
            return this.includedPages.contains(pageId);
        }

        if (CollectionUtils.isNotEmpty(this.excludedPages)) {
            return !this.excludedPages.contains(pageId);
        }

        return true;
    }

    /**
     * @return if true, import the users and groups found in the confluence package
     * @since 9.6
     */
    @PropertyName("Import users")
    @PropertyDescription("Import the users found in the confluence package.")
    public boolean isUsersEnabled()
    {
        return this.usersEnabled;
    }

    /**
     * @param usersEnabled if true, import the users and groups found in the confluence package
     * @since 9.6
     */
    public void setUsersEnabled(boolean usersEnabled)
    {
        this.usersEnabled = usersEnabled;
    }

    /**
     * @return if true, import the groups found in the confluence package
     * @since 9.38.0
     */
    @PropertyName("Import groups")
    @PropertyDescription("Import the groups found in the confluence package.")
    public boolean isGroupsEnabled()
    {
        return this.groupsEnabled;
    }

    /**
     * @param groupsEnabled if true, import the users and groups found in the confluence package
     * @since 9.38.0
     */
    public void setGroupsEnabled(boolean groupsEnabled)
    {
        this.groupsEnabled = groupsEnabled;
    }

    /**
     * @return indicate if the links to users should produce links to XWiki users profile pages or use the {@code user:}
     *         prefix.
     * @since 9.6
     */
    @PropertyName("Produce user references")
    @PropertyDescription("Indicate if the links to users should produce links to XWiki users profile pages "
        + "or use the \"user:\" prefix.")
    public boolean isUserReferences()
    {
        return this.userReferences;
    }

    /**
     * @param userReferences if the links to users should produce links to XWiki users profile pages or use the
     *            {@code user:} prefix.
     * @since 9.6
     */
    public void setUserReferences(boolean userReferences)
    {
        this.userReferences = userReferences;
    }

    /**
     * @return the prefix to use in the name of the macros for which no converter is registered
     * @since 9.7
     */
    @PropertyName("Unknown macro prefix")
    @PropertyDescription("The prefix to use in the name of the macros for which no converter is registered.")
    public String getUnknownMacroPrefix()
    {
        return this.unknownMacroPrefix;
    }

    /**
     * @param unknownMacroPrefix the prefix to use in the name of the macros for which no converter is registered
     * @since 9.7
     */
    public void setUnknownMacroPrefix(String unknownMacroPrefix)
    {
        this.unknownMacroPrefix = unknownMacroPrefix;
    }

    /**
     * @return the prefix to use in the name of the macros parameters that are kept as-is
     * @since 9.84.0
     */
    @PropertyName("Kept macro parameter prefix")
    @PropertyDescription("The prefix to use in the name of the macro parameters that are kept as-is "
        + "(except for atlassian-macro-output-type and for parameters that happen to be already prefixed with this "
        + "string: those will be kept unprefixed)")
    public String getKeptMacroParameterPrefix()
    {
        return this.keptMacroParameterPrefix;
    }

    /**
     * @param keptMacroParameterPrefix the prefix to use in the name of the macros parameters that are kept as-is
     * @since 9.84.0
     */
    public void setKeptMacroParameterPrefix(String keptMacroParameterPrefix)
    {
        this.keptMacroParameterPrefix = keptMacroParameterPrefix == null ? "" : keptMacroParameterPrefix;
    }

    /**
     * @return the prefix to use in the name of the macros parameters that are kept as-is
     * @since 9.84.0
     */
    @PropertyName("Macro parameter keeping mode")
    @PropertyDescription("Which macro parameter to keep as is with the specified prefix. NONE: don't keep Confluence "
        + "macro parameters. UNHANDLED: keep macro parameters that are not handled and normally kept during macro "
        + "conversion. ALL: keep all the parameters, even those ")
    public String getMacroParameterKeepingMode()
    {
        return this.macroParameterKeepingMode;
    }

    /**
     * @param macroParameterKeepingMode the prefix to use in the name of the macros parameters that are kept as-is
     * @since 9.84.0
     */
    public void setMacroParameterKeepingMode(String macroParameterKeepingMode)
    {
        String mode = StringUtils.isEmpty(macroParameterKeepingMode)
            ? NONE
            : macroParameterKeepingMode.toUpperCase();

        if (mode.equals("UNHANDLED") || mode.equals("ALL") || mode.equals(NONE)) {
            this.macroParameterKeepingMode = mode;
        } else {
            LOGGER.error("Unexpected Kept macro parameter mode [{}], will default to NONE", mode);
            this.macroParameterKeepingMode = NONE;
        }
    }
    /**
     * @return the unknown macros for which the name should be prefixed
     * @since 9.8
     */
    @PropertyName("Prefixed macros")
    @PropertyDescription("The unknown macros for which the name should be prefixed.")
    public Set<String> getPrefixedMacros()
    {
        return this.prefixedMacros != null ? this.prefixedMacros : Collections.emptySet();
    }

    /**
     * @param prefixedMacros the unknown macros for which the name should be prefixed
     * @since 9.8
     */
    public void setPrefixedMacros(Set<String> prefixedMacros)
    {
        this.prefixedMacros = prefixedMacros;
    }

    /**
     * @return the unknown macros for which the name should not be prefixed
     * @since 9.8
     */
    @PropertyName("Unprefixed macros")
    @PropertyDescription("The unknown macros for which the name should not be prefixed.")
    public Set<String> getUnprefixedMacros()
    {
        return this.unprefixedMacros != null ? this.unprefixedMacros : Collections.emptySet();
    }

    /**
     * @param unprefixedMacros the unknown macros for which the name should not be prefixed
     * @since 9.8
     */
    public void setUnprefixedMacros(Set<String> unprefixedMacros)
    {
        this.unprefixedMacros = unprefixedMacros;
    }

    /**
     * @return the wiki where users and groups are located
     * @since 9.11
     */
    @PropertyName("Users wiki")
    @PropertyDescription("The wiki in which users and groups are located. "
        + "If applicable, users will be imported in this wiki; any user "
        + "reference, including ones in permissions, will include this wiki. "
        + "If empty, local references will be used.")
    public String getUsersWiki()
    {
        return this.usersWiki;
    }

    /**
     * @param usersWiki the wiki where to imports users
     * @since 9.11
     */
    public void setUsersWiki(String usersWiki)
    {
        this.usersWiki = usersWiki;
    }

    /**
     * @return a mapping between Confluence user id located in the package and wanted ids
     * @since 9.11
     */
    @PropertyName("User id mapping")
    @PropertyDescription("A mapping between Confluence user id located in the package and wanted ids. "
        + "The format is confluenceuser=XWikiUser pairs separated by pipes (\"|\"). "
        + "For instance: myuser=MyUser|user2=User2"

    )
    public Mapping getUserIdMapping()
    {
        return this.userIdMapping;
    }

    /**
     * @param existingUsers a mapping between Confluence user id located in the package and wanted ids
     * @since 9.11
     */
    public void setUserIdMapping(Mapping existingUsers)
    {
        this.userIdMapping = existingUsers;
    }

    /**
     * Replace the current Confluence to XWiki group mapping with the given one.
     * @param existingGroups a mapping between Confluence groups and XWiki groups.
     *                       Use the empty string value to ignore a Confluence group.
     * @since 9.24.0
     */
    public void setGroupMapping(Mapping existingGroups)
    {
        this.groupMapping = existingGroups;
    }

    /**
     * Define a mapping between the Confluence group ID and group name. When we do a Confluence space export this
     * information is missing and so the macro parameter which depends on this mapping won't be converted correctly. So
     * this parameter is to provide the missing information.
     *
     * @param groupIdMapping a mapping between Confluence groups ID and group name.
     * @since 9.57.0
     */
    public void setGroupIdMapping(Mapping groupIdMapping)
    {
        this.groupIdMapping = groupIdMapping;
    }

    /**
     * @return a mapping between Confluence group names located in the package and wanted ids
     * @since 9.24.0
     */
    @PropertyName("Group name mapping")
    @PropertyDescription("A mapping between Confluence group names and XWiki group names. "
        + "The format is confluencegroup=XWikiGroup pairs separated by pipes (\"|\"). "
        + "Use an empty value to ignore the group."
        + "For instance: atlassian-addons=|balsamiq-mockups-editors=MockupEditors|"
        + "administrators=XWikiAdminGroup|site-admins=XWikiAdminGroup|_licensed-confluence=|"
        + "confluence-users=XWikiAllGroup|confluence-administrators=XWikiAdminGroup|"
        + "system-administrators=XWikiAdminGroup|group 1=MyGroup")
    public Mapping getGroupMapping()
    {
        return this.groupMapping;
    }

    /**
     * @return a mapping between Confluence groups ID and group name.
     * @since 9.57.0
     */
    @PropertyName("Group id mapping")
    @PropertyDescription("Define a mapping between Confluence group IDs and Confluence group names. When using a "
        + "Confluence space export, this information is missing and macro parameters which refer to group ids won't "
        + "be converted correctly. This parameter helps work around this issue by providing the missing information."
        + "For instance: a39e82d8-1c93-4395-9358-dc67f2ffa3ef=balsamiq-mockups-editors|"
        + "a39e82d8-1c93-4395-9358-dc67f2ffa3ef=administrators|a78122a5-a46d-497e-904c-3cffd763de31=site-admins")
    public Mapping getGroupIdMapping()
    {
        return this.groupIdMapping;
    }

    /**
     * @return if true, add XWiki object to mark documents as Confluence migrated
     * @since 9.13
     */
    @PropertyName("Store Confluence details")
    @PropertyDescription("Store Confluence details in each migrated page")
    public boolean isStoreConfluenceDetailsEnabled()
    {
        return this.storeConfluenceDetailsEnabled;
    }

    /**
     * @param storeConfluenceDetails if true, add XWiki object to the migrated documents
     * @since 9.13
     */
    public void setStoreConfluenceDetailsEnabled(boolean storeConfluenceDetails)
    {
        this.storeConfluenceDetailsEnabled = storeConfluenceDetails;
    }

    /**
     * @return if true, import the blog posts found in the confluence package
     * @since 9.24.0
     */
    @PropertyName("Import blog posts")
    @PropertyDescription("Import the blog posts found in the confluence package.")
    public boolean isBlogsEnabled()
    {
        return this.blogsEnabled;
    }

    /**
     * @param blogsEnabled if true, import the blog posts found in the confluence package
     * @since 9.24.0
     */
    public void setBlogsEnabled(boolean blogsEnabled)
    {
        this.blogsEnabled = blogsEnabled;
    }

    /**
     * @return if true, import the non-blog content found in the confluence package
     * @since 9.25.0
     */
    @PropertyName("Import non-blog contents")
    @PropertyDescription("Import the non-blog contents found in the confluence package.")
    public boolean isNonBlogContentEnabled()
    {
        return this.nonBlogContentEnabled;
    }

    /**
     * @param nonBlogContentEnabled if true, import the blog posts found in the confluence package
     * @since 9.25.0
     */
    public void setNonBlogContentEnabled(boolean nonBlogContentEnabled)
    {
        this.nonBlogContentEnabled = nonBlogContentEnabled;
    }

    /**
     * @return if true, import the rights found in the confluence package
     * @since 9.24.0
     */
    @PropertyName("Import rights")
    @PropertyDescription("Import the rights found in the confluence package.")
    public boolean isRightsEnabled()
    {
        return this.rightsEnabled;
    }

    /**
     * @param rightsEnabled if true, import the rights found in the confluence package
     * @since 9.24.0
     */
    public void setRightsEnabled(boolean rightsEnabled)
    {
        this.rightsEnabled = rightsEnabled;
    }

    /**
     * @return if true, import the archived documents found in the confluence package
     * @since 9.31.0
     */
    @PropertyName("Import archived documents")
    @PropertyDescription("Import the archived documents found in the confluence package.")
    public boolean isArchivedDocumentsEnabled()
    {
        return this.archivedDocumentsEnabled;
    }

    /**
     * @param archivedDocumentsEnabled if true, import the rights found in the confluence package
     * @since 9.31.0
     */
    public void setArchivedDocumentsEnabled(boolean archivedDocumentsEnabled)
    {
        this.archivedDocumentsEnabled = archivedDocumentsEnabled;
    }

    /**
     * @return if true, import the archived spaces found in the confluence package
     * @since 9.31.0
     */
    @PropertyName("Import archived spaces")
    @PropertyDescription("Import the archived spaces found in the confluence package.")
    public boolean isArchivedSpacesEnabled()
    {
        return this.archivedSpacesEnabled;
    }

    /**
     * @param archivedSpacesEnabled if true, import the archived spaces found in the confluence package
     * @since 9.31.0
     */
    public void setArchivedSpacesEnabled(boolean archivedSpacesEnabled)
    {
        this.archivedSpacesEnabled = archivedSpacesEnabled;
    }

    /**
     * @return if true, import the contents found in the confluence package
     * @since 9.24.0
     */
    @PropertyName("Import contents")
    @PropertyDescription("Import the contents found in the confluence package.")
    public boolean isContentsEnabled()
    {
        return this.contentsEnabled;
    }

    /**
     * @param contentsEnabled if true, import the contents found in the confluence package
     * @since 9.24.0
     */
    public void setContentsEnabled(boolean contentsEnabled)
    {
        this.contentsEnabled = contentsEnabled;
    }

    /**
     * @return if true, import the contents found in the confluence package
     * @since 9.24.0
     */
    @PropertyName("Import history")
    @PropertyDescription("Import history (all the revisions) found in the confluence package.")
    public boolean isHistoryEnabled()
    {
        return this.historyEnabled;
    }

    /**
     * @param historyEnabled if true, import the history found in the confluence package
     * @since 9.24.0
     */
    public void setHistoryEnabled(boolean historyEnabled)
    {
        this.historyEnabled = historyEnabled;
    }

    /**
     * @return The name to use for blog space
     * @since 9.24.0
     */
    @PropertyName("Blog Space name")
    @PropertyDescription("The name to use for blog space")
    public String getBlogSpaceName()
    {
        return this.blogSpaceName;
    }

    /**
     * @param blogSpaceName The name to use for blog space
     * @since 9.24.0
     */
    public void setBlogSpaceName(String blogSpaceName)
    {
        this.blogSpaceName = blogSpaceName;
    }

    /**
     * @return The name to use for blog space
     * @since 9.79.0
     */
    @PropertyName("Template space name")
    @PropertyDescription("The name to use to import templates")
    public String getTemplateSpaceName()
    {
        return this.templateSpaceName;
    }

    /**
     * @param templateSpaceName The name to use for templates space
     * @since 9.79.0
     */
    public void setTemplateSpaceName(String templateSpaceName)
    {
        this.templateSpaceName = templateSpaceName;
    }

    /**
     * @return The name to use for blog space
     * @since 9.79.0
     */
    @PropertyName("Generate Template Providers")
    @PropertyDescription("Whether to create a template provider for each imported template")
    public boolean isTemplateProvidersEnabled()
    {
        return this.templateProvidersEnabled;
    }

    /**
     * @param templateProvidersEnabled whether to enable template provider generation
     * @since 9.79.0
     */
    public void setTemplateProvidersEnabled(boolean templateProvidersEnabled)
    {
        this.templateProvidersEnabled = templateProvidersEnabled;
    }

    /**
     * @return The name to use for the root space
     * @since 9.32.0
     * @deprecated since 9.60.0
     */
    @PropertyName("Root space name")
    @PropertyDescription("The name to use for the space in which pages will be imported")
    @Deprecated (since = "9.60.0")
    @PropertyHidden
    public SpaceReference getRootSpace()
    {
        return this.root == null ? null : new SpaceReference(this.root);
    }

    /**
     * @param rootSpace The name to use for the root space
     * @since 9.32.0
     * @deprecated since 9.60.0
     */
    @Deprecated (since = "9.60.0")
    @PropertyHidden
    public void setRootSpace(SpaceReference rootSpace)
    {
        this.root = rootSpace;
    }

    /**
     * @return The name to use for the root wiki/space
     * @since 9.60.0
     */
    @PropertyName("Root")
    @PropertyDescription("The wiki or space in which pages will be imported. Examples: "
        + "wiki:sub, space:sub:RootInSubWiki, MyRootInCurrentWiki, My.Migration, sub:My.MigrationInSubSpace. "
        + "Note: Make sure your set 'Users wiki' accordingly.")
    public EntityReference getRoot()
    {
        return this.root;
    }

    /**
     * @param root The name to use for the root wiki / space
     * @since 9.60.0
     */
    public void setRoot(EntityReference root)
    {
        EntityReference r = root;
        if (r != null && r.getType() == EntityType.DOCUMENT) {
            String name = r.getName();
            r = WEB_HOME.equals(name)
                ? r.getParent()
                : new EntityReference(name, EntityType.SPACE, r.getParent());
        }
        this.root = r;
    }

    /**
     * @return the cleanup mode.
     * @since 9.33.0
     */
    @PropertyName("Cleanup mode")
    @PropertyDescription("The mode to use for cleaning up temporary files produced when parsing the Confluence package."
        + "SYNC: clean up right after the filter stream is done. "
        + "ASYNC: same, but asynchronously. "
        + "NO: don't clean up at all")
    public String getCleanup()
    {
        return this.cleanup;
    }

    /**
     * @param cleanup The cleanup mode to use
     * @since 9.33.0
     */
    public void setCleanup(String cleanup)
    {
        this.cleanup = cleanup == null ? CLEANUP_SYNC : cleanup.toUpperCase();
    }

    /**
     * @return the object id range.
     * @since 9.35.0
     */
    @PropertyName("Object ID ranges")
    @PropertyDescription("Ranges of Confluence objects to read. Can be used to restore an interrupted migration. "
        + "Several comma-separated ranges can be given. "
        + "Note that the order used for these ranges are not increasingly big ids, but in the order they are processed "
        + "by the Confluence module. This order may change between versions of the parser, "
        + "but is guaranteed to be the same between different runs using the same version of the Confluence module. "
        + "Ranges must not overlap. Overlapping ranges are not supported, may lead to surprising results and their "
        + "behavior is not guaranteed to be stable. In the same vain, ranges must be ordered in the parsing order. "
        + "Examples: "
        + "[4242,] - only read object id 4242 and all the following ones; "
        + "(4242,] - same, but exclude object id 4242; "
        + "[,4242] - read all objects until object id 4242 included; "
        + "[,4242) - same, but exclude 4242; "
        + "[4242,2424], [3456,1234] -  read objects between 4242 and 2424 both included, "
        + "then ignore objects until 5656 and read objects between 5656 and 1234 both included "
        + "(notice how IDs may look disordered).")
    public ConfluenceIdRangeList getObjectIdRanges()
    {
        return objectIdRanges;
    }

    /**
     * Set the object id range list.
     * @param objectIdRanges the list of id ranges to set
     * @since 9.35.0
     */
    public void setObjectIdRanges(ConfluenceIdRangeList objectIdRanges)
    {
        this.objectIdRanges = objectIdRanges;
    }


    /**
     * @return whether attachments are imported.
     * @since 9.35.0
     */
    @PropertyName("Import attachments")
    @PropertyDescription("Import the document attachments.")
    public boolean isAttachmentsEnabled()
    {
        return this.attachmentsEnabled;
    }

    /**
     * @param attachmentsEnabled if true, import the attachments found in the confluence package
     * @since 9.35.0
     */
    public void setAttachmentsEnabled(boolean attachmentsEnabled)
    {
        this.attachmentsEnabled = attachmentsEnabled;
    }

    /**
     * @return whether attachments are imported.
     * @since 9.35.0
     */
    @PropertyName("Import tags")
    @PropertyDescription("Import the document tags.")
    public boolean isTagsEnabled()
    {
        return this.tagsEnabled;
    }

    /**
     * @param tagsEnabled if true, import the tags found in the confluence package
     * @since 9.35.0
     */
    public void setTagsEnabled(boolean tagsEnabled)
    {
        this.tagsEnabled = tagsEnabled;
    }

    /**
     * @return whether spaces are nested during import.
     * Always true, nested space migration is the only supported mode now.
     * @since 9.35.0
     * @deprecated since 9.46.0
     */
    @PropertyName("Nested import")
    @PropertyDescription("Nest spaces during import.")
    @Deprecated (since = "9.46.0")
    @PropertyHidden
    public boolean isNestedSpacesEnabled()
    {
        return true;
    }

    /**
     * Set whether spaces are nested during import. Ignored, nested space migration is the only supported mode now.
     * @param ignored is ignored
     * @since 9.35.0
     * @deprecated since 9.46.0
     */
    @Deprecated (since = "9.46.0")
    public void setNestedSpacesEnabled(boolean ignored)
    {
        // ignored
    }

    /**
     * @return whether spaces should be titled using home page titles
     * @since 9.36.0
     */
    @PropertyName("Title spaces from their home page")
    @PropertyDescription("Title spaces using the Confluence home page titles instead of the Confluence space names")
    public boolean isSpaceTitleFromHomePage()
    {
        return spaceTitleFromHomePage;
    }

    /**
     * @param spaceTitleFromHomePage whether spaces should be titled using the home page titles
     * @since 9.36.0
     */
    public void setSpaceTitleFromHomePage(boolean spaceTitleFromHomePage)
    {
        this.spaceTitleFromHomePage = spaceTitleFromHomePage;
    }

    /**
     * @return the maximum number of pages to read.
     * @since 9.35.0
     */
    @PropertyName("Max page count")
    @PropertyDescription("The maximum number of pages to import (-1 means no limit)")
    public int getMaxPageCount()
    {
        return maxPageCount;
    }

    /**
     * @param maxPageCount the maximum number of pages to import, -1 to disable
     * @since 9.35.0
     */
    public void setMaxPageCount(int maxPageCount)
    {
        this.maxPageCount = maxPageCount;
    }

    /**
     * @return the group format to use
     * @since 9.37.0
     */
    @PropertyName("Group format")
    @PropertyDescription("The format to use to transform a Confluence group name to a XWiki group name for groups"
        + "that are not in the group mapping."
        + "String ${group} will be replaced with the Confluence group name; "
        + "String ${group._clean} same with the special characters removed."
        + "String ${group._lowerCase} will be replaced with the lowercased Confluence group name; "
        + "String ${group._upperCase} will be replaced with the uppercased Confluence group name; "
        + "String ${group._clean._lowerCase} will be replaced with the cleaned, lowercased Confluence group name; "
        + "String ${group._clean._upperCase} will be replaced with the uppercased Confluence group name. "
        + "Default format: " + DEFAULT_GROUP_FORMAT)
    public String getGroupFormat()
    {
        return groupFormat;
    }

    /**
     * @param groupFormat the group format to use
     * @since 9.37.0
     */
    public void setGroupFormat(String groupFormat)
    {
        this.groupFormat = groupFormat;
    }


    /**
     * @return whether to send extraneous spaces
     * @since 9.65.0
     */
    @PropertyName("User format")
    @PropertyDescription("The format to use to transform a Confluence user name to a XWiki user name for users"
        + "that are not in the user id mapping."
        + "String ${username} will be replaced with the Confluence user name; "
        + "String ${username._clean} same with the special characters removed (recommended)."
        + "String ${username._lowerCase} will be replaced with the lowercased Confluence user name; "
        + "String ${username._upperCase} will be replaced with the uppercased Confluence user name; "
        + "String ${username._clean._lowerCase} will be replaced with the cleaned, lowercased Confluence user name; "
        + "String ${username._clean._upperCase} will be replaced with the uppercased Confluence user name. "
        + "By default, for backward compatibility reasons, special characters are replaced with underscores and spaces "
        + "are kept.")
    public String getUserFormat()
    {
        return userFormat;
    }

    /**
     * @param userFormat the format to user
     * @since 9.60.0
     */
    public void setUserFormat(String userFormat)
    {
        this.userFormat = userFormat;
    }

    /**
     * @return the directory used to extract the Confluence package for processing
     * @since 9.38.0
     */
    @PropertyName("Working directory")
    @PropertyDescription("The directory used to extract the Confluence package for processing. "
        + "If an extracted package is found, the analyze will be sped up, "
        + "but this is only supported when using the same version of Confluence XML. Mixing versions is not supported "
        + "and should not be attempted. To use this feature, set the Cleanup mode to NO."
    )
    public String getWorkingDirectory()
    {
        return workingDirectory;
    }

    /**
     * @param workingDirectory the directory to use
     * @since 9.38.0
     */
    public void setWorkingDirectory(String workingDirectory)
    {
        this.workingDirectory = workingDirectory;
    }

    /**
     * @return the link mapping used to produce the correct links to pages not in the Confluence package.
     * @since 9.40.0
     */
    @PropertyName("Link Mapping")
    @PropertyDescription("The link mapping used to produce the correct links to pages missing from the "
        + "Confluence package. "
        + "Format : {"
        + "    \"spaceKey1\": {"
        + "        \"page title 1\": \"Space.Doc1\","
        + "        \"page title 2\": \"Space.Doc2\""
        + "    },"
        + "    \"spaceKey2\": {"
        + "        \"page title 3\": \"Space2.Doc3\","
        + "        \"page title 4\": \"Space2.Doc4\""
        + "    },"
        + "    \"spaceKey:ids\": {"
        + "        \"42\": \"Space.Doc5\""
        + "    },"
        + "    \":ids\": {"
        + "        \"43\": \"Space.Doc6\""
        + "    }"
        + "}"
    )
    public Map<String, Map<String, EntityReference>> getLinkMapping()
    {
        return linkMapping;
    }

    /**
     * @param linkMapping the link mapping used to produce the correct links to pages not in the Confluence package.
     * @since 9.40.0
     */
    public void setLinkMapping(Map<String, Map<String, EntityReference>> linkMapping)
    {
        this.linkMapping = linkMapping;
        if (linkMapping != null) {
            if (!(linkMapping instanceof CaseInsensitiveMap)) {
                this.linkMapping = new CaseInsensitiveMap<>(linkMapping);
            }
            this.linkMapping.replaceAll((k, v) -> v instanceof CaseInsensitiveMap ? v : new CaseInsensitiveMap<>(v));
        }
    }

    /**
     * @return whether Confluence resolvers should be used to resolve links outside the imported package
     * @since 9.64.0
     */
    @PropertyName("Use Confluence Resolvers")
    @PropertyDescription("When there is a link to a page not present in the imported package, use Confluence resolvers "
        + "present in the wiki. ")
    public boolean isUseConfluenceResolvers()
    {
        return this.useConfluenceResolvers;
    }

    /**
     * @param useConfluenceResolvers whether to use Confluence resolvers to resolve links outside the imported package
     */
    public void setUseConfluenceResolvers(boolean useConfluenceResolvers)
    {
        this.useConfluenceResolvers = useConfluenceResolvers;
    }

    /**
     * @return the type of Confluence instance used to produce the backup package being filtered.
     * @since 9.50.0
     */
    @PropertyName("Confluence Instance Type")
    @PropertyDescription("The type of Confluence instance used to produce the backup package being imported. "
        + "There are some differences in how content is managed, anchors for example are not the same. "
        + "Knowing the right Confluence instance will improve the fidelity of the import. "
        + "Detecting the right type of instance will be attempted if an empty value is provided. "
        + "Possible values: SERVER, CLOUD."
    )
    public String getConfluenceInstanceType()
    {
        return confluenceInstanceType;
    }

    /**
     * @param confluenceInstanceType the type to set
     * @since 9.50.0
     */
    public void setConfluenceInstanceType(String confluenceInstanceType)
    {
        this.confluenceInstanceType = confluenceInstanceType;
    }

    /**
     * @return whether to generate automatic anchors for titles, trying tio mimic the ones Confluence would generate.
     * @since 9.50.0
     */
    @PropertyName("Generate title anchors")
    @PropertyDescription("Generate automatic anchors for titles, trying to mimic the ones Confluence would generate.")
    public boolean isTitleAnchorGenerationEnabled()
    {
        return generateTitleAnchors;
    }

    /**
     * @param generateTitleAnchors whether to generate automatic anchors for titles
     * @since 9.50.0
     */
    public void setTitleAnchorGenerationEnabled(boolean generateTitleAnchors)
    {
        this.generateTitleAnchors = generateTitleAnchors;
    }

    /**
     * @return whether to send PinnedChildPagesClass objects to reflect the page ordering in Confluence
     * @since 9.51.0
     */
    @PropertyName("Keep page ordering")
    @PropertyDescription("Use XWiki's pinned pages feature to keep the page ordering from Confluence.")
    public boolean isPageOrderEnabled()
    {
        return pageOrderEnabled;
    }

    /**
     * @param pageOrderEnabled whether to send PinnedChildPagesClass objects to reflect the page ordering in Confluence
     * @since 9.51.0
     */
    public void setPageOrderEnabled(boolean pageOrderEnabled)
    {
        this.pageOrderEnabled = pageOrderEnabled;
    }

    /**
     * @return how to handle orphans
     * @since 9.77.0
     */
    @PropertyName("Orphan mode")
    @PropertyDescription("The mode to use for importing orphans. "
        + "NORMAL: import like normal pages (default). "
        + "HIDE: same, but hide them. "
        + "DISCARD: don't import them at all."
    )
    public String getOrphanMode()
    {
        return orphanMode;
    }

    /**
     * @param orphanMode how to handle orphans
     * @since 9.77.0
     */
    public void setOrphanMode(String orphanMode)
    {
        this.orphanMode = orphanMode;
    }

    /**
     * @return whether to send extraneous spaces
     * @since 9.60.0
     */
    @PropertyName("Import extraneous spaces from the space export")
    @PropertyDescription("Under some conditions, when creating space exports, Confluence incorrectly exports additional"
        + " spaces. This parameter allows importing them as well.")
    public boolean isExtraneousSpacesEnabled()
    {
        return extraneousSpacesEnabled;
    }

    /**
     * @param extraneousSpacesEnabled whether to send extraneous spaces
     * @since 9.60.0
     */
    public void setExtraneousSpacesEnabled(boolean extraneousSpacesEnabled)
    {
        this.extraneousSpacesEnabled = extraneousSpacesEnabled;
    }

    /**
     * @return whether the inline comments that are marked as resolved should be imported as xwiki comments or skipped.
     * @since 9.72.0
     */
    @PropertyName("Skip the inline comments that are marked as resolved")
    @PropertyDescription("All the inline confluence comments are imported by default as xwiki comments. This property"
        + " offers the possibility to not import the inline comments marked as resolved.")
    public boolean isSkipResolvedInlineComments()
    {
        return skipResolvedInlineComments;
    }

    /**
     * @param skipResolvedInlineComments see {@link #isSkipResolvedInlineComments()}.
     * @since 9.72.0
     */
    public void setSkipResolvedInlineComments(boolean skipResolvedInlineComments)
    {
        this.skipResolvedInlineComments = skipResolvedInlineComments;
    }

    /**
     * @return whether pages containing different languages should be converted to translated XWiki documents.
     * @since 9.88.0
     */
    @PropertyName("Translated content support")
    @PropertyDescription("Convert pages containing translations to a XWiki translated document per language")
    public boolean isTranslationsEnabled()
    {
        return translationsEnabled;
    }

    /**
     * @param translationsEnabled whether to enable translations
     * @since 9.88.0
     */
    public void setTranslationsEnabled(boolean translationsEnabled)
    {
        this.translationsEnabled = translationsEnabled;
    }

    /**
     * @return whether pages containing different languages should be converted to translated XWiki documents.
     * @since 9.88.0
     */
    @PropertyName("Favorites")
    @PropertyDescription("Import favorites (requires the Favorites Application)")
    public boolean isFavoritesEnabled()
    {
        return favoritesEnabled;
    }

    /**
     * @param favoritesEnabled whether to enable translations
     * @since 9.88.0
     */
    public void setFavoritesEnabled(boolean favoritesEnabled)
    {
        this.favoritesEnabled = favoritesEnabled;
    }

    /**
     * @return a static set of XWiki spaces that should not be overwritten during an import.
     * @since 9.89.0
     */
    @PropertyName("Overwrite-protected spaces")
    @PropertyDescription(
        "A comma-separated list of XWiki spaces that should not be overwritten during a Confluence import."
            + " If a Confluence space collides with one of these references,"
            + " it will be imported at a different location"
            + " by renaming the Confluence space key according to the Space Renaming Format property."
            + " If the renamed space conflicts, underscores will be added as needed."
            + " Each space name must include the full path, including the wiki.")
    public Set<String> getOverwriteProtectedSpaces()
    {
        return overwriteProtectedSpaces;
    }

    /**
     * @param overwriteProtectedSpaces the static set of XWiki spaces that should not be overwritten during an import.
     * @since 9.89.0
     */
    public void setOverwriteProtectedSpaces(Set<String> overwriteProtectedSpaces)
    {
        this.overwriteProtectedSpaces = overwriteProtectedSpaces;
    }

    /**
     * @return the space renaming format to use for resolving name conflicts between spaces
     * @since 9.89.0
     */
    @PropertyName("Space renaming format")
    @PropertyDescription("The format to use when renaming a space in case of conflict. "
        + "The ${spaceKey} placeholder will be replaced with the Confluence space name.")
    public String getSpaceRenamingFormat()
    {
        return spaceRenamingFormat == null ? "" : spaceRenamingFormat;
    }

    /**
     * @param spaceRenamingFormat the space renaming format to apply in case of a space name conflict
     * @since 9.89.0
     */
    public void setSpaceRenamingFormat(String spaceRenamingFormat)
    {
        this.spaceRenamingFormat = spaceRenamingFormat;
    }

    /**
     * @return the Overwrite Protection Mode for the spaces that have a name conflict and are not present in the static
     *     set of overwrite-protected spaces Forbidden Spaces list
     * @since 9.89.0
     */
    @PropertyName("Overwrite protection mode")
    @PropertyDescription("The protection mode to use for the Confluence spaces that have a conflict with a XWiki space."
        + "space. Possible values: "
        + "NONE - no overwrite protection: don't rename any space;"
        + "NONCONFLUENCE - only rename spaces conflicting with existing XWiki spaces not imported from Confluence;"
        + "ANY - rename any space conflicting with an existing XWiki space.")
    public ConfluenceOverwriteProtectionModeType getOverwriteProtectionMode()
    {
        return overwriteProtectionMode;
    }

    /**
     * Sets the overwrite protection mode to use for spaces that have a name conflict and are not included in the
     * Forbidden Spaces list.
     *
     * @param overwriteProtectionMode the overwrite protection mode to apply for conflicting spaces
     * @since 9.89.0
     */
    public void setOverwriteProtectionMode(ConfluenceOverwriteProtectionModeType overwriteProtectionMode)
    {
        this.overwriteProtectionMode = overwriteProtectionMode;
    }
}
