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

import org.xwiki.rendering.wikimodel.WikiParameter;
import org.xwiki.rendering.wikimodel.xhtml.handler.TagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

import java.util.Map;

/**
 * Handles images.
 * <p>
 * Example:
 * <p>
 * {@code
 * <ac:image><ri:attachment ri:filename="9391963529_96f9f9b16c_o.jpg"><ri:page ri:content-title="xhtml" ri:space-key="SPACE" /></ri:attachment></ac:image>
 * <ac:image><ri:url ri:value="http://host" /></ac:image>
 * <ac:image ac:align="left" ac:layout="align-start" ac:original-height="1200" ac:original-width="1200"
 *   ac:custom-width="true" ac:alt="some alt text" ac:width="50">
 *   <ri:attachment ri:filename="rock.jpg" ri:version-at-save="1" />
 *   <ac:caption>
 *     <p>sos<em>asdas</em>ame ca<strong>sasd</strong>ion</p>
 *   </ac:caption><ac:adf-mark key="border" size="2" color="#091e4224" />
 * </ac:image>
 * }
 *
 * @version $Id$
 * @since 9.0
 */
public class ImageTagHandler extends TagHandler implements ConfluenceTagHandler
{
    private static final Map<String, String> CONFLUENCE_TO_WIKI_PARAMS = Map.of(
        "align-start", "start",
        "align-end", "end",
        "wrap-left", "start",
        "wrap-right", "end"
    );

    public ImageTagHandler()
    {
        super(false);
    }

    @Override
    protected void begin(TagContext context)
    {
        ConfluenceImageWikiReference image = new ConfluenceImageWikiReference();

        for (WikiParameter param : context.getParams()) {
            if ("ac:layout".equals(param.getKey())) {
                image.getImageParameters().put("data-xwiki-image-style-alignment", CONFLUENCE_TO_WIKI_PARAMS.getOrDefault(param.getValue(), param.getValue()));
                if (param.getValue().contains("wrap-")) {
                    image.getImageParameters().put("data-xwiki-image-style-text-wrap", "true");
                }
            } else {
                image.getImageParameters().put(param.getKey().replace("ac:", ""), param.getValue());
            }
        }

        context.getTagStack().pushStackParameter(CONFLUENCE_CONTAINER, image);
    }

    @Override
    protected void end(TagContext context)
    {
        ConfluenceImageWikiReference image =
            (ConfluenceImageWikiReference) context.getTagStack().popStackParameter(CONFLUENCE_CONTAINER);

        context.getScannerContext().onImage(image);
    }
}
