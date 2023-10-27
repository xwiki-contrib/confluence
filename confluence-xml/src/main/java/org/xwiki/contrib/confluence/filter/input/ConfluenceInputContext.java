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
     * @return the confluence package being filtered
     * @since 9.26.0
     */
    ConfluenceXMLPackage getConfluencePackage();
}
