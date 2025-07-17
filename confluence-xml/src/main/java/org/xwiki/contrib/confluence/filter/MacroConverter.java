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
package org.xwiki.contrib.confluence.filter;

import java.util.Locale;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.WrappingListener;

/**
 * Converter Confluence standard macros to XWiki equivalent.
 * 
 * @version $Id$
 * @since 9.1
 */
@Role
public interface MacroConverter
{
    /**
     * Macro inline support information.
     */
    enum InlineSupport
    {
        /**
         * Inline is not supported.
         */
        NO,

        /**
         * Inline is supported.
         */
        YES,

        /**
         * Inline support is not known.
         */
        MAYBE
    }

    /**
     * Convert passed macro to the XWiki equivalent.
     * 
     * @param id the Confluence macro id (eg "toc" for the TOC macro)
     * @param parameters the macro parameters (which can be an unmodifiable map, so please don't attempt to modify it)
     * @param content the macro content
     * @param inline if true the macro is located in an inline content (like paragraph, etc.)
     * @param listener the listener to send events to
     */
    void toXWiki(String id, Map<String, String> parameters, String content, boolean inline, Listener listener);

    /**
     * @return whether the macro supports inline
     * @param id the macro id (eg "toc" for the TOC macro)
     * @param parameters the macro parameters
     * @param content the macro content
     */
    default InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.MAYBE;
    }

    /**
     * @return the id of the macro after conversion given the parameters, the content and whether the context is inline.
     *         The default implementation uses toXWiki to return the converted id.
     * @param id the Confluence macro id
     * @param parameters the macro parameters
     * @param content the macro content
     * @param inline whether the macro is being converted in an inline context
     * @since 9.49.0
     */
    default String toXWikiId(String id, Map<String, String> parameters, String content, boolean inline)
    {
        String[] mid = new String[1];
        toXWiki(id, parameters, content, inline,
            new WrappingListener()
            {
                @Override
                public void onMacro(String id, Map<String, String> parameters, String content, boolean inline)
                {
                    if (mid[0] == null) {
                        mid[0] = id;
                    }
                }
            });
        return mid[0];
    }

    /**
     * @return the language this macro is restricted to (for instance, if it is a language macro), or null if the
     * macro is not restricted to a particular language.
     * If the conversion is performed under a specific language, toXWiki will only be called if getLanguage() returns
     * the requested language or null.
     * @param id the Confluence macro id
     * @param parameters the macro parameters
     * @param content the macro content
     * @param inline whether the macro is being converted in an inline context
     * @since 9.88.0
     */
    default Locale getLanguage(String id, Map<String, String> parameters, String content, boolean inline)
    {
        return null;
    }
}
