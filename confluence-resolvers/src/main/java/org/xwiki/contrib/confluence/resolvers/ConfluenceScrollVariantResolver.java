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

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

/**
 * Get the equivalent variant reference based on a given pair of attribute ID and attribute value ID.
 *
 * @version $Id$
 * @since 9.71.0
 */
@Role
public interface ConfluenceScrollVariantResolver
{
    /**
     * @return the document in XWiki, or null if the document is not found.
     * @param attributeId the attribute ID.
     * @param attributeValueId the id if the attribute value.
     */
    DocumentReference getEquivalentVariantReference(String attributeId, String attributeValueId)
        throws ConfluenceResolverException;
}
