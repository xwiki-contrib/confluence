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
package org.xwiki.contrib.confluence.urlmapping.internal;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageIdResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.contrib.confluence.urlmapping.ConfluenceURLMapper;
import org.xwiki.contrib.urlmapping.AbstractURLMapper;
import org.xwiki.contrib.urlmapping.DefaultURLMappingMatch;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;

/**
 * URL Mapper for Confluence attachments.
 * @since 9.54.0
 * @version $Id$
 */
@Component
@Singleton
@Named("attachment")
public class ConfluenceAttachmentURLMapper extends AbstractURLMapper implements ConfluenceURLMapper
{
    @Inject
    private ConfluencePageIdResolver confluenceIdResolver;

    @Inject
    private Logger logger;

    /**
     * Constructor.
     */
    public ConfluenceAttachmentURLMapper()
    {
        super("download/(?:attachments|thumbnails)/(?<pageId>\\d+)/(?<filename>[^?#]+)(?<params>\\?.*)?");
    }

    @Override
    public ResourceReference convert(DefaultURLMappingMatch match)
    {
        Matcher matcher = match.getMatcher();
        String pageIdStr = matcher.group("pageId");
        String filename = URLDecoder.decode(matcher.group("filename"), StandardCharsets.UTF_8);
        try {
            long pageId = Long.parseLong(pageIdStr);
            EntityReference docRef = confluenceIdResolver.getDocumentById(pageId);
            if (docRef == null) {
                return null;
            }

            AttachmentReference attachmentRef = new AttachmentReference(filename, new DocumentReference(docRef));
            return new EntityResourceReference(attachmentRef, EntityResourceAction.VIEW);
        } catch (ConfluenceResolverException | NumberFormatException e) {
            logger.error("Could not convert URL", e);
            return null;
        }
    }
}
