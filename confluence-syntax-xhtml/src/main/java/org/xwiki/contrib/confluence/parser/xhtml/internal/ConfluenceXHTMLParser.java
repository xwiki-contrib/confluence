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
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceMacroSupport;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceReferenceConverter;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceURLConverter;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceXHTMLInputProperties;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ADFAttributeHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ADFContentHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ADFMarkHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ADFNodeHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.AttachmentTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.CaptionHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.CodeTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceAttributeXMLFilter;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceImgTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceInlineCommentTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceListItemTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceOrderedListTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceParagraphTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceTableColHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceTableRowTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceTableTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceUnorderedListTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceXHTMLWhitespaceXMLFilter;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceXWikiGeneratorListener;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.DefaultMacroParameterTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ElementMacroTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.EmoticonTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.FallbackConfluenceReferenceConverter;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.FallbackConfluenceURLConverter;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.IgnoredTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ImageTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.LinkBodyTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.LinkTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.MacroParameterTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.MacroTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.PageTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.PlaceholderTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.PlainTextBodyTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.PlainTextLinkBodyTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.PreformattedTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ReferenceTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.RichTextBodyTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.SpaceTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.TableCellTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.TableHeadTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.TaskBodyTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.TaskIdTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.TaskStatusTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.TaskTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.TimeTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.URLTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.UserTagHandler;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.VarTagHandler;
import org.xwiki.rendering.internal.parser.wikimodel.AbstractWikiModelParser;
import org.xwiki.rendering.internal.parser.wikimodel.WikiModelStreamParser;
import org.xwiki.rendering.internal.parser.wikimodel.XWikiGeneratorListener;
import org.xwiki.rendering.internal.parser.xhtml.wikimodel.XWikiHeaderTagHandler;
import org.xwiki.rendering.internal.parser.xhtml.wikimodel.XWikiReferenceTagHandler;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.WrappingListener;
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
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ConfluenceXHTMLParser extends AbstractWikiModelParser
{
    /**
     * The identifier of the syntax.
     */
    public static final String SYNTAX_STRING = ConfluenceXHTMLInputProperties.FILTER_STREAM_TYPE_STRING;

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

    private WrappingListener converter;

    private ConfluenceReferenceConverter referenceConverter;

    private ConfluenceURLConverter urlConverter;

    private ConfluenceMacroSupport macroSupport;

    @Override
    public Syntax getSyntax()
    {
        return Syntax.CONFLUENCEXHTML_1_0;
    }

    @Override
    public StreamParser getLinkLabelParser()
    {
        return null;
    }

    private XWikiReferenceTagHandler createXWikiReferenceTagHandler() throws ParseException
    {
        // The second parameter in XWikiReferenceTagHandler(WikiModelStreamParser, PrintRendererFactory) have been
        // removed at some point in XWiki, so we need to check which constructor we have in the current instance

        Constructor<XWikiReferenceTagHandler> constructor =
            ConstructorUtils.getAccessibleConstructor(XWikiReferenceTagHandler.class, WikiModelStreamParser.class);

        if (constructor != null) {
            try {
                return constructor.newInstance(this);
            } catch (Exception e) {
                throw new ParseException("Failed to create a XWikiReferenceTagHandler", e);
            }
        }

        return new XWikiReferenceTagHandler(this);
    }

    @Override
    public IWikiParser createWikiModelParser() throws ParseException
    {
        ConfluenceReferenceConverter refConverter = referenceConverter == null
            ? new FallbackConfluenceReferenceConverter()
            : referenceConverter;

        XhtmlParser parser = new XhtmlParser();

        parser.setNamespacesEnabled(false);

        // Override some of the WikiModel XHTML parser tag handlers to introduce our own logic.
        Map<String, TagHandler> handlers = new HashMap<>();

        // NOTE: when adding support for an inline tag (that may have no content), consider adding it
        // to EMPTYVISIBLE_ELEMENTS in ConfluenceXHTMLWhitespaceXMLFilter so spaces before it are not eaten.

        TagHandler handler = new XWikiHeaderTagHandler();
        handlers.put("h1", handler);
        handlers.put("h2", handler);
        handlers.put("h3", handler);
        handlers.put("h4", handler);
        handlers.put("h5", handler);
        handlers.put("h6", handler);
        handlers.put("a", new ReferenceTagHandler(createXWikiReferenceTagHandler()));
        handlers.put("p", new ConfluenceParagraphTagHandler());
        handlers.put("li", new ConfluenceListItemTagHandler());
        handlers.put("ul", new ConfluenceUnorderedListTagHandler());
        handlers.put("ol", new ConfluenceOrderedListTagHandler());

        handlers.put("ac:emoticon", new EmoticonTagHandler());
        handlers.put("ac:macro", new MacroTagHandler(this.macroSupport, refConverter));
        handlers.put("ac:structured-macro", new MacroTagHandler(this.macroSupport, refConverter));
        handlers.put("ac:default-parameter", new DefaultMacroParameterTagHandler());
        handlers.put("ac:placeholder", new PlaceholderTagHandler());
        handlers.put("ac:parameter", new MacroParameterTagHandler());
        handlers.put("ac:plain-text-body", new PlainTextBodyTagHandler());
        handlers.put("ac:rich-text-body", new RichTextBodyTagHandler(this));

        handlers.put("at:var", new VarTagHandler());

        handlers.put("ac:image", new ImageTagHandler());
        handlers.put("ac:caption", new CaptionHandler(this));
        handlers.put("ri:url", new URLTagHandler());

        handlers.put("ac:link", new LinkTagHandler(refConverter));
        PageTagHandler pageTagHandler = new PageTagHandler();
        handlers.put("ri:page", pageTagHandler);
        handlers.put("ri:blog-post", pageTagHandler);
        handlers.put("ri:space", new SpaceTagHandler(refConverter));
        handlers.put("ri:user", new UserTagHandler(refConverter));
        handlers.put("ac:plain-text-link-body", new PlainTextLinkBodyTagHandler());
        handlers.put("ac:link-body", new LinkBodyTagHandler(this));

        // Directly convert into https://extensions.xwiki.org/xwiki/bin/view/Extension/Container%20Macro ?
        handlers.put("ac:layout", new ElementMacroTagHandler(this));
        handlers.put("ac:layout-section", new ElementMacroTagHandler(this));
        handlers.put("ac:layout-cell", new ElementMacroTagHandler(this));

        handlers.put("ri:attachment", new AttachmentTagHandler(refConverter));

        handlers.put("table", new ConfluenceTableTagHandler());
        handlers.put("col", new ConfluenceTableColHandler());
        handlers.put("tr", new ConfluenceTableRowTagHandler());
        handlers.put("th", new TableHeadTagHandler());
        handlers.put("td", new TableCellTagHandler());

        handlers.put("pre", new PreformattedTagHandler());
        handlers.put("code", new CodeTagHandler(this));

        handlers.put("time", new TimeTagHandler(this.macroSupport, refConverter));

        handlers.put("img", new ConfluenceImgTagHandler());

        handlers.put("ac:task-list", new ElementMacroTagHandler(this));
        handlers.put("ac:task", new TaskTagHandler(this.macroSupport, refConverter));
        handlers.put("ac:task-id", new TaskIdTagHandler());
        handlers.put("ac:task-status", new TaskStatusTagHandler());
        handlers.put("ac:task-body", new TaskBodyTagHandler(this));

        // ac:adf-extension tags are ignored, but their content parsed. Nothing to do.
        handlers.put("ac:adf-fallback", new IgnoredTagHandler(this));
        handlers.put("ac:adf-node", new ADFNodeHandler(this.macroSupport, refConverter));
        handlers.put("ac:adf-attribute", new ADFAttributeHandler());
        handlers.put("ac:adf-content", new ADFContentHandler(this));
        handlers.put("ac:adf-mark", new ADFMarkHandler());

        handlers.put("ac:inline-comment-marker", new ConfluenceInlineCommentTagHandler());

        parser.setExtraHandlers(handlers);

        try {
            parser.setXmlReader(createXMLReader());
        } catch (Exception e) {
            throw new ParseException("Failed to create XMLReader", e);
        }

        return parser;
    }

    private XMLReader createXMLReader() throws ParserConfigurationException, SAXException
    {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        SAXParser parser = parserFactory.newSAXParser();
        XMLReader xmlReader = parser.getXMLReader();

        // Ignore SAX callbacks when the parser parses the DTD
        DTDXMLFilter dtdFilter = new DTDXMLFilter(xmlReader);

        // Add an XML Filter to accumulate onCharacters() calls since SAX
        // parser may call it several times.
        AccumulationXMLFilter accumulationFilter = new AccumulationXMLFilter(dtdFilter);

        // Add an XML Filter to clean up and convert some attributes to styles
        ConfluenceAttributeXMLFilter attributeXMLFilter = new ConfluenceAttributeXMLFilter(accumulationFilter);

        // Add an XML Filter to remove non-semantic white spaces. We need to
        // do that since all WikiModel
        // events contain only semantic information.
        return new ConfluenceXHTMLWhitespaceXMLFilter(attributeXMLFilter);
    }

    @Override
    public void parse(final Reader source, Listener listener, IdGenerator idGenerator) throws ParseException
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
        StreamParser xwikiParser;
        try {
            xwikiParser = componentManagerProvider.get().getInstance(StreamParser.class, "xwiki/2.1");
        } catch (ComponentLookupException e) {
            xwikiParser = null;
        }

        return new ConfluenceXWikiGeneratorListener(
            getLinkLabelParser(),
            listener,
            getLinkReferenceParser(),
            getImageReferenceParser(),
            this.plainRendererFactory,
            idGenerator,
            getSyntax(),
            this.plainParser,
            xwikiParser,
            referenceConverter == null ? new FallbackConfluenceReferenceConverter() : referenceConverter,
            urlConverter == null ? new FallbackConfluenceURLConverter() : urlConverter
        );
    }

    /**
     * @param macroContentSyntax the syntax to use to convert rich macro content
     * @throws ComponentLookupException when failing to find a rendering factory corresponding to the provider
     *     syntax
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
     * @param referenceConverter the converter to use to convert user, space and document references
     * @since 9.29.0
     */
    public void setReferenceConverter(ConfluenceReferenceConverter referenceConverter)
    {
        this.referenceConverter = referenceConverter;
    }

    /**
     * @param urlConverter the converter to use to convert URLs
     * @since 9.76.0
     */
    public void setURLConverter(ConfluenceURLConverter urlConverter)
    {
        this.urlConverter = urlConverter;
    }

    /**
     * @param macroSupport the object providing information about macros
     * @since 9.43.0
     */
    public void setMacroSupport(ConfluenceMacroSupport macroSupport)
    {
        this.macroSupport = macroSupport;
    }

    /**
     * @return the macroContentRendererFactory the rendering factory to use to convert rich macro content
     */
    public PrintRendererFactory getMacroContentRendererFactory()
    {
        return this.macroContentRendererFactory;
    }

    /**
     * @return a filter to use between the parser and the renderer
     * @since 9.10
     */
    public WrappingListener getConverter()
    {
        return this.converter;
    }

    /**
     * @param converter a filter to use between the parser and the renderer
     * @since 9.10
     */
    public void setConverter(WrappingListener converter)
    {
        this.converter = converter;
    }
}
