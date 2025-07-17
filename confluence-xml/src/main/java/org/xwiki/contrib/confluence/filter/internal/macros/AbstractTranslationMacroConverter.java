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
package org.xwiki.contrib.confluence.filter.internal.macros;

import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.syntax.Syntax;

abstract class AbstractTranslationMacroConverter extends AbstractParseContentMacroConverter
{
    @Inject
    private ConfluenceInputContext context;

    @Override
    public void toXWiki(String id, Map<String, String> parameters, String content, boolean inline, Listener listener)
    {
        if (context.getProperties().isTranslationsEnabled()) {
            toXWikiWithTranslationsEnabled(id, parameters, content, inline, listener);
        } else {
            toXWikiWithTranslationsDisabled(id, parameters, content, inline, listener);
        }
    }

    protected void toXWikiWithTranslationsDisabled(String id, Map<String, String> parameters, String content,
        boolean inline, Listener listener)
    {
        super.toXWiki(id, parameters, content, inline, listener);
    }

    protected void toXWikiWithTranslationsEnabled(String id, Map<String, String> parameters, String content,
        boolean inline, Listener listener)
    {
        // marks the language parameter as handled
        Locale language = getLanguage(id, parameters, content, inline);

        context.addUsedLocale(language);

        Locale currentLanguage = context.getCurrentLocale();
        if (currentLanguage == null) {
            super.toXWiki(id, parameters, content, inline, listener);
            return;
        }

        if (!currentLanguage.equals(language)) {
            // Languages don't match, we drop the macro
            return;
        }

        sendContent(id, content, listener);
    }

    protected void sendContent(String id, String content, Listener listener)
    {
        // We import the content without any macro
        ConfluenceInputProperties inputProperties = context.getProperties();
        Syntax macroContentSyntax = inputProperties == null ? null : inputProperties.getMacroContentSyntax();
        String syntaxId = macroContentSyntax != null ? macroContentSyntax.toIdString() : Syntax.XWIKI_2_1.toIdString();
        parseContent(id, listener, syntaxId, content);
    }
}
