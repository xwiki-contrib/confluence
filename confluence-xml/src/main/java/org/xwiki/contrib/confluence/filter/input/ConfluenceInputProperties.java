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

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.xwiki.contrib.confluence.filter.Mapping;
import org.xwiki.filter.DefaultFilterStreamProperties;
import org.xwiki.filter.input.InputSource;
import org.xwiki.properties.annotation.PropertyDescription;
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
     * @see #getSpacePageName()
     */
    private String spacePageName = "WebHome";

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
    private String unknownMacroPrefix = "confluence_";

    private Set<String> prefixedMacros;

    private Set<String> unprefixedMacros;

    private Mapping userIdMapping;

    /**
     * @see #isStoreConfluenceDetailsEnabled()
     */
    private boolean storeConfluenceDetailsEnabled;

    /**
     * @see #isBlogsEnabled()
     */
    private boolean blogsEnabled = true;

    /**
     * @see #getBlogSpaceName()
     */
    private String blogSpaceName = "Blog";

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
     */
    @PropertyName("Space home page")
    @PropertyDescription("The name to use for space home page")
    public String getSpacePageName()
    {
        return this.spacePageName;
    }

    /**
     * @param spacePageName The name to use for space home page
     */
    public void setSpacePageName(String spacePageName)
    {
        this.spacePageName = spacePageName;
    }

    /**
     * @return the base URLs
     * @since 9.1
     */
    @PropertyName("Base URLs")
    @PropertyDescription("The list of base URLs leading to the Confluence instance."
        + " They are used to convert wrongly entered absoulte URLs into wiki links."
        + " The first URL in the list will be used to compute page URLs used in the conversion report if the"
        + " the 'Store Confluence details' property is used")
    public List<URL> getBaseURLs()
    {
        return this.baseURLs;
    }

    /**
     * @param baseURLs the base URLs
     * @since 9.1
     */
    public void setBaseURLs(List<URL> baseURLs)
    {
        this.baseURLs = baseURLs;
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
    @PropertyDescription("The Confluence identifiers of the pages to read from the input package.")
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
    @PropertyDescription("The Confluence identifiers of the pages to skip from the input package.")
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
    @PropertyDescription("Import the users and groups found in the confluence package.")
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
     * @return the wiki where to imports users
     * @since 9.11
     */
    @PropertyName("Users wiki")
    @PropertyDescription("The wiki where to import users.")
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
    @PropertyDescription("A mapping between Confluence user id located in the package and wanted ids.")
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
}
