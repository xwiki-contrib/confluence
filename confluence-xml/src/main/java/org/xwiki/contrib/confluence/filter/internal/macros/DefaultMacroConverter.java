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
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;

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

    @Inject
    private ConfluenceInputContext context;

    @Inject
    private MacroManager macroManager;

    protected MacroConverter getMacroConverter(String macroId)
    {
        if (this.componentManager.hasComponent(MacroConverter.class, macroId)) {
            try {
                return this.componentManager.getInstance(MacroConverter.class, macroId);
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to lookup converter for macro [{}] ", macroId);
            }
            return null;
        }
        return this;
    }

    @Override
    public void toXWiki(String id, Map<String, String> parameters, String content, boolean inline, Listener listener)
    {
        MacroConverter converter = getMacroConverter(id);
        if (converter == this) {
            super.toXWiki(id, parameters, content, inline, listener);
            return;
        }

        if (converter != null) {
            try {
                converter.toXWiki(id, parameters, content, inline, listener);
            } catch (Exception e) {
                this.logger.error("Failed to convert macro [{}] using converter [{}]", id, converter, e);
                super.toXWiki(id, parameters, content, inline, listener);
            }
        }

        // If we haven't found a specific macro converter matching this id, we use the default behavior
    }

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        if (!this.context.getProperties().getUnprefixedMacros().isEmpty()) {
            if (this.context.getProperties().getUnprefixedMacros().contains(confluenceId)
                || confluenceId.startsWith("confluence_")) {
                return confluenceId;
            }

            // If there is an explicit list of unprefixed macros the others are prefixed
            return this.context.getProperties().getUnknownMacroPrefix() + confluenceId;
        }

        // Check the explicit list of prefixed macros
        if (this.context.getProperties().getPrefixedMacros().contains(confluenceId)) {
            return this.context.getProperties().getUnknownMacroPrefix() + confluenceId;
        }

        // By default macros are not prefixed
        return confluenceId;
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        MacroConverter conv = getMacroConverter(id);
        if (conv == null) {
            // should not happen
            conv = this;
        }
        try {
            if (conv != this) {
                InlineSupport converterInlineSupport = conv.supportsInlineMode(id, parameters, content);
                if (converterInlineSupport != null && !converterInlineSupport.equals(InlineSupport.MAYBE)) {
                    return converterInlineSupport;
                }
            }
            Macro<?> macro = macroManager.getMacro(new MacroId(conv.toXWikiId(id, parameters, content, true)));
            if (macro != null) {
                return macro.supportsInlineMode() ? InlineSupport.YES : InlineSupport.NO;
            }
        } catch (MacroLookupException e) {
            // Ignore
        }
        return InlineSupport.MAYBE;
    }
}
