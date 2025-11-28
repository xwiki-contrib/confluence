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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.ConversionException;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.rendering.listener.Listener;
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
public class MacroToContentConverter extends AbstractParseContentMacroConverter
{
    private static final String DIV_CLASS_FORMAT = "confluence_%s_content";

    private static final String HTML_ATTRIBUTE_ID = "id";

    private static final String HTML_ATTRIBUTE_CLASS = "class";

    @Inject
    protected ConfluenceInputContext context;

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return confluenceId;
    }

    @Override
    public void toXWiki(String id, Map<String, String> parameters, boolean inline, String content, Listener listener)
        throws ConversionException
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

    /**
     * The content is about to be processed (parseContent is about to be called).
     * @param id the Confluence macro name
     * @param parameters the Confluence parameters
     * @param content the content of the macro
     * @param inline whether the macro is parsed inline
     * @param listener the listener
     * @throws ConversionException if the conversion cannot continue
     */
    protected void beginEvent(String id, Map<String, String> parameters, String content, boolean inline,
        Listener listener) throws ConversionException
    {
        listener.beginGroup(parameters);
    }

    /**
     * The content was just processed (parseContent just returned).
     * @param id the Confluence macro name
     * @param parameters the Confluence parameters
     * @param content the content of the macro
     * @param inline whether the macro is parsed inline
     * @param listener the listener
     * @throws ConversionException if the conversion cannot continue
     */
    protected void endEvent(String id, Map<String, String> parameters, String content, boolean inline,
        Listener listener) throws ConversionException
    {
        listener.endGroup(parameters);
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content) throws ConversionException
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
