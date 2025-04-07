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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.confluence.filter.ConfluenceFilterReferenceConverter;
import org.xwiki.contrib.confluence.filter.MacroConverter;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.parser.confluence.internal.wikimodel.ConfluenceResourceReference;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceURLConverter;
import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceInlineCommentTagHandler;
import org.xwiki.rendering.listener.CompositeListener;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.QueueListener;
import org.xwiki.rendering.listener.WrappingListener;
import org.xwiki.rendering.listener.chaining.EventType;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.listener.reference.UserResourceReference;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

import static org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverter.getConfluenceServerAnchor;
import static org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverter.spacesToDash;

/**
 * Convert various Confluence content elements to their XWiki equivalent.
 *
 * @version $Id$
 * @since 9.1
 */
@Component(roles = ConfluenceConverterListener.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ConfluenceConverterListener extends WrappingListener
{
    /**
     * The auto-cursor-target class that appears on some paragraphs and headings in Confluence, most of the
     * paragraphs are empty and should be removed, but not all.
     */
    private static final String AUTO_CURSOR_TARGET_CLASS = "auto-cursor-target";

    private static final String CLASS_ATTRIBUTE = "class";

    private static final String ID_MACRO_NAME = "id";
    private static final String ID_MACRO_NAME_PARAMETER = "name";

    @Inject
    private MacroConverter macroConverter;

    @Inject
    private Logger logger;

    @Inject
    private ConfluenceInputContext context;

    @Inject
    private ConfluenceFilterReferenceConverter confluenceConverter;

    @Inject
    @Named("plain/1.0")
    private PrintRenderer plainTextRenderer;

    @Inject
    private ConfluenceURLConverter urlConverter;

    @Inject
    private ComponentManager componentManager;

    private Map<String, String> inlineComments = new LinkedHashMap<>();

    /**
     * A stack of queues that are used to record the content of a paragraph with the auto-cursor-target class. For
     * the unlikely case that paragraphs are nested (e.g., because there is a paragraph in a nested macro), a stack
     * is used instead of a single listener.
     */
    private final Deque<QueueListener> contentListenerStack = new ArrayDeque<>();

    /**
     * A stack of previous listeners that is used to record the previous wrapped listener when a new listener is set
     * while examining the content of a paragraph with the auto-cursor-target class. This is used to restore the
     * previous listener at the end of the paragraph. Again, a stack is used instead of a single listener to handle
     * the unlikely case of nested paragraphs.
     */
    private final Deque<Listener> previousListenerStack = new ArrayDeque<>();

    private Map<String, Integer> macroIds;
    private final WrappingListener wrappingListener = new WrappingListener() {
        @Override
        public void onMacro(String id, Map<String, String> parameters, String content, boolean inline)
        {
            countMacro(id);

            if (ID_MACRO_NAME.equals(id)) {
                handleIdMacro(parameters, content, inline);
            } else {
                super.onMacro(id, parameters, content, inline);
            }
        }

        private void handleIdMacro(Map<String, String> parameters, String content, boolean inline)
        {
            String name = parameters.get(ID_MACRO_NAME_PARAMETER);
            if (name == null || name.isEmpty()) {
                return;
            }

            String currentPageTitle = ((ConfluenceConverter) confluenceConverter).getCurrentPageTitleForAnchor();

            if (context.isConfluenceCloud()) {
                String dashedName = spacesToDash(name);
                getWrappedListener().onMacro(
                    ID_MACRO_NAME,
                    Map.of(ID_MACRO_NAME_PARAMETER, dashedName),
                    content,
                    inline
                );

                if (currentPageTitle != null) {
                    getWrappedListener().onId(spacesToDash(currentPageTitle) + '-' + dashedName);
                }
            } else if (currentPageTitle != null) {
                getWrappedListener().onMacro(
                    ID_MACRO_NAME,
                    Map.of(ID_MACRO_NAME_PARAMETER, getConfluenceServerAnchor(currentPageTitle, name)),
                    content,
                    inline
                );
            }
        }
    };

    private void countMacro(String id)
    {
        if (macroIds != null && !isQueuingEvents()) {
            // Don't count the macros if we are recording events, we only count them when actually rendering.
            macroIds.put(id, macroIds.getOrDefault(id, 0) + 1);
        }
    }

    private class NormalizedPlainFilter extends CompositeListener
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
    }

    /**
     * @param inlineComments
     * @since 9.79.0
     */
    public void setInlineComments(Map<String, String> inlineComments)
    {
        this.inlineComments = inlineComments;
    }

    @Override
    public void onMacro(String id, Map<String, String> parameters, String content, boolean inline)
    {
        this.macroConverter.toXWiki(id, parameters, content, inline, wrappingListener);
    }

    @Override
    public void setWrappedListener(Listener listener)
    {
        wrappingListener.setWrappedListener(listener);
        super.setWrappedListener(wrappingListener);
    }

    /**
     * @param macroIds a set of macro ids that will be updated whenever a new macro event will be called.
     *
     * @since 9.30.0
     */
    public void setMacroIds(Map<String, Integer> macroIds)
    {
        this.macroIds = macroIds;
    }

    @Override
    public void beginParagraph(Map<String, String> parameters)
    {
        if (hasAutoCursorTargetClass(parameters)) {
            // Record the content of the paragraph to check if it is empty and should be removed, or if we should just
            // remove the class parameter.
            this.queueEvents();

            super.beginParagraph(removeClassParameter(parameters));
        } else {
            super.beginParagraph(parameters);
        }
    }

    private void queueEvents()
    {
        this.contentListenerStack.push(new QueueListener());
        // We need to get the actual wrapped listener, not the wrappingListener instance, to be able to restore it
        // later.
        this.previousListenerStack.push(wrappingListener.getWrappedListener());
        setWrappedListener(this.contentListenerStack.element());
    }

    private QueueListener dequeueEvents()
    {
        Listener previousListener = this.previousListenerStack.pop();
        setWrappedListener(previousListener);
        return this.contentListenerStack.pop();
    }

    private boolean isQueuingEvents()
    {
        return !this.contentListenerStack.isEmpty();
    }

    @Override
    public void endParagraph(Map<String, String> parameters)
    {
        // Check if we reached the end of a paragraph with the auto-cursor-target class.
        if (hasAutoCursorTargetClass(parameters) && this.isQueuingEvents()) {
            // Restore the previous listener and get the recorded events.
            QueueListener contentListener = this.dequeueEvents();

            // Check if the content of the paragraph is empty.
            boolean isEmpty = contentListener.stream()
                // Skip the first event which is the beginning of the paragraph
                .skip(1)
                .allMatch(event -> event.eventType == EventType.ON_NEW_LINE
                    || event.eventType == EventType.ON_SPACE);
            if (!isEmpty) {
                // Relay the recorded events
                contentListener.consumeEvents(this.getWrappedListener());
                super.endParagraph(removeClassParameter(parameters));
            }
        } else {
            super.endParagraph(parameters);
        }
    }

    @Override
    public void beginHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        if (hasAutoCursorTargetClass(parameters)) {
            super.beginHeader(level, id, removeClassParameter(parameters));
        } else {
            super.beginHeader(level, id, parameters);
        }

        if (context.getProperties().isTitleAnchorGenerationEnabled()) {
            // To generate an automatic anchor compatible with anchors generated by Confluence, we record the content
            // from which we will build the anchor name.
            this.queueEvents();
        }
    }

    @Override
    public void endHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        if (context.getProperties().isTitleAnchorGenerationEnabled() && this.isQueuingEvents()) {
            // We generate the anchor (get the text content of generated events, produce an id macro), and then consume
            // events so the title content is correctly rendered.

            QueueListener contentListener = this.dequeueEvents();

            DefaultWikiPrinter printer = new DefaultWikiPrinter();
            plainTextRenderer.setPrinter(printer);
            contentListener.forEach(this::fireEvent);
            String titleText = printer.toString();
            String anchor = confluenceConverter.convertAnchor("", "", titleText);

            if (!anchor.isEmpty()) {
                wrappingListener.getWrappedListener().onMacro(
                    ID_MACRO_NAME,
                    Map.of(ID_MACRO_NAME_PARAMETER, anchor),
                    null,
                    true);
            }

            contentListener.consumeEvents(wrappingListener.getWrappedListener());
        }

        if (hasAutoCursorTargetClass(parameters)) {
            super.endHeader(level, id, removeClassParameter(parameters));
        } else {
            super.endHeader(level, id, parameters);
        }
    }

    private void fireEvent(QueueListener.Event event)
    {
        event.eventType.fireEvent(plainTextRenderer, event.eventParameters);
        if (event.eventType.equals(EventType.ON_MACRO)) {
            countMacro((String) event.eventParameters[0]);
        }
    }

    private static boolean hasAutoCursorTargetClass(Map<String, String> parameters)
    {
        return parameters.get(CLASS_ATTRIBUTE) != null
            && parameters.get(CLASS_ATTRIBUTE).contains(AUTO_CURSOR_TARGET_CLASS);
    }

    private static Map<String, String> removeClassParameter(Map<String, String> parameters)
    {
        Map<String, String> cleanedParameters;
        // Remove the class parameter.
        if (parameters.size() == 1) {
            cleanedParameters = Listener.EMPTY_PARAMETERS;
        } else {
            cleanedParameters = new LinkedHashMap<>(parameters);
            cleanedParameters.remove(CLASS_ATTRIBUTE);
        }
        return cleanedParameters;
    }

    private ResourceReference convert(ResourceReference reference)
    {
        if (reference instanceof ConfluenceResourceReference) {
            return convertConfluenceResourceReference((ConfluenceResourceReference) reference);
        }

        ResourceReference fixedReference = reference.clone();
        if (reference.getType() == ResourceType.URL) {
            String url = reference.getReference();
            ResourceReference res = urlConverter.convertURL(url);
            if (res != null) {
                return res;
            }

            return reference;
        }

        return fixedReference;
    }

    private ResourceReference convertConfluenceResourceReference(ConfluenceResourceReference ref)
    {
        if (ref.getType() == ResourceType.SPACE) {
            return new ResourceReference(
                confluenceConverter.convertSpaceReference(ref.getSpaceKey()),
                ResourceType.SPACE);
        }

        if (ref.getType() == ResourceType.DOCUMENT) {
            return new ResourceReference(
                confluenceConverter.convertDocumentReference(ref.getSpaceKey(), ref.getPageTitle()),
                ResourceType.DOCUMENT);
        }

        if (ref.getType() == ResourceType.ATTACHMENT) {
            return new ResourceReference(
                confluenceConverter.convertAttachmentReference(ref.getSpaceKey(), ref.getPageTitle(),
                    ref.getFilename()),
                ResourceType.ATTACHMENT
            );
        }

        logger.error("Unexpected Confluence resource reference type for [{}]", ref);
        return new ResourceReference(ref.toString(), ResourceType.UNKNOWN);
    }

    private ResourceReference convertLinkRef(ResourceReference reference)
    {
        if (reference instanceof UserResourceReference) {
            // Resolve proper user reference
            ResourceReference userRef = confluenceConverter.resolveUserReference((UserResourceReference) reference);
            if (userRef != null) {
                return userRef;
            }
            // FIXME should we handle things like this when userRef is null?
            // (probably meaning that the Confluence user is mapped to nothing)
        }

        return convert(reference);
    }

    @Override
    public void endLink(ResourceReference reference, boolean freestanding, Map<String, String> parameters)
    {
        // Fix and optimize the link reference according to various rules
        super.endLink(convertLinkRef(reference), freestanding, parameters);
    }

    @Override
    public void onImage(ResourceReference reference, boolean freestanding, Map<String, String> parameters)
    {
        // Fix and optimize the link reference according to various rules
        super.onImage(convert(reference), freestanding, parameters);
    }

    @Override
    public void onImage(ResourceReference reference, boolean freestanding, String id, Map<String, String> parameters)
    {
        // Fix and optimize the link reference according to various rules
        super.onImage(convert(reference), freestanding, id, parameters);
    }

    private Map<String, String> removeInlineCommentParameter(Map<String, String> parameters)
    {
        Map<String, String> cleanedParameters;

        // There is no point transmitting this marker to the final listener, it would just be noise
        if (parameters.size() == 1) {
            cleanedParameters = Listener.EMPTY_PARAMETERS;
        } else {
            cleanedParameters = new LinkedHashMap<>(parameters);
            cleanedParameters.remove(ConfluenceInlineCommentTagHandler.PARAMETER_REF);
        }

        return cleanedParameters;
    }

    @Override
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        Map<String, String> finalParameters = parameters;

        boolean annotation = finalParameters.containsKey(ConfluenceInlineCommentTagHandler.PARAMETER_REF);
        if (annotation) {
            // This is an inline comment marker
            finalParameters = removeInlineCommentParameter(parameters);
        }

        super.beginFormat(format, finalParameters);

        if (annotation) {
            // Catch the plain version of the annotated content
            try {
                // Get the renderer in charge of normalizing the annotation content
                PrintRenderer renderer = this.componentManager.getInstance(PrintRenderer.class, "normalizer-plain/1.0");

                // Add the normalizer renderer to the receiving renders
                this.wrappingListener.setWrappedListener(
                    new NormalizedPlainFilter(renderer, this.wrappingListener.getWrappedListener()));
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to get the [normalizer-plain/1.0] renderer, annotations won't be exteracted");
            }
        }
    }

    @Override
    public void endFormat(Format format, Map<String, String> parameters)
    {
        Map<String, String> finalParameters = parameters;

        String ref = finalParameters.get(ConfluenceInlineCommentTagHandler.PARAMETER_REF);
        if (ref != null) {
            // This is an inline comment marker
            finalParameters = removeInlineCommentParameter(finalParameters);

            Listener listener = this.wrappingListener.getWrappedListener();
            if (listener instanceof NormalizedPlainFilter) {
                NormalizedPlainFilter normalizedFilter = (NormalizedPlainFilter) listener;

                String currentAnnotation = this.inlineComments.get(ref);
                this.inlineComments.put(ref, currentAnnotation != null
                    ? currentAnnotation + normalizedFilter.printer.toString() : normalizedFilter.printer.toString());

                // Restore previous wrapped listener
                this.wrappingListener.setWrappedListener(normalizedFilter.wrappedListener);
            }
        }

        super.endFormat(format, finalParameters);
    }
}
