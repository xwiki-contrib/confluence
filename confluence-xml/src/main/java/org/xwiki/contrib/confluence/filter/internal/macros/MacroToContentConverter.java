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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Converts a macro call to a group, retaining the id, class and content.
 *
 * @version $Id$
 * @since 9.51.1
 */
@Component(hints = { "ul", "legend", "auihorizontalnav", "auibuttongroup", "auihorizontalnavpage", "tableenhancer",
    "footnote" })
@Singleton
public class MacroToContentConverter extends AbstractMacroConverter
{
    private static final String DIV_CLASS_FORMAT = "confluence_%s_content";

    private static final String HTML_ATTRIBUTE_ID = "id";

    private static final String HTML_ATTRIBUTE_CLASS = "class";

    @Inject
    protected ConfluenceInputContext context;

    @Inject
    protected ComponentManager componentManager;

    @Override
    public void toXWiki(String id, Map<String, String> parameters, String content, boolean inline, Listener listener)
    {
        Map<String, String> divWrapperParams = toXWikiParameters(id, parameters, content);
        ConfluenceInputProperties inputProperties = context.getProperties();
        Syntax macroContentSyntax = inputProperties == null ? null : inputProperties.getMacroContentSyntax();
        String syntaxId = macroContentSyntax != null ? macroContentSyntax.toIdString() : Syntax.XWIKI_2_1.toIdString();
        String newContent = toXWikiContent(id, parameters, content);
        beginEvent(id, divWrapperParams, newContent, inline, listener);
        parseContent(id, listener, syntaxId, newContent);
        endEvent(id, divWrapperParams, newContent, inline, listener);
    }

    protected void parseContent(String id, Listener listener, String syntaxId, String newContent)
    {
        if (StringUtils.isEmpty(newContent)) {
            return;
        }
        try {
            Parser parser = componentManager.getInstance(Parser.class, syntaxId);
            XDOM contentXDOM = parser.parse(new StringReader(newContent));
            contentXDOM.getChildren().forEach(child -> child.traverse(listener));
        } catch (ComponentLookupException | ParseException e) {
            new MacroBlock("error", Collections.emptyMap(),
                String.format("Failed to parse the content of the [%s] macro with the syntax [%s].", id, syntaxId),
                false).traverse(listener);
        }
    }

    protected void beginEvent(String id, Map<String, String> parameters, String content, boolean inline,
        Listener listener)
    {
        listener.beginGroup(parameters);
    }

    protected void endEvent(String id, Map<String, String> parameters, String content, boolean inline,
        Listener listener)
    {
        listener.endGroup(parameters);
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> divWrapperParams = new HashMap<>();

        List<String> classes = new ArrayList<>();
        classes.add(String.format(DIV_CLASS_FORMAT, confluenceId));
        String className = confluenceParameters.get(HTML_ATTRIBUTE_CLASS);
        if (StringUtils.isNotEmpty(className)) {
            classes.add(className);
        }
        divWrapperParams.put(HTML_ATTRIBUTE_CLASS, String.join(" ", classes));

        String id = confluenceParameters.get(HTML_ATTRIBUTE_ID);
        if (StringUtils.isNotEmpty(id)) {
            divWrapperParams.put(HTML_ATTRIBUTE_ID, id);
        }
        return divWrapperParams;
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.NO;
    }
}
