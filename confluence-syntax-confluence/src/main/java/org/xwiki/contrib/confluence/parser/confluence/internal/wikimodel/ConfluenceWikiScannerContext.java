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
package org.xwiki.contrib.confluence.parser.confluence.internal.wikimodel;

import org.xwiki.rendering.wikimodel.IWemListener;
import org.xwiki.rendering.wikimodel.impl.WikiScannerContext;

/**
 * @version $Id: 10b2134d170fa32b0cd635974f80768f1e9a6da7 $
 * @since 9.0
 */
public class ConfluenceWikiScannerContext extends WikiScannerContext
{
    /**
     * @param listener the listener to use
     */
    public ConfluenceWikiScannerContext(IWemListener listener)
    {
        super(listener);
    }

    @Override
    protected ConfluenceInternalWikiScannerContext newInternalContext()
    {
        ConfluenceInternalWikiScannerContext context = new ConfluenceInternalWikiScannerContext(
            fSectionBuilder,
            fListener);
        return context;
    }

    @Override
    public ConfluenceInternalWikiScannerContext getContext()
    {
        if (!fStack.isEmpty()) {
            return (ConfluenceInternalWikiScannerContext) fStack.peek();
        }
        ConfluenceInternalWikiScannerContext context = newInternalContext();
        fStack.push(context);
        return context;
    }

    /**
     * @return whether the context is in a table.
     */
    public boolean isExplicitInTable()
    {
        return getContext().isExplicitInTable();
    }
}
