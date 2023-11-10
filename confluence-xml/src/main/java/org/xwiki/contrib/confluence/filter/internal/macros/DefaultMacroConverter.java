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
package org.xwiki.contrib.confluence.filter.internal.macros;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.confluence.filter.MacroConverter;
import org.xwiki.rendering.listener.Listener;

/**
 * Find converter for passed macro.
 * @version $Id$
 * @since 9.1
 */
@Component
@Singleton
public class DefaultMacroConverter extends AbstractMacroConverter
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Override
    public void toXWiki(String id, Map<String, String> parameters, String content, boolean inline, Listener listener)
    {
        if (this.componentManager.hasComponent(MacroConverter.class, id)) {
            try {
                MacroConverter converter = this.componentManager.getInstance(MacroConverter.class, id);
                converter.toXWiki(id, parameters, content, inline, listener);
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to lookup converter for macro [{}] (id=[{}], parameters={}, inline=[{}])", id,
                    parameters, content, inline, e);
            }
        } else {
            // If we haven't found a specific macro converter matching this id, we use the default behavior
            super.toXWiki(id, parameters, content, inline, listener);
        }
    }
}
