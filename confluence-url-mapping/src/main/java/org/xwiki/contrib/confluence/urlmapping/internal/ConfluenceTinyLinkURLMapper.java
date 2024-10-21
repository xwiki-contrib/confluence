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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.regex.Matcher;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageIdResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.contrib.urlmapping.AbstractURLMapper;
import org.xwiki.contrib.urlmapping.DefaultURLMappingMatch;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.stability.Unstable;

/**
 * URL Mapper for Confluence tiny links.
 * @since 9.55.0
 * @version $Id$
 */
@Component (roles = ConfluenceTinyLinkURLMapper.class)
@Unstable
@Singleton
public class ConfluenceTinyLinkURLMapper extends AbstractURLMapper
{
    @Inject
    private ConfluencePageIdResolver confluenceIdResolver;

    @Inject
    private Logger logger;

    /**
     * Constructor.
     */
    public ConfluenceTinyLinkURLMapper()
    {
        super("x/(?<part>[^?#]+)(?<params>&.*)?");
    }

    @Override
    public ResourceReference convert(DefaultURLMappingMatch match)
    {
        Matcher matcher = match.getMatcher();
        long pageId;
        try {
            pageId = tinyPartToPageId(matcher.group("part"));
        } catch (IllegalArgumentException e) {
            logger.error("Failed to decode the tiny link", e);
            return null;
        }

        try {
            EntityReference docRef = confluenceIdResolver.getDocumentById(pageId);
            if (docRef != null) {
                return new EntityResourceReference(docRef, EntityResourceAction.VIEW);
            }
        } catch (ConfluenceResolverException e) {
            logger.warn("Could not convert tiny link", e);
        }
        return null;
    }

    private long tinyPartToPageId(String part)
    {
        // Reverse-engineered and inspired by https://confluence.atlassian.com/x/2EkGOQ
        // not sure the replaceChars part is necessary, but it shouldn't hurt
        String base64WithoutPadding = StringUtils.replaceChars(part, "-_/", "/+\n");

        byte[] decoded = new byte[8];
        Base64.getUrlDecoder().decode(base64WithoutPadding.getBytes(), decoded);
        return ByteBuffer.wrap(decoded).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }
}
