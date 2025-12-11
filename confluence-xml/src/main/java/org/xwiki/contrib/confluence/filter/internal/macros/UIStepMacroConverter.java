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

import java.util.Collections;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.ConversionException;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;

/**
 * Convert the confluence ui-step macro to a list item.
 *
 * @version $Id$
 * @since 1.90.0
 */
@Component
@Singleton
@Named("ui-step")
public class UIStepMacroConverter extends AbstractParseContentMacroConverter
{
    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return null;
    }

    @Override
    protected void toXWiki(String confluenceId, Map<String, String> confluenceParameters, boolean inline,
        String confluenceContent, Listener listener) throws ConversionException
    {
        try {
            listener.beginList(ListType.NUMBERED, Collections.emptyMap());
            listener.beginListItem();
            parseContent(confluenceId, listener, confluenceContent);
            listener.endListItem();
            listener.endList(ListType.NUMBERED, Collections.emptyMap());
            // horrible hack. We need the list items to be inside a list (or the xwiki syntax horribly breaks with
            // NPEs), but we apparently don't really have a good means to start and end the list at the right place
            // due to how we browse and recurse macros. We surround each list item in its own list and we will merge
            // the lists in ui-steps by looking for those fake confluence_betwwen_ui_step empty block macros
            listener.onMacro("confluence_betwwen_ui_step", Collections.emptyMap(), "", false);
        } catch (Exception e) {
            throw new ConversionException("Failed to convert a ui-step macro", e);
        }
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        return Collections.emptyMap();
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.NO;
    }
}
