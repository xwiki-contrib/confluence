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
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.MacroTagHandler.ConfluenceMacro;
import org.xwiki.rendering.internal.parser.wikimodel.XWikiGeneratorListener;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.wikimodel.impl.WikiScannerContext;
import org.xwiki.rendering.wikimodel.xhtml.handler.PreserveTagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Handles text inside a macro which might contain markup.
 * <p>
 * Example:
 * 
 * <pre>
 * {@code <ac:rich-text-body><p>some <em>text</em> here</p></ac:rich-text-body>}
 * </pre>
 *
 * @version $Id$
 * @since 9.0
 */
public class RichTextBodyTagHandler extends PreserveTagHandler implements ConfluenceTagHandler
{
    private final ConfluenceXHTMLParser parser;

    public RichTextBodyTagHandler(ConfluenceXHTMLParser parser)
    {
        this.parser = parser;
    }

    @Override
    protected void begin(TagContext context)
    {
        DefaultWikiPrinter printer = new DefaultWikiPrinter();

        PrintRendererFactory rendererFactory = this.parser.getMacroContentRendererFactory();

        if (rendererFactory != null) {
            PrintRenderer contentRenderer = rendererFactory.createRenderer(printer);

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
            PrintRenderer contentRenderer = (PrintRenderer) xwikiListener.getListener();

            ConfluenceMacro macro = (ConfluenceMacro) context.getTagStack().getStackParameter(CONFLUENCE_CONTAINER);

            macro.content = contentRenderer.getPrinter().toString();
        } else {
            // TODO keep the content as is and not just the words
            super.end(context);
        }
    }

    @Override
    protected void handlePreservedContent(TagContext context, String preservedContent)
    {
        ConfluenceMacro macro = (ConfluenceMacro) context.getTagStack().getStackParameter(CONFLUENCE_CONTAINER);

        if (macro != null) {
            macro.content = preservedContent;
        }
    }
}
