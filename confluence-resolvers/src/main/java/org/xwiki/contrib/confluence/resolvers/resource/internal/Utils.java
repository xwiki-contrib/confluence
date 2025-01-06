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
package org.xwiki.contrib.confluence.resolvers.resource.internal;

final class Utils
{
    private Utils()
    {
        // ignore
    }

    static String getAnchor(String reference, int hash)
    {
        return (hash + 1 < reference.length())
            ? reference.substring(hash + 1).split("\\s")[0]
            : null;
    }

    static String unescape(String s)
    {
        StringBuilder res = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') {
                i++;
                if (i >= s.length()) {
                    break;
                }
                c = s.charAt(i);
            }
            res.append(c);
            i++;
        }
        return res.toString();
    }

    static int indexOf(String s, char end, int beginIndex)
    {
        int i = beginIndex;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == end) {
                return i;
            }
            if (c == '\\') {
                i++;
                if ((i >= s.length())) {
                    return i;
                }
            }
            i++;
        }
        return i;
    }
}
