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
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.confluence.filter.ConfluenceFilterReferenceConverter;
import org.xwiki.contrib.confluence.filter.MacroConverter;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage;
import org.xwiki.contrib.confluence.parser.confluence.internal.wikimodel.ConfluenceResourceReference;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceURLConverter;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.QueueListener;
import org.xwiki.rendering.listener.WrappingListener;
import org.xwiki.rendering.listener.chaining.EventType;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.listener.reference.UserResourceReference;
import org.xwiki.rendering.renderer.PrintRenderer;

/**
 * <p>
 * This listener takes Confluence-y events and "fixes them up" so they have a better shape to produce clean XWiki
 * syntax:
 * - removes auto-cursor-target classes still hanging around;
 * - removes paragraph wrapping lone block macros;
 * - converts inline comment markers to annotations (selection and selection contexts);
 * - produces header anchors that Confluence automatically renders when pages are rendered;
 * - handles blog post teasers
 * - calls macro converters to convert Confluence macros to xwiki syntax (often XWiki macros).
 * <p>
 * Some of these features require queueing, recording and replaying events and filter listener producing plain text
 * versions of what's coming up. All this is currently based on the event queue provided by ConfluenceWrappingListener,
 * which lives between ConfluenceConverterListener and the actual listener.
 * <p>
 * We have the following architecture:
 * <pre><code>
 *   ConfluenceXWikiGeneratorListener -|
 *                                     +-> ConfluenceConverterListener -> ConfluenceWrappingListener -> actual listener
 *      Legacy Confluence Syntax      -|                      \-> Macro converters ->/
 * </code></pre>
 * - ConfluenceXWikiGeneratorListener takes events produced by parsing the Confluence XHTML syntax
 * - ConfluenceConverterListener fixes up these events and calls macro converters
 * - ConfluenceWrappingListener receives events from the macro converters and the fixed up events from
 *   ConfluenceConverterListener. It can queue events for complex handling and generate selection contexts
 * - the actual listener receives the final XWiki syntax event stream.
 * <p>
 * This is a complex architecture that would benefit from neat and clean ideas to simplify it. It is possible that some
 * handling in ConfluenceConverterListener could be moved to ConfluenceXWikiGeneratorListener or
 * ConfluenceWrappingListener. However, only ConfluenceConverterListener and ConfluenceWrappingListener are common
 * between all the Confluence syntaxes (the legacy one and the XHTML one), so some stuff can't really be pushed there
 * without duplication. Some handling cannot be done in ConfluenceWrappingListener because that's what comes after macro
 * conversion and events shouldn't really be messed with after macro conversion.
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
    private static final String AC_REF = "ac:ref";

    @Inject
    private MacroConverter macroConverter;

    @Inject
    private Logger logger;

    @Inject
    private ConfluenceInputContext context;

    @Inject
    private ConfluenceFilterReferenceConverter confluenceConverter;

    @Inject
    private ConfluenceXMLPackage confluencePackage;

    @Inject
    private ConfluenceURLConverter urlConverter;

    @Inject
    private ComponentManager componentManager;

    private Map<String, String> inlineComments = new LinkedHashMap<>();

    /**
     * The images put as trailers in blog posts.
     */
    private Collection<ResourceReference> teasers;

    private final ConfluenceWrappingListener wrappingListener = new ConfluenceWrappingListener();
    private final Deque<QueueListener> queuedListeners = new ArrayDeque<>();

    private void addTeaser(ResourceReference ref)
    {
        if (teasers != null && queuedListeners.isEmpty()) {
            // Don't save trailers if we are recording events
            teasers.add(ref);
        }
    }

    /**
     * @param inlineComments the inline comments
     * @since 9.79.0
     */
    public void setInlineComments(Map<String, String> inlineComments)
    {
        this.inlineComments = inlineComments;
    }

    @Override
    public void onMacro(String id, Map<String, String> parameters, String content, boolean inline)
    {
        switch (id) {
            case "confluence_inline_comment_marker_start":
                beginInlineCommentMarker(parameters.get(AC_REF));
                return;

            case "confluence_inline_comment_marker_end":
                endInlineCommentMarker(parameters.get(AC_REF));
                return;

            default:
                this.macroConverter.toXWiki(id, parameters, content, inline, wrappingListener);
        }
    }

    @Override
    public void setWrappedListener(Listener listener)
    {
        if (super.getWrappedListener() != null) {
            throw new UnsupportedOperationException(
                    "setWrappedListener cannot be called a second time on ConfluenceConverterListener. "
                            + "Please use a new instance of ConfluenceConverterListener. "
                            + "You can use converterProvider.apply to do so.");
        }
        wrappingListener.setComponentManager(this.componentManager);
        wrappingListener.setWrappedListener(listener);
        super.setWrappedListener(wrappingListener);
    }

    @Override
    public Listener getWrappedListener()
    {
        return wrappingListener.getWrappedListener();
    }

    /**
     * @param macroIds a set of macro ids that will be updated whenever a new macro event will be called.
     *
     * @since 9.30.0
     */
    public void setMacroIds(Map<String, Integer> macroIds)
    {
        wrappingListener.setMacroIds(macroIds);
    }

    /**
     * @param teasers a list that will be filled with trailer images
     *
     * @since 9.91.0
     */
    public void setTeasers(Collection<ResourceReference> teasers)
    {
        this.teasers = teasers;
    }

    @Override
    public void beginParagraph(Map<String, String> parameters)
    {
        if (hasAutoCursorTargetClass(parameters)) {
            // Record the content of the paragraph to check if it is empty and should be removed, or if we should just
            // remove the class parameter.
            queueEvents();

            super.beginParagraph(removeClassParameter(parameters));
        } else {
            super.beginParagraph(parameters);
        }
    }

    private void queueEvents()
    {
        QueueListener contentListener = new QueueListener();
        queuedListeners.push(contentListener);
        wrappingListener.queueEvents(contentListener);
    }

    private QueueListener dequeueEvents()
    {
        QueueListener contentListener = queuedListeners.pop();
        wrappingListener.dequeueEvents(contentListener);
        return contentListener;
    }

    @Override
    public void endParagraph(Map<String, String> parameters)
    {
        // Check if we reached the end of a paragraph with the auto-cursor-target class.
        if (hasAutoCursorTargetClass(parameters) && !queuedListeners.isEmpty()) {
            // Restore the previous listener and get the recorded events.
            QueueListener contentListener = dequeueEvents();

            // Check if the content of the paragraph is empty.
            boolean isEmpty = contentListener.stream()
                // Skip the first event which is the beginning of the paragraph
                .skip(1)
                .allMatch(event -> event.eventType == EventType.ON_NEW_LINE
                    || event.eventType == EventType.ON_SPACE);
            if (!isEmpty) {
                // Relay the recorded events
                contentListener.consumeEvents(wrappingListener);
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
            queueEvents();
        }
    }

    @Override
    public void endHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        if (context.getProperties().isTitleAnchorGenerationEnabled() && !queuedListeners.isEmpty()) {
            // We generate the anchor (get the text content of generated events, produce an id macro), and then consume
            // events so the title content is correctly rendered.

            QueueListener contentListener = dequeueEvents();

            try {
                PrintRenderer renderer = componentManager.getInstance(PrintRenderer.class, "normalizer-plain/1.0");
                NormalizedPlainFilter normalizedPlainFilter = new NormalizedPlainFilter(renderer, null);
                contentListener.forEach(e -> e.eventType.fireEvent(normalizedPlainFilter, e.eventParameters));
                String titleText = normalizedPlainFilter.consumeString();
                String anchor = confluenceConverter.convertAnchor("", "", titleText);
                if (!anchor.isEmpty()) {
                    wrappingListener.onId(anchor);
                }
            } catch (ComponentLookupException e) {
                logger.error("Failed to get the [normalizer-plain/1.0] renderer, a header will be missing an anchor");
            }

            contentListener.forEach(e -> e.eventType.fireEvent(wrappingListener, e.eventParameters));
        }

        if (hasAutoCursorTargetClass(parameters)) {
            super.endHeader(level, id, removeClassParameter(parameters));
        } else {
            super.endHeader(level, id, parameters);
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
            String userName = "";
            String userKey = reference.getReference();
            if (StringUtils.isNotEmpty(userKey)) {
                String confluenceUserName = confluencePackage.resolveUserName(userKey, userKey);
                if (StringUtils.isNotEmpty(confluenceUserName)) {
                    userName = confluenceConverter.convertUserNameToReferenceName(confluenceUserName);
                }
            }
            if (userName != null) {
                reference.setReference(userName);
                return reference;
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
        ResourceReference ref = convert(reference);
        maybeAddTeaser(ref, parameters);
        super.onImage(ref, freestanding, parameters);
    }

    @Override
    public void onImage(ResourceReference reference, boolean freestanding, String id, Map<String, String> parameters)
    {
        ResourceReference ref = convert(reference);
        maybeAddTeaser(ref, parameters);
        super.onImage(ref, freestanding, id, parameters);
    }

    private void maybeAddTeaser(ResourceReference ref, Map<String, String> parameters)
    {
        String classAttr = parameters.get(CLASS_ATTRIBUTE);
        if (StringUtils.isNotEmpty(classAttr)) {
            for (String className : StringUtils.split(classAttr, " ,")) {
                if ("teaser".equals(className.trim())) {
                    addTeaser(ref);
                    break;
                }
            }
        }
    }

    private void beginInlineCommentMarker(String ref)
    {
        if (StringUtils.isEmpty(ref)) {
            // should not happen, an empty ac:ref parameter on an inline comment marker would not really make sense
            return;
        }

        // Save the before annotation context (but only if we haven't done it already)
        String selectionLeftContext = wrappingListener.getSelectionLeftContext();
        this.inlineComments.computeIfAbsent("selectionLeftContext--" + ref, k -> selectionLeftContext);
        // Catch the plain version of the annotated content
        try {
            this.wrappingListener.recordPlainTextEvents();
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to get the [normalizer-plain/1.0] renderer, annotations won't be extracted");
        }
    }

    private void endInlineCommentMarker(String ref)
    {
        if (StringUtils.isEmpty(ref)) {
            // should not happen, an empty ac:ref parameter on an inline comment marker would not really make sense
            return;
        }

        NormalizedPlainFilter normalizedFilter = wrappingListener.stopRecordingPlainTextEvents();
        if (normalizedFilter != null) {
            String annotationPart = normalizedFilter.consumeString();
            String currentAnnotation = this.inlineComments.getOrDefault(ref, "");
            this.inlineComments.put(ref, currentAnnotation + annotationPart);
        }
        wrappingListener.getSelectionRightContext(ref, r -> inlineComments.put("selectionRightContext--" + ref, r));
    }
}
