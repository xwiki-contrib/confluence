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
package org.xwiki.contrib.confluence.resolvers.resource.internal;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageIdResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageTitleResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.contrib.confluence.resolvers.resource.ConfluenceResourceReferenceResolver;
import org.xwiki.contrib.confluence.resolvers.resource.ConfluenceResourceReferenceType;
import org.xwiki.contrib.confluence.resolvers.ConfluenceSpaceKeyResolver;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.SpaceResourceReference;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.xwiki.contrib.confluence.resolvers.resource.internal.Utils.indexOf;
import static org.xwiki.contrib.confluence.resolvers.resource.internal.Utils.getAnchor;
import static org.xwiki.contrib.confluence.resolvers.resource.internal.Utils.unescape;

/**
 * Default Confluence Resource Reference Resolver.
 * @since 9.70.0
 * @version $Id$
 */
@Component
@Singleton
public class DefaultConfluenceResourceReferenceResolver implements ConfluenceResourceReferenceResolver
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
    public ResourceReference resolve(ConfluenceResourceReferenceType type, String reference)
        throws ConfluenceResolverException
    {
        switch (type) {
            case CONFLUENCE_PAGE:
                return parseConfluencePageReference(reference);
            case CONFLUENCE_ATTACH:
                return parseConfluenceAttachReference(reference);
            case CONFLUENCE_SPACE:
                return parseConfluenceSpaceReference(reference);
            default:
                return null;
        }
    }

    /**
     * @return the resource reference corresponding to this Confluence reference, or null if not found
     * @param reference the reference to parse
     * @throws ConfluenceResolverException if something wrong happens
     */
    public ResourceReference parseConfluencePageReference(String reference) throws ConfluenceResolverException
    {
        return parseInternal(reference, null);
    }

    /**
     * @return the resource reference corresponding to this Confluence reference, or null if not found
     * @param reference the reference to parse
     * @throws ConfluenceResolverException if something wrong happens
     */
    public ResourceReference parseConfluenceSpaceReference(String reference) throws ConfluenceResolverException
    {
        int hash = Utils.indexOf(reference, '#', 0);
        EntityReference spaceRef = spaceKeyResolver.getSpaceByKey(Utils.unescape(reference.substring(0, hash)));
        if (spaceRef == null) {
            return null;
        }
        SpaceResourceReference spaceRRef = new SpaceResourceReference(referenceSerializer.serialize(spaceRef));
        String anchor = Utils.getAnchor(reference, hash);
        if (StringUtils.isNotEmpty(anchor)) {
            spaceRRef.setAnchor(anchor);
        }
        return spaceRRef;
    }

    /**
     * @return the space resource reference corresponding to this Confluence reference, or null if not found
     * @param reference the reference to parse
     * @throws ConfluenceResolverException if something wrong happens
     */
    public ResourceReference parseConfluenceAttachReference(String reference) throws ConfluenceResolverException
    {
        int at = Utils.indexOf(reference, '@', 0);
        if (at == reference.length()) {
            return null;
        }

        int hash = Utils.indexOf(reference, '#', at);
        String r = reference.substring(0, at);
        if (hash < reference.length()) {
            r += reference.substring(hash);
        }

        String filename = Utils.unescape(reference.substring(at + 1, hash));
        return parseInternal(r, filename);
    }

    @Override
    public ConfluenceResourceReferenceType getType(String ref)
    {
        for (ConfluenceResourceReferenceType type : ConfluenceResourceReferenceType.values()) {
            String candidateType = type.getId();
            int length = candidateType.length();
            if (ref.length() > length + 1 && ref.charAt(length) == ':' && ref.startsWith(candidateType)) {
                return type;
            }
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
        if ("@self".equals(spaceKey)) {
            // FIXME we don't handle @self references yet
            return null;
        }
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
