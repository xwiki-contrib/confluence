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

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.text.StringSubstitutor;

import java.util.HashMap;
import java.util.Map;

/**
 * Clean usernames. Adapted from ldap-authenticator (DefaultLDAPDocumentHelper.java).
 */
final class UsernameCleaner
{
    private UsernameCleaner()
    {
        // empty
    }

    private static String clean(String str)
    {
        return RegExUtils.removePattern(str, "[\\.\\:\\s,@\\^\\/]");
    }

    private static void putVariable(Map<String, String> map, String key, String value)
    {
        if (value != null) {
            map.put(key, value);

            map.put(key + ".lowerCase", value.toLowerCase());
            map.put(key + "._lowerCase", value.toLowerCase());
            map.put(key + ".upperCase", value.toUpperCase());
            map.put(key + "._upperCase", value.toUpperCase());

            String cleanValue = clean(value);
            map.put(key + ".clean", cleanValue);
            map.put(key + "._clean", cleanValue);
            map.put(key + ".clean.lowerCase", cleanValue.toLowerCase());
            map.put(key + "._clean._lowerCase", cleanValue.toLowerCase());
            map.put(key + ".clean.upperCase", cleanValue.toUpperCase());
            map.put(key + "._clean._upperCase", cleanValue.toUpperCase());
        }
    }

    private static Map<String, String> createFormatMap(Map<String, String> variables)
    {
        Map<String, String> formatMap = new HashMap<>();
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            putVariable(formatMap, variable.getKey(), variable.getValue());
        }
        return formatMap;
    }

    private static String formatSubject(StringSubstitutor substitutor, String format)
    {
        return substitutor.replace(format);
    }

    static String format(String format, Map<String, String> variables)
    {
        return formatSubject(new StringSubstitutor(createFormatMap(variables)), format);
    }
}
