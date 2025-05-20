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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.rendering.wikimodel.WikiParameter;
import org.xwiki.rendering.wikimodel.WikiParameters;
import org.xwiki.rendering.wikimodel.xhtml.handler.TagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

import static org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceTagHandler.CONFLUENCE_TABLE_COLUMN_ATTRIBUTES;

/**
 * Table tag col handler.
 * @since 9.85.0
 * @version $Id$
 */
public class ConfluenceTableColHandler extends TagHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfluenceTableColHandler.class);

    private static final String STYLE = "style";

    /**
     * Constructor.
     */
    public ConfluenceTableColHandler()
    {
        super(false);
    }

    @Override
    protected void begin(TagContext context)
    {
        super.begin(context);
        String style = null;
        WikiParameters params = context.getParams();
        WikiParameter styleParam = params.getParameter(STYLE);
        if (styleParam != null) {
            style = styleParam.getValue();
        }

        Map<String, String> attributes = StringUtils.isEmpty(style) ? Collections.emptyMap() : Map.of(STYLE, style);
        int span = 1;
        WikiParameter spanParam = params.getParameter("span");
        if (spanParam != null) {
            try {
                span = Integer.parseInt(spanParam.getValue());
            } catch (Exception e) {
                LOGGER.error("Could not parse the span parameter of the col element (value: [{}]), expected an integer",
                    spanParam.getValue(), e);
            }
        }

        List<Map<String, String>> tableAttributes =
            (List<Map<String, String>>) context.getTagStack().getStackParameter(CONFLUENCE_TABLE_COLUMN_ATTRIBUTES);

        for (int i = 0; i < span; i++) {
            tableAttributes.add(attributes);
        }
    }
}
