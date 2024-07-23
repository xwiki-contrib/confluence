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
package org.xwiki.contrib.confluence.filter.internal.input;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.model.validation.EntityNameValidationManager;

/**
 * @version $Id$
 * @since 9.15
 */
@Component(roles = XWikiConverter.class)
@Singleton
public class XWikiConverter implements Initializable
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    private EntityNameValidationManager validation;

    @Override
    public void initialize()
    {
        this.validation = getValidationManager();
    }

    private EntityNameValidationManager getValidationManager()
    {
        try {
            return this.componentManager.getInstance(EntityNameValidationManager.class);
        } catch (ComponentLookupException e) {
            this.logger.warn("No EntityNameValidationManager implementation could be found", e);
        }

        return null;
    }

    /**
     * @param entityName the entity name to convert
     * @return the converted entity name
     */
    public String convert(String entityName)
    {
        if (this.validation == null) {
            // Workaround for https://jira.xwiki.org/projects/CONFLUENCE/issues/CONFLUENCE-256
            // We don't know how this.validation can be null here, if you figure this out, please
            // fix the root cause.
            this.validation = getValidationManager();
        }

        if (this.validation == null) {
            return entityName;
        }

        return this.validation.getEntityReferenceNameStrategy().transform(entityName);
    }
}
