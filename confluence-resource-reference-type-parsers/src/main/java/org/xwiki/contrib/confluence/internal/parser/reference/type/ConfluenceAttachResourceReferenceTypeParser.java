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
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

import javax.inject.Named;
import javax.inject.Singleton;

import static org.xwiki.contrib.confluence.internal.parser.reference.type.Utils.indexOf;
import static org.xwiki.contrib.confluence.internal.parser.reference.type.Utils.unescape;

/**
 * Confluence Page Resource Reference Type parser.
 * This allows Confluence-XML to output references for Confluence pages that couldn't be located during an
 * import, and that will work anyway when the pages are (later made) available on the XWiki instance.
 * @since 9.66.0
 * @version $Id$
 */
@Component
@Named("confluenceAttach")
@Singleton
public class ConfluenceAttachResourceReferenceTypeParser extends ConfluencePageResourceReferenceTypeParser
{
    @Override
    public ResourceType getType()
    {
        return ResourceType.ATTACHMENT;
    }

    @Override
    public ResourceReference parse(String reference)
    {
        int at = indexOf(reference, '@', 0);
        if (at == reference.length()) {
            return null;
        }

        int hash = indexOf(reference, '#', at);
        String r = reference.substring(0, at);
        if (hash < reference.length()) {
            r += reference.substring(hash);
        }

        try {
            String filename = unescape(reference.substring(at + 1, hash));
            return parseInternal(r, filename);
        } catch (ConfluenceResolverException ignored) {
            // let's not spam the logs with errors
        }
        return null;
    }
}
