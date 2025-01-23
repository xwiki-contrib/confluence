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
package org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel;

import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceReferenceConverter;
import org.xwiki.rendering.listener.reference.ResourceReference;

/**
 * Fallback Confluence Reference Converter.
 * Used when parsing Confluence syntax outside the confluence-xml filter.
 * @version $Id$
 * @since 9.76.0
 */
public class FallbackConfluenceReferenceConverter implements ConfluenceReferenceConverter
{
    @Override
    public String convertUserReference(String userId)
    {
        return userId == null ? "" : userId;
    }

    @Override
    public String convertDocumentReference(String spaceKey, String pageTitle)
    {
        return refToString(getResourceReference(spaceKey, pageTitle, null, null));
    }

    @Override
    public String convertSpaceReference(String spaceKey)
    {
        return refToString(getResourceReference(spaceKey, null, null, null));
    }

    private String refToString(ResourceReference resourceReference)
    {
        return resourceReference == null ? "" : resourceReference.getReference();
    }
}
