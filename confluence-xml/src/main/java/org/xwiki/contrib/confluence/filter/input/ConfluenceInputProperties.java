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
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
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
     * @see getUnknownMacroPrefix()
     */
    private String unknownMacroPrefix = "confluence_";

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
     * @return if true, convert various Confluence standards to XWiki standard (the name of the admin group, etc.)
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
        + " They are used to convert wrongly entered absoulte URL into wiki links.")
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
}
