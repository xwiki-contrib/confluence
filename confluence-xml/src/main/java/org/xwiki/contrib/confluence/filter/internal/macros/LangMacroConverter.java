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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Singleton;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;

/**
 * Language Macro Converter.
 * @since 9.88.0
 * @version $Id$
 */
@Component (hints = {
    "belarusian",
    "bulgarian",
    "canadian-en",
    "canadian-fr",
    "catalan",
    "chinese",
    "croatian",
    "czech",
    "danish",
    "dutch",
    "english",
    "english-us",
    "estonian",
    "french",
    "german",
    "greek",
    "hindi",
    "hungarian",
    "icelandic",
    "indonesian",
    "irish",
    "italian",
    "japanese",
    "korean",
    "latvian",
    "lithuanian",
    "maltese",
    "norwegian",
    "norwegian-nb",
    "polish",
    "portuguese",
    "portuguese-br",
    "romanian",
    "russian",
    "slovak",
    "slovenian",
    "spanish",
    "thai",
    "turkish",
    "ukrainian",
    "vietnamese",
    "welsh"
})
@Singleton
public class LangMacroConverter extends AbstractTranslationMacroConverter
{
    private static final Map<String, String> LANGUAGE_NAME_TO_CODE = new HashMap<>();
    static {
        LANGUAGE_NAME_TO_CODE.put("belarusian", "be");
        LANGUAGE_NAME_TO_CODE.put("bulgarian", "bg");
        LANGUAGE_NAME_TO_CODE.put("canadian-en", "en_CA");
        LANGUAGE_NAME_TO_CODE.put("canadian-fr", "fr_CA");
        LANGUAGE_NAME_TO_CODE.put("catalan", "ca");
        LANGUAGE_NAME_TO_CODE.put("chinese", "zh");
        LANGUAGE_NAME_TO_CODE.put("croatian", "hr");
        LANGUAGE_NAME_TO_CODE.put("czech", "cs");
        LANGUAGE_NAME_TO_CODE.put("danish", "da");
        LANGUAGE_NAME_TO_CODE.put("dutch", "nl");
        LANGUAGE_NAME_TO_CODE.put("english", "en");
        LANGUAGE_NAME_TO_CODE.put("english-us", "en_US");
        LANGUAGE_NAME_TO_CODE.put("estonian", "et");
        LANGUAGE_NAME_TO_CODE.put("french", "fr");
        LANGUAGE_NAME_TO_CODE.put("german", "de");
        LANGUAGE_NAME_TO_CODE.put("greek", "el");
        LANGUAGE_NAME_TO_CODE.put("hindi", "hi");
        LANGUAGE_NAME_TO_CODE.put("hungarian", "hu");
        LANGUAGE_NAME_TO_CODE.put("icelandic", "is");
        LANGUAGE_NAME_TO_CODE.put("indonesian", "id");
        LANGUAGE_NAME_TO_CODE.put("irish", "ga");
        LANGUAGE_NAME_TO_CODE.put("italian", "it");
        LANGUAGE_NAME_TO_CODE.put("japanese", "ja");
        LANGUAGE_NAME_TO_CODE.put("korean", "ko");
        LANGUAGE_NAME_TO_CODE.put("latvian", "lv");
        LANGUAGE_NAME_TO_CODE.put("lithuanian", "lt");
        LANGUAGE_NAME_TO_CODE.put("maltese", "mt");
        LANGUAGE_NAME_TO_CODE.put("norwegian", "nn");
        LANGUAGE_NAME_TO_CODE.put("norwegian-nb", "nb");
        LANGUAGE_NAME_TO_CODE.put("polish", "pl");
        LANGUAGE_NAME_TO_CODE.put("portuguese", "pt");
        LANGUAGE_NAME_TO_CODE.put("portuguese-br", "pt_BR");
        LANGUAGE_NAME_TO_CODE.put("romanian", "ro");
        LANGUAGE_NAME_TO_CODE.put("russian", "ru");
        LANGUAGE_NAME_TO_CODE.put("slovak", "sk");
        LANGUAGE_NAME_TO_CODE.put("slovenian", "sl");
        LANGUAGE_NAME_TO_CODE.put("spanish", "es");
        LANGUAGE_NAME_TO_CODE.put("thai", "th");
        LANGUAGE_NAME_TO_CODE.put("turkish", "tr");
        LANGUAGE_NAME_TO_CODE.put("ukrainian", "uk");
        LANGUAGE_NAME_TO_CODE.put("vietnamese", "vi");
        LANGUAGE_NAME_TO_CODE.put("welsh", "cy");
    }

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return "contentTranslation";
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        return Map.of("language", LANGUAGE_NAME_TO_CODE.getOrDefault(confluenceId, confluenceId));
    }

    @Override
    public Locale getLanguage(String id, Map<String, String> parameters, String content, boolean inline)
    {
        String l = LANGUAGE_NAME_TO_CODE.get(id);
        if (StringUtils.isEmpty(l)) {
            return null;
        }

        try {
            return LocaleUtils.toLocale(l.split("_")[0]);
        } catch (IllegalArgumentException e) {
            // Should never happen
            throw new RuntimeException("Failed to compute a Locale for language macro " + id, e);
        }
    }
}
