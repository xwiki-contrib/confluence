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
package org.xwiki.contrib.confluence.resolvers.resource;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.rendering.listener.reference.ResourceReference;

/**
 * Confluence Resource Reference Resolver.
 * @since 9.70.0
 * @version $Id$
 */
@Role
public interface ConfluenceResourceReferenceResolver
{
    /**
     * @return the resource reference corresponding to this Confluence reference, or null if not found
     * @param type the reference type
     * @param typelessReference the reference to pass
     */
    ResourceReference resolve(ConfluenceResourceReferenceType type, String typelessReference)
        throws ConfluenceResolverException;

    /**
     * @return the type of this reference
     * @param reference the reference of which the type is to be determined
     */
    ConfluenceResourceReferenceType getType(String reference);
}
