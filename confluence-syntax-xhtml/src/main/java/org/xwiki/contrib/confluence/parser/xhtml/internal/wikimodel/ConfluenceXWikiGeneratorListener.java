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

import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.AttachmentTagHandler.ConfluenceAttachment;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.parser.xhtml.wikimodel.XHTMLXWikiGeneratorListener;
import org.xwiki.rendering.listener.InlineFilterListener;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.WrappingListener;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.util.IdGenerator;
import org.xwiki.rendering.wikimodel.WikiReference;

/**
 * WikiModel listener bridge for the XHTML Syntax.
 *
 * @version $Id$
 * @since 9.0
 */
public class ConfluenceXWikiGeneratorListener extends XHTMLXWikiGeneratorListener
{
    // Not using model API to not trigger a dependency on platform
    // TODO: Should probably introduce a component implemented on confluence-xml module side but based on actual model
    // API this time to be safe at least in this use case

    private static final String[] REPLACE_DOCUMENT_PREVIOUS = new String[] {"\\", "."};

    private static final String[] REPLACE_DOCUMENT_NEXT = new String[] {"\\\\", "\\."};

    private static final String[] REPLACE_SPACE_PREVIOUS = new String[] {"\\", ".", ":"};

    private static final String[] REPLACE_SPACE_NEXT = new String[] {"\\\\", "\\.", "\\:"};

    private StreamParser plainParser;

    /**
     * @param parser the parser to use to parse link labels
     * @param listener the XWiki listener to which to forward WikiModel events
     * @param linkReferenceParser the parser to parse link references
     * @param imageReferenceParser the parser to parse image references
     * @param plainRendererFactory used to generate header ids
     * @param idGenerator used to generate header ids
     * @param syntax the syntax of the parsed source
     * @param plainParser the parser to use to parse link labels
     * @since 3.0M3
     */
    public ConfluenceXWikiGeneratorListener(StreamParser parser, Listener listener,
        ResourceReferenceParser linkReferenceParser, ResourceReferenceParser imageReferenceParser,
        PrintRendererFactory plainRendererFactory, IdGenerator idGenerator, Syntax syntax, StreamParser plainParser)
    {
        super(parser, listener, linkReferenceParser, imageReferenceParser, plainRendererFactory, idGenerator, syntax);

        this.plainParser = plainParser;
    }

    private String escapeSpace(String space)
    {
        return StringUtils.replaceEach(space, REPLACE_SPACE_PREVIOUS, REPLACE_SPACE_NEXT);
    }

    private String escapeDocument(String document)
    {
        return StringUtils.replaceEach(document, REPLACE_DOCUMENT_PREVIOUS, REPLACE_DOCUMENT_NEXT);
    }

    @Override
    public void onReference(WikiReference reference)
    {
        if (reference instanceof ConfluenceLinkWikiReference) {
            ConfluenceLinkWikiReference confluenceReference = (ConfluenceLinkWikiReference) reference;

            ResourceReference resourceReference = null;

            if (confluenceReference.getDocument() != null) {
                StringBuilder str = new StringBuilder();
                if (confluenceReference.getSpace() != null) {
                    str.append(escapeSpace(confluenceReference.getSpace()));
                    str.append('.');
                }
                if (confluenceReference.getDocument() != null) {
                    str.append(escapeDocument(confluenceReference.getDocument()));
                } else if (confluenceReference.getSpace() != null) {
                    str.append("WebHome");
                }

                DocumentResourceReference documentResourceReference = new DocumentResourceReference(str.toString());

                if (confluenceReference.getAnchor() != null) {
                    documentResourceReference.setAnchor(confluenceReference.getAnchor());
                }

                resourceReference = documentResourceReference;
            } else if (confluenceReference.getSpace() != null) {
                DocumentResourceReference documentResourceReference =
                    new DocumentResourceReference(escapeSpace(confluenceReference.getSpace()) + ".WebHome");

                if (confluenceReference.getAnchor() != null) {
                    documentResourceReference.setAnchor(confluenceReference.getAnchor());
                }

                resourceReference = documentResourceReference;
            } else if (confluenceReference.getAttachment() != null) {
                ConfluenceAttachment attachment = confluenceReference.getAttachment();

                StringBuilder str = new StringBuilder();
                if (attachment.space != null) {
                    str.append(attachment.space);
                    str.append('.');
                }
                if (attachment.page != null) {
                    str.append(attachment.page);
                    str.append('@');
                } else if (attachment.space != null) {
                    str.append("WebHome");
                    str.append('@');
                }

                if (attachment.user != null) {
                    // TODO
                }

                str.append(attachment.filename);

                AttachmentResourceReference attachmentResourceReference =
                    new AttachmentResourceReference(str.toString());

                if (confluenceReference.getAnchor() != null) {
                    attachmentResourceReference.setAnchor(confluenceReference.getAnchor());
                }

                resourceReference = attachmentResourceReference;
            }  else if (confluenceReference.getAnchor() != null) {
                DocumentResourceReference documentResourceReference = new DocumentResourceReference("");
                documentResourceReference.setAnchor(confluenceReference.getAnchor());
                resourceReference = documentResourceReference;
            }

            if (resourceReference != null) {
                getListener().beginLink(resourceReference, false, Collections.<String, String>emptyMap());
                XDOM labelXDOM = confluenceReference.getLabelXDOM();
                String label = confluenceReference.getLabel();
                if (labelXDOM != null) {
                    InlineFilterListener inlineFilterListener = new InlineFilterListener();
                    inlineFilterListener.setWrappedListener(getListener());
                    labelXDOM.traverse(inlineFilterListener);
                } else if (label != null) {
                    parsePlainInline(label, getListener());
                }
                getListener().endLink(resourceReference, false, Collections.<String, String>emptyMap());
            }
        } else {
            super.onReference(reference);
        }
    }

    /**
     * Parse the given content inline using the given listener.
     * @param content the content to parse
     * @param listener the listener to wrap
     */
    public void parsePlainInline(String content, Listener listener)
    {
        WrappingListener inlineFilterListener = new InlineFilterListener();
        inlineFilterListener.setWrappedListener(listener);

        try {
            this.plainParser.parse(new StringReader(content), inlineFilterListener);
        } catch (ParseException e) {
            // TODO supposedly impossible with plain test parser
        }
    }

    @Override
    public void onImage(WikiReference reference)
    {
        if (reference instanceof ConfluenceImageWikiReference) {
            ConfluenceImageWikiReference confluenceReference = (ConfluenceImageWikiReference) reference;

            ResourceReference resourceReference = null;

            if (confluenceReference.getAttachment() != null) {
                ConfluenceAttachment attachment = confluenceReference.getAttachment();

                StringBuilder str = new StringBuilder();
                if (attachment.space != null) {
                    str.append(attachment.space);
                    str.append('.');
                }
                if (attachment.page != null) {
                    str.append(attachment.page);
                    str.append('@');
                } else if (attachment.space != null) {
                    str.append("WebHome");
                    str.append('@');
                }

                if (attachment.user != null) {
                    // TODO
                }

                str.append(attachment.filename);

                resourceReference = new AttachmentResourceReference(str.toString());
            } else if (confluenceReference.getURL() != null) {
                resourceReference = new ResourceReference(confluenceReference.getURL(), ResourceType.URL);
            }

            if (resourceReference != null) {
                if (confluenceReference.getCaption() != null) {
                    Map<String, String> figureParameters = Collections.singletonMap("class", "image");
                    this.getListener().beginFigure(figureParameters);
                    onImage(resourceReference, false, confluenceReference.getImageParameters());
                    this.getListener().beginFigureCaption(Listener.EMPTY_PARAMETERS);

                    InlineFilterListener inlineFilterListener = new InlineFilterListener();
                    inlineFilterListener.setWrappedListener(this.getListener());
                    confluenceReference.getCaption().traverse(inlineFilterListener);

                    this.getListener().endFigureCaption(Listener.EMPTY_PARAMETERS);
                    this.getListener().endFigure(figureParameters);
                } else {
                    onImage(resourceReference, false, confluenceReference.getImageParameters());
                }
            }
        } else {
            super.onImage(reference);
        }
    }
}
