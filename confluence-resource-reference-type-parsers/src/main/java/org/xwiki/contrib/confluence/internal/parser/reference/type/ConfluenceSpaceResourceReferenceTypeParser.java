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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.contrib.confluence.resolvers.ConfluenceSpaceKeyResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.listener.reference.SpaceResourceReference;
import org.xwiki.rendering.parser.ResourceReferenceTypeParser;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static org.xwiki.contrib.confluence.internal.parser.reference.type.Utils.getAnchor;
import static org.xwiki.contrib.confluence.internal.parser.reference.type.Utils.unescape;
import static org.xwiki.contrib.confluence.internal.parser.reference.type.Utils.indexOf;

/**
 * Confluence Page Resource Reference Type parser.
 * This allows Confluence-XML to output references for Confluence pages that couldn't be located during an
 * import, and that will work anyway when the pages are (later made) available on the XWiki instance.
 * @since 9.66.0
 * @version $Id$
 */
@Component
@Named("confluenceSpace")
@Singleton
public class ConfluenceSpaceResourceReferenceTypeParser implements ResourceReferenceTypeParser
{
    @Inject
    private ConfluenceSpaceKeyResolver spaceKeyResolver;

    @Inject
    private EntityReferenceSerializer<String> referenceSerializer;

    @Override
    public ResourceType getType()
    {
        return ResourceType.SPACE;
    }

    @Override
    public ResourceReference parse(String reference)
    {
        try {
            return parseInternal(reference);
        } catch (ConfluenceResolverException ignored) {
            // let's not spam the logs with errors
        }
        return null;
    }

    private ResourceReference parseInternal(String reference) throws ConfluenceResolverException
    {
        int hash = indexOf(reference, '#', 0);
        EntityReference spaceRef = spaceKeyResolver.getSpaceByKey(unescape(reference.substring(0, hash)));
        if (spaceRef == null) {
            return null;
        }
        SpaceResourceReference spaceRRef = new SpaceResourceReference(referenceSerializer.serialize(spaceRef));
        String anchor = getAnchor(reference, hash);
        if (StringUtils.isNotEmpty(anchor)) {
            spaceRRef.setAnchor(anchor);
        }
        return spaceRRef;
    }
}
