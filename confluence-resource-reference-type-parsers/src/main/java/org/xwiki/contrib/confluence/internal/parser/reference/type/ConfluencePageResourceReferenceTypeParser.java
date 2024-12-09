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
import org.xwiki.contrib.confluence.resolvers.ConfluencePageIdResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageTitleResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.contrib.confluence.resolvers.ConfluenceSpaceKeyResolver;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.parser.ResourceReferenceTypeParser;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static org.xwiki.contrib.confluence.internal.parser.reference.type.Utils.indexOf;
import static org.xwiki.contrib.confluence.internal.parser.reference.type.Utils.getAnchor;
import static org.xwiki.contrib.confluence.internal.parser.reference.type.Utils.unescape;

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
public class ConfluencePageResourceReferenceTypeParser implements ResourceReferenceTypeParser
{
    @Inject
    private ConfluencePageIdResolver pageIdResolver;

    @Inject
    private ConfluencePageTitleResolver pageTitleResolver;

    @Inject
    private ConfluenceSpaceKeyResolver spaceKeyResolver;

    @Inject
    private EntityReferenceSerializer<String> referenceSerializer;

    @Override
    public ResourceType getType()
    {
        return ResourceType.DOCUMENT;
    }

    @Override
    public ResourceReference parse(String reference)
    {
        try {
            return parseInternal(reference, null);
        } catch (ConfluenceResolverException ignored) {
            // let's not spam the logs with errors
        }
        return null;
    }

    protected ResourceReference parseInternal(String reference, String filename) throws ConfluenceResolverException
    {
        if (reference.startsWith("page:")) {
            return getPageResourceReference(reference, filename);
        }

        if (reference.startsWith("id:")) {
            return getPageIdResourceReference(reference, filename);
        }

        if (reference.startsWith("spaceHome:")) {
            return getSpaceHomeResourceReference(reference, filename);
        }

        return null;
    }

    private ResourceReference getPageResourceReference(String reference, String filename)
        throws ConfluenceResolverException
    {
        int dot = indexOf(reference, '.', 5);
        if (dot + 1 >= reference.length()) {
            return null;
        }
        int hash = indexOf(reference, '#', dot + 1);
        String spaceKey = reference.substring(5, dot);
        String pageTitle = unescape(reference.substring(dot + 1, hash));
        EntityReference docRef = pageTitleResolver.getDocumentByTitle(spaceKey, pageTitle);
        return toDocumentResourceReference(docRef, hash, reference, filename);
    }

    private ResourceReference getPageIdResourceReference(String reference, String filename)
        throws ConfluenceResolverException
    {
        int hash = indexOf(reference, '#', 3);
        long pageId = Long.parseLong(reference.substring(3, hash));
        EntityReference docRef = pageIdResolver.getDocumentById(pageId);
        return toDocumentResourceReference(docRef, hash, reference, filename);
    }

    private ResourceReference getSpaceHomeResourceReference(String reference, String filename)
        throws ConfluenceResolverException
    {
        int hash = indexOf(reference, '#', 10);
        EntityReference spaceRef = spaceKeyResolver.getSpaceByKey(unescape(reference.substring(10, hash)));
        if (spaceRef == null) {
            return null;
        }
        EntityReference docRef = new EntityReference("WebHome", EntityType.DOCUMENT, spaceRef);
        return toDocumentResourceReference(docRef, hash, reference, filename);
    }

    private ResourceReference toDocumentResourceReference(EntityReference docRef, int hash, String reference,
        String filename)
    {
        if (docRef == null) {
            return null;
        }

        String anchor = getAnchor(reference, hash);
        if (StringUtils.isEmpty(filename)) {
            DocumentResourceReference documentResourceReference = new DocumentResourceReference(
                referenceSerializer.serialize(docRef));
            if (StringUtils.isNotEmpty(anchor)) {
                documentResourceReference.setAnchor(anchor);
            }
            return documentResourceReference;
        } else {
            AttachmentResourceReference attachmentResourceReference = new AttachmentResourceReference(
                referenceSerializer.serialize(new EntityReference(filename, EntityType.ATTACHMENT, docRef)));
            if (StringUtils.isNotEmpty(anchor)) {
                attachmentResourceReference.setAnchor(anchor);
            }
            return attachmentResourceReference;
        }
    }
}
