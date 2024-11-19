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
import org.xwiki.rendering.wikimodel.impl.WikiScannerContext;
import org.xwiki.rendering.wikimodel.xhtml.handler.ImgTagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Handles img, including emoticons as img.
 *
 * @version $Id$
 * @since 9.64.0
 */
public class ConfluenceImgTagHandler extends ImgTagHandler
{
    /**
     * Default constructor.
     */
    public ConfluenceImgTagHandler()
    {
        super();
    }

    @Override
    protected void begin(TagContext context)
    {
        if (sendEmoticon(context.getParams(), context.getScannerContext(), false)) {
            return;
        }
        super.begin(context);
    }

    @Override
    protected void end(TagContext context)
    {
        if (sendEmoticon(context.getParams(), context.getScannerContext(), true)) {
            return;
        }

        super.end(context);
    }

    private static boolean sendEmoticon(WikiParameters params, WikiScannerContext scannerContext, boolean dryRun)
    {
        WikiParameter classParam = params.getParameter("class");
        if (classParam == null) {
            return false;
        }

        String classNames = classParam.getValue();
        if (StringUtils.isEmpty(classNames)) {
            return false;
        }
        String[] classes = classNames.split("\\s+");
        for (String className : classes) {
            if ("emoticon".equals(className)) {
                WikiParameter altParam = params.getParameter("alt");
                String emoticonNameParens = altParam.getValue();
                if (StringUtils.isNotEmpty(emoticonNameParens)
                    && emoticonNameParens.startsWith("(")
                    && emoticonNameParens.endsWith(")")
                ) {
                    String emoticonName = emoticonNameParens.substring(1, emoticonNameParens.length() - 1);
                    String emoticon = EmoticonTagHandler.NAME_MAP.get(emoticonName);
                    if (StringUtils.isNotEmpty(emoticon)) {
                        onWord(scannerContext, dryRun, emoticon);
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    private static void onWord(WikiScannerContext scannerContext, boolean dryRun, String emoticon)
    {
        if (!dryRun) {
            scannerContext.onWord(emoticon);
        }
    }
}
