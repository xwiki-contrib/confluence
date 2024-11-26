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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.urlmapping.AbstractURLMappingPrefixHandler;
import org.xwiki.contrib.urlmapping.DefaultURLMappingConfiguration;
import org.xwiki.contrib.urlmapping.URLMapper;
import org.xwiki.localization.ContextualLocalizationManager;

import static org.xwiki.contrib.urlmapping.DefaultURLMappingConfiguration.Key;

/**
 * Confluence URL prefix handler.
 *
 * @version $Id$
 * @since 9.53.0
 */
@Component
@Singleton
@Named("confluence")
public class ConfluenceURLMappingPrefixHandler extends AbstractURLMappingPrefixHandler
{
    @Inject
    private ComponentManager componentManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Override
    protected URLMapper[] getMappers()
    {
        try {
            List<URLMapper> urlMappers = componentManager.getInstanceList(ConfluenceURLMapper.class);
            return urlMappers.toArray(URLMapper[]::new);
        } catch (ComponentLookupException e) {
            this.logger.error(e.getMessage(), e);
        }
        return new URLMapper[] {};
    }

    @Override
    protected void initializeConfigurationDefaults(DefaultURLMappingConfiguration configuration)
    {
        configuration.setDefault(Key.INTRO_MESSAGE,
            localizationManager.getTranslationPlain("confluence.urlmapping.introMessage"));
        configuration.setDefault(Key.NOT_FOUND_INTRO_MESSAGE,
            localizationManager.getTranslationPlain("confluence.urlmapping.notFoundIntroMessage"));
    }
}
