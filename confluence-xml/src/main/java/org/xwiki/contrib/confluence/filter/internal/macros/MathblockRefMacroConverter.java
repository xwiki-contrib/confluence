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
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Map;

/**
 * Convert Mathblock-ref to mathjax.
 * @since 9.57.0
 * @version $Id$
 */
@Component
@Singleton
@Named("mathblock-ref")
public class MathblockRefMacroConverter extends AbstractMacroConverter
{
    private static final String ANCHOR = "anchor";

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
        // based on https://tex.stackexchange.com/questions/18311/what-are-the-valid-names-as-labels
        // this also makes sure we don't allow XWiki syntax injection by forbidding '"', '{', '}' and '$'
        String anchor = parameters.get(ANCHOR);
        if (StringUtils.isEmpty(anchor)) {
            throw new RuntimeException(
                "The mathblock-ref macro is missing its anchor, killing the macro conversion");
        }
        anchor = anchor.replaceAll(
            "[^!&()*+,\\-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ\\[\\]`abcdefghijklmnopqrstuvwxyz|]",
            ""
        );

        return "\\label{" + anchor + "}";
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.YES;
    }
}
