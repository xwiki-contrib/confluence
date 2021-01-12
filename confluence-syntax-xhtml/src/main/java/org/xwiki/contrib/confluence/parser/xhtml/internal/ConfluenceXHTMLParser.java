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
package org.xwiki.contrib.confluence.parser.xhtml.internal;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.xml.sax.XMLReader;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceXHTMLInputProperties;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.AttachmentTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceXHTMLWhitespaceXMLFilter;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceXWikiGeneratorListener;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.TimeTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.DefaultMacroParameterTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ImageTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.LinkTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.MacroParameterTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.MacroTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.PageTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.PlainTextBodyTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.PlainTextLinkBodyTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.PreformattedTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.RichTextBodyTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.SpaceTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.TableCellTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.TableHeadTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.URLTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.UserTagHandler;
import org.xwiki.rendering.internal.parser.wikimodel.AbstractWikiModelParser;
import org.xwiki.rendering.internal.parser.wikimodel.XWikiGeneratorListener;
import org.xwiki.rendering.internal.parser.xhtml.wikimodel.XWikiHeaderTagHandler;
import org.xwiki.rendering.internal.parser.xhtml.wikimodel.XWikiReferenceTagHandler;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.util.IdGenerator;
import org.xwiki.rendering.wikimodel.IWikiParser;
import org.xwiki.rendering.wikimodel.xhtml.XhtmlParser;
import org.xwiki.rendering.wikimodel.xhtml.filter.AccumulationXMLFilter;
import org.xwiki.rendering.wikimodel.xhtml.filter.DTDXMLFilter;
import org.xwiki.rendering.wikimodel.xhtml.handler.TagHandler;

/**
 * Parses Confluence XHTML and generate rendering events.
 *
 * @version $Id$
 * @since 9.0
 */
@Component
@Named(ConfluenceXHTMLParser.SYNTAX_STRING)
@Singleton
public class ConfluenceXHTMLParser extends AbstractWikiModelParser
{
    /**
     * The identifier of the syntax.
     */
    public static final String SYNTAX_STRING = ConfluenceXHTMLInputProperties.FILTER_STREAM_TYPE_STRING;

    @Inject
    @Named("xdom+xml/current")
    private StreamParser xmlParser;

    @Inject
    @Named("xdom+xml/current")
    private PrintRendererFactory xmlRenderer;

    /**
     * @see #getLinkReferenceParser()
     */
    @Inject
    @Named("link")
    private ResourceReferenceParser linkReferenceParser;

    /**
     * @see #getImageReferenceParser()
     */
    @Inject
    @Named("image")
    private ResourceReferenceParser imageReferenceParser;

    @Inject
    @Named("plain/1.0")
    private StreamParser plainParser;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    private PrintRendererFactory macroContentRendererFactory;

    @Override
    public Syntax getSyntax()
    {
        return Syntax.CONFLUENCEXHTML_1_0;
    }

    @Override
    public StreamParser getLinkLabelParser()
    {
        return this.xmlParser;
    }

    @Override
    public IWikiParser createWikiModelParser() throws ParseException
    {
        XhtmlParser parser = new XhtmlParser();

        parser.setNamespacesEnabled(false);

        // Override some of the WikiModel XHTML parser tag handlers to introduce our own logic.
        Map<String, TagHandler> handlers = new HashMap<>();

        TagHandler handler = new XWikiHeaderTagHandler();
        handlers.put("h1", handler);
        handlers.put("h2", handler);
        handlers.put("h3", handler);
        handlers.put("h4", handler);
        handlers.put("h5", handler);
        handlers.put("h6", handler);
        handlers.put("a", new XWikiReferenceTagHandler(this, this.xmlRenderer));

        handlers.put("ac:macro", new MacroTagHandler());
        handlers.put("ac:structured-macro", new MacroTagHandler());
        handlers.put("ac:default-parameter", new DefaultMacroParameterTagHandler());
        handlers.put("ac:parameter", new MacroParameterTagHandler());
        handlers.put("ac:plain-text-body", new PlainTextBodyTagHandler());
        handlers.put("ac:rich-text-body", new RichTextBodyTagHandler(this));

        handlers.put("ac:image", new ImageTagHandler());
        handlers.put("ri:url", new URLTagHandler());

        handlers.put("ac:link", new LinkTagHandler());
        handlers.put("ri:page", new PageTagHandler());
        handlers.put("ri:space", new SpaceTagHandler());
        handlers.put("ri:user", new UserTagHandler());
        handlers.put("ac:plain-text-link-body", new PlainTextLinkBodyTagHandler());

        handlers.put("ri:attachment", new AttachmentTagHandler());

        handlers.put("th", new TableHeadTagHandler());
        handlers.put("td", new TableCellTagHandler());
        
        handlers.put("pre", new PreformattedTagHandler());
        
        handlers.put("time", new TimeTagHandler());

        parser.setExtraHandlers(handlers);

        try {
            parser.setXmlReader(createXMLReader());
        } catch (Exception e) {
            throw new ParseException("Failed to create XMLReader", e);
        }

        return parser;
    }

    private XMLReader createXMLReader() throws Exception
    {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        SAXParser parser = parserFactory.newSAXParser();
        XMLReader xmlReader = parser.getXMLReader();

        // Ignore SAX callbacks when the parser parses the DTD
        DTDXMLFilter dtdFilter = new DTDXMLFilter(xmlReader);

        // Add a XML Filter to accumulate onCharacters() calls since SAX
        // parser may call it several times.
        AccumulationXMLFilter accumulationFilter = new AccumulationXMLFilter(dtdFilter);

        // Add a XML Filter to remove non-semantic white spaces. We need to
        // do that since all WikiModel
        // events contain only semantic information.
        return new ConfluenceXHTMLWhitespaceXMLFilter(accumulationFilter);
    }

    @Override
    protected void parse(final Reader source, Listener listener, IdGenerator idGenerator) throws ParseException
    {
        String content;
        try {
            content = IOUtils.toString(source);
        } catch (IOException e) {
            throw new ParseException("Failed to read source", e);
        }

        // Add <void> element around the content to make sure to have valid xml
        content = "<void>" + content + "</void>";

        // Add XHTML entities
        content = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\""
            + " \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" + content;

        super.parse(new StringReader(content), listener, idGenerator);
    }

    @Override
    public ResourceReferenceParser getLinkReferenceParser()
    {
        return this.linkReferenceParser;
    }

    @Override
    public ResourceReferenceParser getImageReferenceParser()
    {
        return this.imageReferenceParser;
    }

    @Override
    public XWikiGeneratorListener createXWikiGeneratorListener(Listener listener, IdGenerator idGenerator)
    {
        return new ConfluenceXWikiGeneratorListener(getLinkLabelParser(), listener, getLinkReferenceParser(),
            getImageReferenceParser(), this.plainRendererFactory, idGenerator, getSyntax(), this.plainParser);
    }

    /**
     * @param macroContentSyntax the syntax to use to convert rich macro content
     * @throws ComponentLookupException when failing to find a rendering factory conrresponding to the provider syntax
     */
    public void setMacroContentSyntax(Syntax macroContentSyntax) throws ComponentLookupException
    {
        if (macroContentSyntax != null) {
            this.macroContentRendererFactory = this.componentManagerProvider.get()
                .getInstance(PrintRendererFactory.class, macroContentSyntax.toIdString());
        } else {
            this.macroContentRendererFactory = null;
        }
    }

    /**
     * @return the macroContentRendererFactory the rendering factory to use to convert rich macro content
     */
    public PrintRendererFactory getMacroContentRendererFactory()
    {
        return this.macroContentRendererFactory;
    }
}
