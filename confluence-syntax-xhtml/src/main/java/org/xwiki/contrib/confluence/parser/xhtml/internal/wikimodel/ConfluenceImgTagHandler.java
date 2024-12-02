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
    private static final String CLASS = "class";

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
        if (sendEmoticon(context, CLASS, false)) {
            return;
        }
        super.begin(context);
    }

    @Override
    protected void end(TagContext context)
    {
        if (sendEmoticon(context, CLASS, true)) {
            return;
        }

        super.end(context);
    }

    static boolean sendEmoticon(TagContext context, String classParameterName, boolean dryRun)
    {
        WikiParameter classParam = context.getParams().getParameter(classParameterName);
        if (classParam == null) {
            return false;
        }

        String classNames = classParam.getValue();
        if (StringUtils.isEmpty(classNames)) {
            return false;
        }
        String[] classes = classNames.split("\\s+");
        for (String className : classes) {
            if (className.startsWith("emoticon-")) {
                String emoticonName = className.substring(9);
                String emoticon = EmoticonTagHandler.NAME_MAP.get(emoticonName);
                if (StringUtils.isNotEmpty(emoticon)) {
                    onWord(context.getScannerContext(), dryRun, emoticon);
                    return true;
                }
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
