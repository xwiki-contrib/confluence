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

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * Convert the translation macro.
 *
 * @version $Id$
 * @since 9.75.0
 */
@Singleton
@Component
@Named("sv-translation")
public class TranslationMacroConverter extends AbstractMacroConverter
{
    private static final String MACRO_ID = "contentTranslation";
    
    private static final String MACRO_PARAMETER_LANGUAGE = "language";

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return MACRO_ID;
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.YES;
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Throwable cause = null;

        String language = confluenceParameters.get(MACRO_PARAMETER_LANGUAGE);
        if (confluenceParameters == null || language == null) {
            // we throw a runtime exception so the macro is prevented from being converted, as it doesn't make sense
            // to convert it if we can't resolve the language. A post migration fix will then be possible using
            // something like the "Replace macros using Macro Converters from XDOM" snippet
            throw new RuntimeException(String.format("Could not get the language from id [{}]", confluenceId),
                cause);
        }

        return Map.of(MACRO_PARAMETER_LANGUAGE, language);
    }

}
