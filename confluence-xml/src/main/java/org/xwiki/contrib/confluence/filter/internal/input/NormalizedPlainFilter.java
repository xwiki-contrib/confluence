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
    private final Listener wrappedListener;

    NormalizedPlainFilter(PrintRenderer plainRenderer, Listener wrappedListener)
    {
        this.wrappedListener = wrappedListener;

        plainRenderer.setPrinter(this.printer);

        addListener(plainRenderer);
        addListener(wrappedListener);
    }

    Listener getWrappedListener()
    {
        return wrappedListener;
    }

    public WikiPrinter getPrinter()
    {
        return printer;
    }
}
