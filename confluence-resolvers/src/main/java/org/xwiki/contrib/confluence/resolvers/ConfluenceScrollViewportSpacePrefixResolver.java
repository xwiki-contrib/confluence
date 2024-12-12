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
package org.xwiki.contrib.confluence.resolvers;

import java.util.Map;

import org.xwiki.component.annotation.Role;

/**
 * Scroll viewport space prefix resolver.
 *
 * @version $Id$
 * @since 9.67.0
 */
@Role
public interface ConfluenceScrollViewportSpacePrefixResolver
{
    /**
     * Return a map entry with the space viewport prefix and the space name.
     *
     * @param fullUrl the full url from which to find the matching space.
     * @return a map entry which contains the viewport space prefix as key and the value which contains the space name.
     */
    Map.Entry<String, String> getSpaceAndPrefixForUrl(String fullUrl) throws ConfluenceResolverException;
}
