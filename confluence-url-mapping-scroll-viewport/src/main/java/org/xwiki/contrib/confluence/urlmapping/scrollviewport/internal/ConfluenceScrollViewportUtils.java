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
package org.xwiki.contrib.confluence.urlmapping.scrollviewport.internal;

import java.util.List;

final class ConfluenceScrollViewportUtils
{
    // List taken from https://help.k15t.com/scroll-viewport-data-center/2.22.0/configure-global-url-redirects
    public static final List<String> EXCLUDED_PREFIX_LIST =
        List.of("admin", "ajax", "display", "download", "favicon.ico", "images", "includes", "jcaptcha", "json",
            "label", "login.action", "noop.jsp", "pages", "plugins", "rest", "rpc", "s", "spaces", "status", "styles",
            "synchrony", "synchrony-proxy", "x");

    private ConfluenceScrollViewportUtils()
    {
    }
}
