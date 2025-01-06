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

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.resolvers.resource.ConfluenceResourceReferenceType;
import org.xwiki.rendering.listener.reference.ResourceType;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Confluence Page Resource Reference Type parser.
 * This allows Confluence-XML to output references for Confluence pages that couldn't be located during an
 * import, and that will work anyway when the pages are (later made) available on the XWiki instance.
 * @since 9.66.0
 * @version $Id$
 */
@Component
@Named("confluencePage")
@Singleton
public class ConfluencePageResourceReferenceTypeParser extends AbstractConfluenceResourceReferenceTypeParser
{
    @Override
    public ResourceType getType()
    {
        return ResourceType.DOCUMENT;
    }

    ConfluenceResourceReferenceType getConfluenceResourceReferenceType()
    {
        return ConfluenceResourceReferenceType.CONFLUENCE_PAGE;
    }
}
