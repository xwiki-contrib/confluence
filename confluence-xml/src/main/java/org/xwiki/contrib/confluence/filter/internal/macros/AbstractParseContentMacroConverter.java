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

import java.io.StringReader;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;

import org.xwiki.contrib.confluence.filter.AbstractMacroConverter;
import org.xwiki.rendering.syntax.Syntax;

abstract class AbstractParseContentMacroConverter extends AbstractMacroConverter
{
    @Inject
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private ConfluenceInputContext inputContext;

    protected void parseContent(String id, Listener listener, String newContent)
    {
        ConfluenceInputProperties inputProperties = inputContext.getProperties();
        Syntax macroContentSyntax = inputProperties == null ? null : inputProperties.getMacroContentSyntax();
        String syntaxId = macroContentSyntax != null ? macroContentSyntax.toIdString() : Syntax.XWIKI_2_1.toIdString();

        if (StringUtils.isEmpty(newContent)) {
            return;
        }
        try {
            Parser parser = componentManagerProvider.get().getInstance(Parser.class, syntaxId);
            XDOM contentXDOM = parser.parse(new StringReader(newContent));
            contentXDOM.getChildren().forEach(child -> child.traverse(listener));
        } catch (ComponentLookupException | ParseException e) {
            new MacroBlock("error", Collections.emptyMap(),
                String.format("Failed to parse the content of the [%s] macro with the syntax [%s].", id, syntaxId),
                false).traverse(listener);
        }
    }
}
