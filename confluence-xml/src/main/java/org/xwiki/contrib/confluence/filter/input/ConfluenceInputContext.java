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

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Provide various info about the current context.
 * 
 * @version $Id$
 * @since 9.7
 */
@Role
public interface ConfluenceInputContext
{
    /**
     * @return the properties controlling the behavior of the Confluence input filter stream
     */
    ConfluenceInputProperties getProperties();

    /**
     * @return the Confluence package being filtered
     * @since 9.26.0
     */
    ConfluenceXMLPackage getConfluencePackage();

    /**
     * @return the Confluence space being filtered
     * @since 9.35.0
     */
    default String getCurrentSpace()
    {
        return null;
    }

    /**
     * @return the Confluence page being filtered
     * @since 9.47.0
     */
    default Long getCurrentPage()
    {
        return null;
    }

    /**
     * @return whether the Confluence instance is a cloud instance
     */
    default boolean isConfluenceCloud()
    {
        return "cloud".equals(getProperties().getConfluenceInstanceType());
    }

    /**
     * @return the cached reference
     * @param pageId the page id corresponding to the cached reference to find
     * @param valueGetter the function to call to get the value if not found (the result will be added to the cache)
     * @since 9.68.0
     */
    default EntityReference getCachedReference(long pageId, Supplier<EntityReference> valueGetter)
    {
        return null;
    }

    /**
     * @return the cached reference
     * @param spaceKey the spaceKey corresponding to the cached reference to find
     * @param pageTitle the spaceKey corresponding to the cached reference to find
     * @param valueGetter the function to call to get the value if not found (the result will be added to the cache)
     * @since 9.68.0
     */
    default EntityReference getCachedReference(String spaceKey, String pageTitle, Supplier<EntityReference> valueGetter)
    {
        return null;
    }

    /**
     * @return the language in which content is to be analysed, or null if the language should not be taken in
     *         account.
     *         If set, the content of language macros matching this language
     *         is supposed to be imported and content of language macros not matching this language is supposed to be
     *        dropped. If not set, macros should be imported as is.
     * @since 9.88.0
     */
    default Locale getCurrentLocale()
    {
        return null;
    }

    /**
     * Advertise the use of a language in the current content.
     * @param language the language to advertise
     * @since 9.88.0
     */
    default void addUsedLocale(Locale language)
    {
        // ignore
    }

    /**
     * @return the ISO 639‑1 or IETF language tag of the languages that were advertized as used in the current
     * content.
     * @since 9.88.0
     */
    default Collection<Locale> getCurrentlyUsedLocales()
    {
        return null;
    }

    /**
     * @param language the language being processed
     * @since 9.88.0
     */
    default void setCurrentLocale(Locale language)
    {
        // ignore
    }

    /**
     * @return the default locale
     * @since 9.88.0
     */
    default Locale getDefaultLocale()
    {
        return Locale.ROOT;
    }
}
