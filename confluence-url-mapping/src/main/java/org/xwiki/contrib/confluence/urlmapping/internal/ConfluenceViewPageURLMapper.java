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

import java.util.regex.Matcher;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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
 * URL Mapper for Confluence "viewpage.action" URLs (linking to pages using an ID).
 * @since 9.54.0
 * @version $Id$
 */
@Component
@Unstable
@Singleton
@Named("viewPage")
public class ConfluenceViewPageURLMapper extends AbstractURLMapper implements ConfluenceURLMapper
{
    @Inject
    private ConfluencePageIdResolver confluenceIdResolver;

    @Inject
    private Logger logger;

    /**
     * Constructor.
     */
    public ConfluenceViewPageURLMapper()
    {
        super("pages/viewpage.action\\?pageId=(?<pageId>\\d+)(?<params>&.*)?");
    }

    @Override
    public ResourceReference convert(DefaultURLMappingMatch match)
    {
        Matcher matcher = match.getMatcher();
        String pageIdStr = matcher.group("pageId");
        try {
            long pageId = Long.parseLong(pageIdStr);
            EntityReference docRef = confluenceIdResolver.getDocumentById(pageId);
            if (docRef == null) {
                return null;
            }

            return new EntityResourceReference(docRef, EntityResourceAction.VIEW);
        } catch (NumberFormatException | ConfluenceResolverException e) {
            logger.error("Could not convert URL", e);
            return null;
        }
    }
}
