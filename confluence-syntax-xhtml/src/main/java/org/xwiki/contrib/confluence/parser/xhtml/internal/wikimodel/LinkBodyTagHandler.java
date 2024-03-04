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

import org.xwiki.contrib.confluence.parser.xhtml.internal.ConfluenceXHTMLParser;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.parser.XDOMGeneratorListener;
import org.xwiki.rendering.internal.parser.wikimodel.XWikiGeneratorListener;
import org.xwiki.rendering.wikimodel.impl.WikiScannerContext;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Handles link content content.
 * <p>
 * Example:
 * <p>
 * <pre>{@code
 * <ac:link-body>Page with / in title</ac:link-body>
 * }</pre>
 *
 * @version $Id$
 * @since 9.17
 */
public class LinkBodyTagHandler extends AbstractConfluenceTagHandler implements ConfluenceTagHandler
{
    private ConfluenceXHTMLParser parser;

    /**
     * @param parser the Confluence parser
     */
    public LinkBodyTagHandler(ConfluenceXHTMLParser parser)
    {
        super(true);
        this.parser = parser;
    }

    @Override
    protected void begin(TagContext context)
    {
        XDOMGeneratorListener labelListener = new XDOMGeneratorListener();
        XWikiGeneratorListener xwikiListener = this.parser.createXWikiGeneratorListener(labelListener, null);
        context.getTagStack().pushScannerContext(new WikiScannerContext(xwikiListener));
        context.getScannerContext().beginDocument();
    }

    @Override
    protected void end(TagContext context)
    {
        context.getScannerContext().endDocument();
        WikiScannerContext scannerContext = context.getTagStack().popScannerContext();
        Object container = context.getTagStack().getStackParameter(CONFLUENCE_CONTAINER);

        if (container instanceof LabelContainer) {
            XWikiGeneratorListener xwikiListener = (XWikiGeneratorListener) scannerContext.getfListener();
            XDOMGeneratorListener labelListener = (XDOMGeneratorListener) xwikiListener.getListener();

            XDOM label = labelListener.getXDOM();
            ((LabelContainer) container).setLabel(label);
        }
    }
}
