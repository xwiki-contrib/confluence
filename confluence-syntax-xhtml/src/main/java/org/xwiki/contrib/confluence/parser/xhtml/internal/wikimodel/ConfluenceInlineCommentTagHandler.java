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
package org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.rendering.wikimodel.WikiParameter;
import org.xwiki.rendering.wikimodel.WikiParameters;
import org.xwiki.rendering.wikimodel.xhtml.handler.TagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

import java.util.Collections;

/**
 * Handles inline comments. The goal is not so much to convert it to some XWiki equivalent but to make easier to extract
 * them from the content.
 * <p>
 * {@code
 * 
<p>
 * Before
 * <ac:inline-comment-marker ac:ref="21641e47-9393-4d3c-890f-0994b2181443">annotated content</ac:inline-comment-marker>
 * after.
 * 
</p>
 * }
 * 
 * @version $Id$
 * @since 9.79.0
 */
public class ConfluenceInlineCommentTagHandler extends TagHandler
{
    /**
     * The name of the parameter holding the reference of the inline comment.
     */
    public static final String PARAMETER_REF = "ac:ref";

    /**
     * Constructor.
     */
    public ConfluenceInlineCommentTagHandler()
    {
        super(true);
    }

    @Override
    protected void begin(TagContext context)
    {
        String current = getACParam(context);
        context.getScannerContext().onMacroInline("confluence_inline_comment_marker_start",
                new WikiParameters(Collections.singletonList(new WikiParameter(PARAMETER_REF, current))), "");
    }

    @Override
    protected void end(TagContext context)
    {
        String current = getACParam(context);
        context.getScannerContext().onMacroInline("confluence_inline_comment_marker_end",
                new WikiParameters(Collections.singletonList(new WikiParameter(PARAMETER_REF, current))), "");
    }

    private static String getACParam(TagContext context)
    {
        if (!"ac:inline-comment-marker".equals(context.getName())) {
            return "";
        }

        WikiParameters params = context.getParams();
        if (params == null) {
            return "";
        }

        WikiParameter parameter = params.getParameter(PARAMETER_REF);
        return parameter == null ? "" : StringUtils.defaultString(parameter.getValue());
    }
}
