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
package org.xwiki.contrib.confluence.internal.parser.reference.type;

import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.contrib.confluence.resolvers.resource.ConfluenceResourceReferenceResolver;
import org.xwiki.contrib.confluence.resolvers.resource.ConfluenceResourceReferenceType;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.parser.ResourceReferenceTypeParser;

import javax.inject.Inject;

/**
 * Abstract Confluence Page Resource Reference Type parser.
 * @since 9.70.0
 * @version $Id$
 */
abstract class AbstractConfluenceResourceReferenceTypeParser implements ResourceReferenceTypeParser
{
    @Inject
    private ConfluenceResourceReferenceResolver resolver;

    @Override
    public ResourceReference parse(String reference)
    {
        try {
            return resolver.resolve(getConfluenceResourceReferenceType(), reference);
        } catch (ConfluenceResolverException ignored) {
            // let's not spam the logs with errors
        }
        return null;
    }

    abstract ConfluenceResourceReferenceType getConfluenceResourceReferenceType();
}
