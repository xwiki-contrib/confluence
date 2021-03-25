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
import org.xwiki.rendering.internal.parser.wikimodel.XWikiGeneratorListener;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.WrappingListener;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.wikimodel.impl.WikiScannerContext;
import org.xwiki.rendering.wikimodel.xhtml.handler.PreserveTagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Handles an element containing other elements which need to be converted to a single text conent.
 *
 * @version $Id$
 * @since 9.8
 */
public abstract class AbstractRichContentTagHandler extends PreserveTagHandler implements ConfluenceTagHandler
{
    private static final String CURRRENT_LISTENER = "rich_current_listener";

    private final ConfluenceXHTMLParser parser;

    /**
     * @param parser is used access the parser and the rendering to use to manipulate the content
     */
    AbstractRichContentTagHandler(ConfluenceXHTMLParser parser)
    {
        this.parser = parser;
    }

    @Override
    protected void begin(TagContext context)
    {
        PrintRendererFactory rendererFactory = this.parser.getMacroContentRendererFactory();

        if (rendererFactory != null) {
            Listener contentRenderer = rendererFactory.createRenderer(new DefaultWikiPrinter());

            WrappingListener converter = this.parser.getConverter();
            if (converter != null) {
                // Remember the current listener to put it back
                context.getTagStack().setStackParameter(CURRRENT_LISTENER, converter.getWrappedListener());

                // Put a converter in front of the renderer if one is provided
                converter.setWrappedListener(contentRenderer);
                contentRenderer = converter;
            }

            XWikiGeneratorListener xwikiListener = this.parser.createXWikiGeneratorListener(contentRenderer, null);
            context.getTagStack().pushScannerContext(new WikiScannerContext(xwikiListener));
            context.getTagStack().pushStackParameters();

            // Ensure we simulate a new document being parsed
            context.getScannerContext().beginDocument();
        } else {
            // TODO keep the content as is and not just the words
            super.begin(context);
        }
    }

    @Override
    protected void end(TagContext context)
    {
        if (this.parser.getMacroContentRendererFactory() != null) {
            // Ensure we simulate a document parsing end
            context.getScannerContext().endDocument();

            WikiScannerContext scannerContext = context.getTagStack().popScannerContext();
            context.getTagStack().popStackParameters();

            XWikiGeneratorListener xwikiListener = (XWikiGeneratorListener) scannerContext.getfListener();

            PrintRenderer contentRenderer;
            WrappingListener converter = this.parser.getConverter();
            if (converter != null) {
                // Get the wrapped renderer
                contentRenderer = (PrintRenderer) converter.getWrappedListener();

                // Put back the current listener
                converter.setWrappedListener((Listener) context.getTagStack().getStackParameter(CURRRENT_LISTENER));
            } else {
                contentRenderer = (PrintRenderer) xwikiListener.getListener();
            }

            endContent(contentRenderer.getPrinter().toString(), context);
        } else {
            // TODO keep the content as is and not just the words
            super.end(context);
        }
    }

    protected abstract void endContent(String content, TagContext context);

    @Override
    protected void handlePreservedContent(TagContext context, String preservedContent)
    {
        endContent(preservedContent, context);
    }
}
