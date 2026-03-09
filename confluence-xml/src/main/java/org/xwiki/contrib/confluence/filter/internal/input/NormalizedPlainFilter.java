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
package org.xwiki.contrib.confluence.filter.internal.input;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.xwiki.rendering.listener.CompositeListener;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Normalized Plain Filter.
 * @since 9.92.0
 */
class NormalizedPlainFilter extends CompositeListener
{
    private final WikiPrinter printer = new DefaultWikiPrinter();
    private PrintRenderer plainRenderer;

    NormalizedPlainFilter(PrintRenderer plainRenderer, Listener wrappedListener)
    {
        this.plainRenderer = plainRenderer;
        plainRenderer.setPrinter(this.printer);

        // the special symbol is a hack to keep the leading whitespaces
        plainRenderer.onSpecialSymbol('!');

        addListener(plainRenderer);
        if (wrappedListener != null) {
            addListener(wrappedListener);
        }
    }

    String consumeString()
    {
        if (plainRenderer == null) {
            LoggerFactory.getLogger(this.getClass()).error(
                    "There was an attempt to render an annotation twice. This should not happen. Please report a bug.");
            return "";
        }
        // the special symbol followed is a hack to keep the trailing whitespaces
        plainRenderer.onSpecialSymbol('!');

        String content = printer.toString();

        plainRenderer = null;

        // with the substring, we remove the leading and trailing hack special symbols
        return StringUtils.substring(content, 1, -1);
    }
}
