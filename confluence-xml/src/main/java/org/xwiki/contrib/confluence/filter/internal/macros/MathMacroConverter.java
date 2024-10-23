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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * Convert Mathinline to mathjax.
 * @since 9.57.0
 * @version $Id$
 */
@Component (hints = {"mathinline", "mathblock"})
@Singleton
public class MathMacroConverter extends AbstractMacroConverter
{
    private static final String INLINE = "inline";

    @Inject
    private Logger logger;

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return "mathjax";
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        return Collections.emptyMap();
    }

    @Override
    protected String toXWikiContent(String confluenceId, Map<String, String> parameters, String confluenceContent)
    {

        String content = confluenceContent;
        if (StringUtils.isEmpty(content)) {
            content = parameters.get("body");
        }

        if (StringUtils.isEmpty(content)) {
            logger.warn("The body parameter of macro [{}] is missing", confluenceId);
            return "";
        }

        if (content.startsWith("--uriencoded--")) {
            String urlEncodedContent = content.substring(14).replace("+", "%2B");
            content = URLDecoder.decode(urlEncodedContent, StandardCharsets.UTF_8);
        }

        content = escapeMathjax(content);

        if (confluenceId.contains(INLINE)) {
            content = "\\(" + content + "\\)";
        } else {
            String anchor = parameters.get("anchor");
            if (StringUtils.isNotEmpty(anchor)) {
                content = "\\label{" + escapeMathjax(anchor) + "}\n" + content;
            }
            content = "\\begin{equation}\n" + content + "\n\\end{equation}";
        }

        return content;
    }

    private static String escapeMathjax(String content)
    {
        return content.replace("{{/", "{{ /");
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return id.contains(INLINE) ? InlineSupport.YES : InlineSupport.NO;
    }
}
