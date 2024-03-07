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

import org.xwiki.rendering.wikimodel.xhtml.handler.ParagraphTagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Handles macros.
 * <p>
 * Example:
 * <p>
 * {@code
 * <ac:macro ac:name="code">
 *   <ac:parameter ac:name="title">Bloc code title</ac:parameter>
 *   <ac:parameter ac:name="theme">theme</ac:parameter>
 *   <ac:parameter ac:name="language">language</ac:parameter>
 *   <ac:plain-text-body><![CDATA[Content of bloc code]]></ac:plain-text-body>
 * </ac:macro>
 * }
 *
 * @version $Id$
 * @since 9.0
 */
public class ConfluenceParagraphTagHandler extends ParagraphTagHandler implements ConfluenceTagHandler
{
    /**
     * Default constructor.
     */
    public ConfluenceParagraphTagHandler()
    {
        super();
    }

    @Override
    protected void begin(TagContext context)
    {
        super.begin(context);
        context.getTagStack().pushStackParameter(CONFLUENCE_IN_PARAGRAPH, true);
    }

    @Override
    protected void end(TagContext context)
    {
        super.end(context);
        context.getTagStack().popStackParameter(CONFLUENCE_IN_PARAGRAPH);
    }
}
