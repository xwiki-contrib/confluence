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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceReferenceConverter;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.parser.wikimodel.DefaultXWikiGeneratorListener;
import org.xwiki.rendering.internal.parser.xhtml.wikimodel.XHTMLXWikiGeneratorListener;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.InlineFilterListener;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.QueueListener;
import org.xwiki.rendering.listener.WrappingListener;
import org.xwiki.rendering.listener.chaining.EventType;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.util.IdGenerator;
import org.xwiki.rendering.wikimodel.WikiParameter;
import org.xwiki.rendering.wikimodel.WikiParameters;
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

    private static final List<EventType> PLAIN_TEXT_EVENTS = List.of(
        EventType.ON_SPACE,
        EventType.ON_RAW_TEXT,
        EventType.ON_NEW_LINE,
        EventType.ON_EMPTY_LINES,
        EventType.ON_SPECIAL_SYMBOL,
        EventType.ON_VERBATIM,
        EventType.ON_WORD
    );

    private final Method pushListenerMethod;

    private final Method popListenerMethod;

    private final StreamParser plainParser;

    private final StreamParser xwikiParser;

    private final PrintRendererFactory plainRendererFactory;

    private final ConfluenceReferenceConverter confluenceConverter;

    private int mustCallPopListener;

    /**
     * @param parser the parser to use to parse link labels
     * @param listener the XWiki listener to which to forward WikiModel events
     * @param linkReferenceParser the parser to parse link references
     * @param imageReferenceParser the parser to parse image references
     * @param plainRendererFactory used to generate header ids
     * @param idGenerator used to generate header ids
     * @param syntax the syntax of the parsed source
     * @param plainParser the parser to use to parse link labels
     * @param xwikiParser the parser to use to parse XWiki syntax
     * @param confluenceConverter the confluence reference converter
     * @since 3.0M3
     */
    public ConfluenceXWikiGeneratorListener(StreamParser parser, Listener listener,
        ResourceReferenceParser linkReferenceParser, ResourceReferenceParser imageReferenceParser,
        PrintRendererFactory plainRendererFactory, IdGenerator idGenerator, Syntax syntax, StreamParser plainParser,
        StreamParser xwikiParser, ConfluenceReferenceConverter confluenceConverter)
    {
        super(parser, listener, linkReferenceParser, imageReferenceParser, plainRendererFactory, idGenerator, syntax);
        this.plainParser = plainParser;
        this.xwikiParser = xwikiParser;
        this.plainRendererFactory = plainRendererFactory;
        this.confluenceConverter = confluenceConverter;

        // We need pushListener and popListener but they are private. For a lack of better solution, we use reflection
        // to access them. When we stop supporting 14.10, we should remove these reflection tricks as these methods are
        // now protected.
        Method push;
        Method pop;
        try {
            push = DefaultXWikiGeneratorListener.class.getDeclaredMethod("pushListener", Listener.class);
            pop = DefaultXWikiGeneratorListener.class.getDeclaredMethod("popListener");
            push.setAccessible(true);
            pop.setAccessible(true);
        } catch (NoSuchMethodException e) {
            push = null;
            pop = null;
        }
        this.pushListenerMethod = push;
        this.popListenerMethod = pop;
    }

    private String convertDocumentReference(String spaceKey, String pageTitle)
    {
        if (this.confluenceConverter == null) {
            return spaceKey + '.' + pageTitle;
        }

        return this.confluenceConverter.convertDocumentReference(spaceKey, pageTitle);
    }

    private String convertSpaceReference(String spaceKey)
    {
        if (this.confluenceConverter == null) {
            return spaceKey;
        }

        return this.confluenceConverter.convertSpaceReference(spaceKey, true);
    }

    private String convertAnchor(String spaceKey, String pageTitle, String anchor)
    {
        if (this.confluenceConverter == null) {
            return anchor;
        }
        return this.confluenceConverter.convertAnchor(spaceKey, pageTitle, anchor);
    }

    @Override
    public void onReference(WikiReference reference)
    {
        if (!(reference instanceof ConfluenceXHTMLWikiReference)) {
            super.onReference(reference);
            return;
        }

        ConfluenceXHTMLWikiReference confluenceReference = (ConfluenceXHTMLWikiReference) reference;

        ResourceReference resourceReference;
        if (confluenceReference.getAttachment() != null) {
            resourceReference = getAttachmentResourceReference(confluenceReference.getAttachment());
        }  else {
            DocumentResourceReference documentResourceReference = null;

            if (confluenceReference.getPage() != null) {
                documentResourceReference = new DocumentResourceReference(
                    convertDocumentReference(
                        confluenceReference.getSpace(),
                        confluenceReference.getPage()
                    )
                );
            } else if (confluenceReference.getSpace() != null) {
                documentResourceReference = new DocumentResourceReference(
                    convertSpaceReference(confluenceReference.getSpace()));
            } else {
                documentResourceReference = new DocumentResourceReference("");
            }

            String anchor = confluenceReference.getAnchor();
            if (anchor != null) {
                documentResourceReference.setAnchor(convertAnchor(
                    confluenceReference.getSpace(),
                    confluenceReference.getPage(),
                    confluenceReference.getAnchor()
                ));
            }

            resourceReference = documentResourceReference;
        }

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

    private ResourceReference getAttachmentResourceReference(ConfluenceXHTMLAttachment attachment)
    {
        DocumentResourceReference documentResourceReference;
        if (attachment.page != null) {
            documentResourceReference = new DocumentResourceReference(
                convertDocumentReference(attachment.space, attachment.page));
        } else if (attachment.space != null) {
            documentResourceReference = new DocumentResourceReference(
                convertSpaceReference(attachment.space));
        } else {
            documentResourceReference = new DocumentResourceReference("");
        }
        String ref = documentResourceReference.getReference();
        if (!ref.isEmpty()) {
            ref += '@';
        }
        return new AttachmentResourceReference(ref + attachment.filename);
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
                resourceReference = getAttachmentResourceReference(confluenceReference.getAttachment());
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

    @Override
    public void beginParagraph(WikiParameters params)
    {
        maybePushListener();
        super.beginParagraph(params);
    }

    private void maybePushListener()
    {
        if (this.pushListenerMethod == null) {
            return;
        }

        this.mustCallPopListener++;

        try {
            this.pushListenerMethod.invoke(this, new QueueListener());
        } catch (InvocationTargetException | IllegalAccessException e) {
            // ignore
        }
    }

    private QueueListener maybePopListener()
    {
        Listener l = getListener();
        if (this.mustCallPopListener < 1 || this.popListenerMethod == null || !(l instanceof QueueListener)) {
            return null;
        }
        try {
            this.popListenerMethod.invoke(this);
            mustCallPopListener--;
            return (QueueListener) l;
        } catch (InvocationTargetException | IllegalAccessException e) {
            // ignore
        }
        return null;
    }

    @Override
    public void onMacroInline(String macroName, WikiParameters params, String content)
    {
        if ("confluence_code".equals(macroName)) {
            Map<String, String> classFormat = beginFormat(params);
            handleCodeMacro(content);
            endFormat(classFormat);
            return;
        }

        super.onMacroInline(macroName, params, content);
    }

    @Override
    public void onMacroBlock(String macroName, WikiParameters params, String content)
    {
        /**
         * Wikimodel doesn't handle nested lists and lists with unique paragraphs  well.
         * We bypass it by manually handling list-related tags.
         */
        switch (macroName) {
            case "confluence_ul_start":
                getListener().beginList(ListType.BULLETED, Collections.emptyMap());
                return;

            case "confluence_ol_start":
                getListener().beginList(ListType.NUMBERED, Collections.emptyMap());
                return;

            case "confluence_ul_end":
                super.endList(params, false);
                return;

            case "confluence_ol_end":
                super.endList(params, true);
                return;

            case "confluence_li_start":
                // we begin the list item and then record the following events. This will allow us to do some cleanup
                // that will avoid clunky syntax and broken rendering. See handleListItem().
                getListener().beginListItem();
                maybePushListener();
                return;

            case "confluence_li_end":
                handleListItem();
                return;

            default:
                // ignore
        }

        super.onMacroBlock(macroName, params, content);
    }

    private void endFormat(Map<String, String> format)
    {
        if (format != null) {
            getListener().endFormat(Format.NONE, format);
        }
    }

    private Map<String, String> beginFormat(WikiParameters params)
    {
        WikiParameter classParam = params.getParameter("class");
        WikiParameter styleParam = params.getParameter("style");
        Map<String, String> format = new HashMap<>(2);

        if (styleParam != null && !StringUtils.isEmpty(styleParam.getValue())) {
            format.put("style", styleParam.getValue());
        }

        if (classParam != null && !StringUtils.isEmpty(classParam.getValue())) {
            format.put("class", classParam.getValue());
        }

        if (format.isEmpty()) {
            return null;
        }

        getListener().beginFormat(Format.NONE, format);
        return format;
    }

    private List<QueueListener.Event> parseContent(String content)
    {
        QueueListener queueListener = new QueueListener();
        if (xwikiParser != null) {
            try {
                xwikiParser.parse(new StringReader(content), queueListener);
            } catch (ParseException e) {
                return null;
            }
        }
        int s = queueListener.size();

        if (queueListener.size() > 4
            && queueListener.get(0).eventType.equals(EventType.BEGIN_DOCUMENT)
            && queueListener.get(1).eventType.equals(EventType.BEGIN_PARAGRAPH)
            && queueListener.get(s - 2).eventType.equals(EventType.END_PARAGRAPH)
            && queueListener.get(s - 1).eventType.equals(EventType.END_DOCUMENT)
        ) {
            // we skip BEGIN_DOCUMENT,  BEGIN_PARAGRAPH at the start and END_PARAGRAPH, END_DOCUMENT at the end
            return queueListener.subList(2, s - 2);
        }
        return null;
    }

    private void handleListItem()
    {
        /*
         * We make sure the list item content is wrapped in a group if necessary.
         * This avoids broken syntax like having empty list items and then the content of the list item standalone.
         *
         * We handle some common situations where the events can be simplified so the group is not necessary.
         * In particular, groups are not necessary when:
         *  - a paragraph is alone in the list item. In this case, we can simply remove it to allow cleaner syntax and
         *    unnecessary vertical spacing.
         *  - a list is alone in the list item. In this case, not producing a group leads to a much cleaner syntax for
         *    nested lists
         *  - the list item starts with a paragraph and is followed by a unique list. Then, the paragraph can be removed
         *    to unnecessary vertical spacing and allow the cleaner nested list syntax as well.
         */

        QueueListener queueListener = maybePopListener();
        if (queueListener != null) {
            removeParagraphImmediatelyFollowedByList(queueListener);

            boolean wrapInGroup = needsGroupWrapInListItem(queueListener);

            if (wrapInGroup) {
                getListener().beginGroup(Collections.emptyMap());
            }

            fireEvents(queueListener);

            if (wrapInGroup) {
                getListener().endGroup(Collections.emptyMap());
            }
        }
        getListener().endListItem();
    }

    private void removeParagraphImmediatelyFollowedByList(QueueListener queueListener)
    {
        int s = queueListener.size();
        if (s > 1 && queueListener.get(0).eventType.equals(EventType.BEGIN_PARAGRAPH)) {
            int i = 1;
            while (i < s && !queueListener.get(i).eventType.equals(EventType.END_PARAGRAPH)) {
                i++;
            }

            if (i < s && queueListener.get(i).eventType.equals(EventType.END_PARAGRAPH)
                && (i + 1 >= s || queueListener.get(i + 1).eventType.equals(EventType.BEGIN_LIST))
            ) {
                queueListener.remove(i);
                queueListener.remove(0);
            }
        }
    }

    private boolean needsGroupWrapInListItem(QueueListener contentEvents)
    {
        for (int i = 0; i < contentEvents.size(); i++) {
            QueueListener.Event e = contentEvents.get(i);
            if (e.eventType.isInlineEnd()) {
                return !wrappedInList(contentEvents.subList(i, contentEvents.size()));
            }
        }
        return false;
    }

    private boolean wrappedInList(List<QueueListener.Event> contentEvents)
    {
        int s = contentEvents.size();
        if (s < 2
            || !contentEvents.get(0).eventType.equals(EventType.BEGIN_LIST)
            || !contentEvents.get(s - 1).eventType.equals(EventType.END_LIST)
        ) {
            return false;
        }

        int level = 0;
        boolean forbidLists = false;
        for (QueueListener.Event e : contentEvents) {
            if (e.eventType.equals(EventType.BEGIN_LIST)) {
                if (forbidLists) {
                    return false;
                }
                level++;
            } else  if (e.eventType.equals(EventType.END_LIST)) {
                level--;
                if (level == 0) {
                    forbidLists = true;
                }
            }
        }
        return true;
    }

    private void handleCodeMacro(String content)
    {
        if (content.isEmpty()) {
            // let's ignore empty code tags
            return;
        }

        List<QueueListener.Event> contentEvents = parseContent(content);
        if (contentEvents == null) {
            outputCodeMacro(content);
            return;
        }
        Iterator<QueueListener.Event> relevantEventsIterator = contentEvents.iterator();
        boolean useFormat = false;
        while (relevantEventsIterator.hasNext()) {
            QueueListener.Event e = relevantEventsIterator.next();
            if (isClassPre(e)) {
                // Let's just ignore <span class="pre"> elements in <code>
                relevantEventsIterator.remove();
            } else if (!PLAIN_TEXT_EVENTS.contains(e.eventType)) {
                useFormat = true;
                break;
            }
        }

        if (useFormat) {
            this.getListener().beginFormat(Format.MONOSPACE, Collections.emptyMap());
            fireEvents(contentEvents);
            this.getListener().endFormat(Format.MONOSPACE, Collections.emptyMap());
            return;
        }
        outputCodeMacro(eventsToText(contentEvents));
    }

    private void fireEvents(Iterable<QueueListener.Event> contentEvents, Listener listener)
    {
        for (QueueListener.Event e : contentEvents) {
            e.eventType.fireEvent(listener, e.eventParameters);
        }
    }

    private void fireEvents(Iterable<QueueListener.Event> contentEvents)
    {
        fireEvents(contentEvents, getListener());
    }

    private String eventsToText(List<QueueListener.Event> contentEvents)
    {
        PrintRenderer plainTextRenderer = plainRendererFactory.createRenderer(new DefaultWikiPrinter());
        fireEvents(contentEvents, plainTextRenderer);
        return plainTextRenderer.getPrinter().toString();
    }

    private void outputCodeMacro(String content)
    {
        WikiParameters params = new WikiParameters().addParameter("language", "none");
        if (content.contains("{{/code}}")) {
            params = params.addParameter("source", "string:" + content);
            super.onMacroInline("code", params, null);
            return;
        }
        super.onMacroInline("code", params, content);
    }

    private static boolean isClassPre(QueueListener.Event e)
    {
        if (e.eventType != EventType.BEGIN_FORMAT && e.eventType != EventType.END_FORMAT) {
            return false;
        }

        for (Object eventParameter : e.eventParameters) {
            if (!isFormatNone(eventParameter) && !isClassPre(eventParameter)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isFormatNone(Object eventParameter)
    {
        return eventParameter instanceof Format && Format.NONE.equals(eventParameter);
    }

    private static boolean isClassPre(Object eventParameter)
    {
        if (eventParameter instanceof Map) {
            Map<String, String> params = (Map<String, String>) eventParameter;
            return params.size() == 1 && "pre".equals(params.get("class"));
        }
        return false;
    }

    @Override
    public void endParagraph(WikiParameters params)
    {
        super.endParagraph(params);

        // We work around an issue in Confluence exports. They contain things like <p>[macro]</p>, where macro
        // can be a block macro. We detect this pattern and remove the paragraphs in this case.
        // If macro was inline, it doesn't hurt because the macro will be in its own line in the XWiki syntax anyway.

        QueueListener queueListener = maybePopListener();
        if (queueListener == null) {
            // reflection failed, fall back to the default behavior
            return;
        }

        // Check if the second event is a macro event. Then this macro should actually be a block macro without
        // begin/end paragraph.
        if (queueListener.size() == 3 && queueListener.get(1).eventType == EventType.ON_MACRO) {
            QueueListener.Event macroEvent = queueListener.get(1);
            // Set the inline parameter to false in macros that are alone in paragraph. These macros can be block
            // macros. If they are inline, not having the inline attribute should not hurt when generating XWiki syntax.
            if (macroEvent.eventParameters.length == 4 && macroEvent.eventParameters[3] instanceof Boolean) {
                macroEvent.eventParameters[3] = false;
            }
            macroEvent.eventType.fireEvent(this.getListener(), macroEvent.eventParameters);
        } else {
            queueListener.consumeEvents(this.getListener());
        }
    }
}
