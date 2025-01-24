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
package org.xwiki.contrib.confluence.filter.url.internal;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceURLConverter;
import org.xwiki.rendering.listener.reference.ResourceReference;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.List;

/**
 * Default Confluence URL Converter, that iterates over all the available (named) Confluence URL converters.
 * @version $Id$
 * @since 9.76.0
 */
@Component
@Singleton
public class DefaultConfluenceURLConverter implements ConfluenceURLConverter
{
    @Inject
    private Logger logger;

    @Inject
    private Provider<ComponentManager> componentManagerProvider;

    @Override
    public ResourceReference convertURL(String url)
    {
        List<ConfluenceURLConverter> converters;

        try {
            converters = componentManagerProvider.get().getInstanceList(ConfluenceURLConverter.class);
        } catch (ComponentLookupException e) {
            logger.error("Failed to get the Confluence URL converters", e);
            return null;
        }

        for (ConfluenceURLConverter converter : converters) {
            if (converter == this) {
                continue;
            }

            ResourceReference resourceReference = converter.convertURL(url);
            if (resourceReference != null) {
                return resourceReference;
            }
        }

        return null;
    }
}
