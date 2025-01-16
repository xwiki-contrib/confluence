package org.xwiki.contrib.confluence.resolvers;
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

import java.util.Map;

import org.xwiki.component.annotation.Role;

/**
 * Get the the configuration of the given translation language, for the page with the given id.
 *
 * @version $Id$
 * @since 9.75.0
 */
@Role
public interface ConfluenceScrollTranslationResolver
{
    /**
     * Get the configuration of the given translation language.
     * 
     * @param confluenceId The Confluence page id.
     * @param language The translation language.
     * @return the configuration of the given translation language.
     * @throws ConfluenceResolverException in case something goes wrong.
     */
    Map<String, String> getMacroParameters(Long confluenceId, String language) throws ConfluenceResolverException;
}
