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

import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.confluence.filter.MacroConverter;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.rendering.listener.Listener;

/**
 * Find converter for passed macro.
 * 
 * @version $Id$
 * @since 9.1
 */
@Component
@Singleton
public class DefaultMacroConverter implements MacroConverter
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private ConfluenceInputContext context;

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
            listener.onMacro(toXWikiMacroName(id), toXWikiMacroParaleters(parameters), content, inline);
        }
    }

    private String toXWikiMacroName(String confluenceMacroName)
    {
        if (!this.context.getProperties().getUnprefixedMacros().isEmpty()) {
            if (this.context.getProperties().getUnprefixedMacros().contains(confluenceMacroName)) {
                return confluenceMacroName;
            }

            // If there is an explicit list of unprefixed macros the others are prefixed
            return this.context.getProperties().getUnknownMacroPrefix() + confluenceMacroName;
        }

        // Check the explicit list of prefixed macros
        if (this.context.getProperties().getPrefixedMacros().contains(confluenceMacroName)) {
            return this.context.getProperties().getUnknownMacroPrefix() + confluenceMacroName;
        }

        // By default macros are not prefixed
        return confluenceMacroName;
    }

    private Map<String, String> toXWikiMacroParaleters(Map<String, String> confluenceMacroParameters)
    {
        if (confluenceMacroParameters == null || !this.context.getProperties().isConvertToXWiki()
            || !confluenceMacroParameters.containsKey("")) {
            return confluenceMacroParameters;
        }

        Map<String, String> xwikiMacroParameters = new LinkedHashMap<>(confluenceMacroParameters.size());

        for (Map.Entry<String, String> entry : confluenceMacroParameters.entrySet()) {
            String key = entry.getKey();
            if (key.isEmpty()) {
                // xwiki/2.x syntax does not currently support empty parameter name so we workaround it using the same
                // default parameter name than the Confluence wiki syntax parser
                // TODO: should probably get rid of that hack when https://jira.xwiki.org/browse/XRENDERING-601 is fixed
                key = "0";
            }

            xwikiMacroParameters.put(key, entry.getValue());
        }

        return xwikiMacroParameters;
    }
}
