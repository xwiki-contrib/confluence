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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.confluence.filter.MacroConverter;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.QueueListener;
import org.xwiki.rendering.listener.WrappingListener;
import org.xwiki.rendering.listener.chaining.EventType;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.listener.reference.UserResourceReference;

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
    private static final Pattern PATTERN_URL_DISPLAY = Pattern.compile("^/display/(.+)/([^?#]+)(\\?.*)?$");

    private static final Pattern PATTERN_URL_VIEWPAGE =
        Pattern.compile("^/pages/viewpage.action\\?pageId=(\\d+)(&.*)?$");

    private static final Pattern PATTERN_URL_SPACES = Pattern.compile("^/spaces/(.+)/pages/\\d+/([^?#]+)(\\?.*)?$");

    private static final Pattern PATTERN_URL_ATTACHMENT =
        Pattern.compile("^/download/attachments/(\\d+)/([^?#]+)(\\?.*)?$");

    private static final Pattern PATTERN_URL_EMOTICON =
        Pattern.compile("^/images/icons/emoticons/([^?#]+)(\\....)(\\?.*)?$");

    /**
     * The auto-cursor-target class that appears on some paragraphs and headings in Confluence, most of the
     * paragraphs are empty and should be removed, but not all.
     */
    private static final String AUTO_CURSOR_TARGET_CLASS = "auto-cursor-target";

    private static final String CLASS_ATTRIBUTE = "class";

    @Inject
    private MacroConverter macroConverter;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private Logger logger;

    @Inject
    private ConfluenceInputContext context;

    @Inject
    private ConfluenceConverter confluenceConverter;

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
            if (macroIds != null) {
                macroIds.put(id, macroIds.getOrDefault(id, 0) + 1);
            }
            super.onMacro(id, parameters, content, inline);
        }
    };

    @Override
    public void onMacro(String id, Map<String, String> parameters, String content, boolean inline)
    {
        this.macroConverter.toXWiki(id, parameters, content, inline, this);
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
            this.contentListenerStack.push(new QueueListener());
            // We need to get the actual wrapped listener, not the wrappingListener instance, to be able to restore it
            // later.
            this.previousListenerStack.push(wrappingListener.getWrappedListener());
            setWrappedListener(this.contentListenerStack.element());

            super.beginParagraph(removeClassParameter(parameters));
        } else {
            super.beginParagraph(parameters);
        }
    }

    @Override
    public void endParagraph(Map<String, String> parameters)
    {
        // Check if we reached the end of a paragraph with the auto-cursor-target class.
        if (hasAutoCursorTargetClass(parameters) && !this.contentListenerStack.isEmpty()) {
            // Check if the content of the paragraph is empty.
            QueueListener contentListener = this.contentListenerStack.pop();
            boolean isEmpty = contentListener.stream()
                // Skip the first event which is the beginning of the paragraph
                .skip(1)
                .allMatch(event -> event.eventType == EventType.ON_NEW_LINE
                    || event.eventType == EventType.ON_SPACE);
            // Restore the previous listener.
            Listener previousListener = this.previousListenerStack.pop();
            setWrappedListener(previousListener);
            if (!isEmpty) {
                // Relay the recorded events directly to the previous listener to avoid counting macros twice.
                contentListener.consumeEvents(previousListener);
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
    }

    @Override
    public void endHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
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

    private List<String[]> parseURLParameters(String queryString)
    {
        if (queryString == null) {
            return null;
        }

        String[] elements = StringUtils.split(queryString, '&');

        List<String[]> parameters = new ArrayList<>(elements.length);

        for (String element : elements) {
            parameters.add(StringUtils.split(element, '='));
        }

        return parameters;
    }

    private String serializeURLParameters(List<String[]> parameters)
    {
        StringBuilder builder = new StringBuilder();

        for (String[] parameter : parameters) {
            if (builder.length() > 0) {
                builder.append('&');
            }

            builder.append(parameter[0]);
            builder.append('=');
            builder.append(parameter[1]);
        }

        return builder.toString();
    }

    private String enforceSlash(String pattern)
    {
        if (pattern.isEmpty() || pattern.charAt(0) != '/') {
            return "/" + pattern;
        }
        return pattern;
    }

    private ResourceReference convert(ResourceReference reference)
    {
        ResourceReference fixedReference = reference.clone();

        if (CollectionUtils.isNotEmpty(context.getProperties().getBaseURLs())
            && Objects.equals(reference.getType(), ResourceType.URL)) {
            for (URL baseURL : context.getProperties().getBaseURLs()) {
                String baseURLString = baseURL.toExternalForm();

                if (reference.getReference().startsWith(baseURLString)) {
                    // Fix the URL if the format is known

                    String urlString = reference.getReference();

                    URL url;
                    try {
                        url = new URL(urlString);
                    } catch (MalformedURLException e) {
                        // Should never happen
                        this.logger.error("Wrong URL resource reference [{}]", reference, e);

                        continue;
                    }

                    String pattern = enforceSlash(urlString.substring(baseURLString.length()));

                    List<String[]> urlParameters = parseURLParameters(url.getQuery());
                    String urlAnchor = url.getRef();

                    ResourceReference ref = fixReference(pattern, urlParameters, urlAnchor);

                    if (ref != null) {
                        fixedReference = ref;
                        break;
                    }
                }
            }
        } else if (Objects.equals(reference.getType(), ResourceType.DOCUMENT)) {
            // Make sure the reference follows the configured rules of conversion
            fixedReference.setReference(confluenceConverter.convert(reference.getReference(), EntityType.DOCUMENT));
        } else if (Objects.equals(reference.getType(), ResourceType.ATTACHMENT)) {
            // Make sure the reference follows the configured rules of conversion
            fixedReference.setReference(confluenceConverter.convert(reference.getReference(), EntityType.ATTACHMENT));
        }

        return fixedReference;
    }

    private EntityReference fromPageId(String id) throws NumberFormatException, ConfigurationException
    {
        return confluenceConverter.convertDocumentReference(Long.parseLong(id), false);
    }

    private AttachmentResourceReference createAttachmentResourceReference(EntityReference reference,
        List<String[]> urlParameters, String urlAnchor)
    {
        if (reference == null) {
            return null;
        }

        AttachmentResourceReference resourceReference =
            new AttachmentResourceReference(this.serializer.serialize(reference));

        // Query string
        if (CollectionUtils.isNotEmpty(urlParameters)) {
            resourceReference.setQueryString(serializeURLParameters(urlParameters));
        }

        // Anchor
        if (StringUtils.isNotBlank(urlAnchor)) {
            resourceReference.setAnchor(urlAnchor);
        }

        return resourceReference;
    }

    private DocumentResourceReference createDocumentResourceReference(EntityReference reference,
        List<String[]> urlParameters, String urlAnchor)
    {
        if (reference == null) {
            return null;
        }

        DocumentResourceReference resourceReference =
            new DocumentResourceReference(this.serializer.serialize(reference));

        // Query string
        if (CollectionUtils.isNotEmpty(urlParameters)) {
            resourceReference.setQueryString(serializeURLParameters(urlParameters));
        }

        // Anchor
        if (StringUtils.isNotBlank(urlAnchor)) {
            resourceReference.setAnchor(urlAnchor);
        }

        return resourceReference;
    }

    private String decode(String encoded)
    {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    private ResourceReference tryPattern(Pattern pattern, String p, Function<Matcher, ResourceReference> f)
    {
        Matcher matcher = pattern.matcher(p);
        if (matcher.matches()) {
            return f.apply(matcher);
        }

        return null;
    }

    private DocumentResourceReference simpleDocRef(Matcher m, List<String[]> urlParameters, String urlAnchor)
    {
        String spaceKey = decode(m.group(1));
        String docRef = decode(m.group(2));
        EntityReference documentReference = confluenceConverter.toDocumentReference(spaceKey, docRef);

        return createDocumentResourceReference(documentReference, urlParameters, urlAnchor);
    }

    private ResourceReference fixReference(String pattern, List<String[]> urlParameters, String urlAnchor)
    {
        return ObjectUtils.firstNonNull(
            // Try /display
            tryPattern(PATTERN_URL_DISPLAY, pattern, matcher -> simpleDocRef(matcher, urlParameters, urlAnchor)),

            // Try /spaces
            tryPattern(PATTERN_URL_SPACES, pattern, matcher -> simpleDocRef(matcher, urlParameters, urlAnchor)),

            // Try viewpage.action
            tryPattern(PATTERN_URL_VIEWPAGE, pattern, matcher -> {
                EntityReference documentReference = getEntityReference(matcher);
                if (documentReference == null) {
                    return null;
                }

                // Clean id parameter
                urlParameters.removeIf(parameter -> parameter[0].equals("pageId"));

                return createDocumentResourceReference(documentReference, urlParameters, urlAnchor);
            }),

            // Try attachments
            tryPattern(PATTERN_URL_ATTACHMENT, pattern, matcher -> {
                EntityReference documentReference = getEntityReference(matcher);
                if (documentReference == null) {
                    return null;
                }

                EntityReference attachmentReference =
                    new EntityReference(decode(matcher.group(2)), EntityType.ATTACHMENT, documentReference);

                return createAttachmentResourceReference(attachmentReference, urlParameters, urlAnchor);
            }),

            // emoticons
            tryPattern(PATTERN_URL_EMOTICON, pattern, m -> new ResourceReference(decode(m.group(1)), ResourceType.ICON))
        );
    }

    private EntityReference getEntityReference(Matcher matcher)
    {
        try {
            return fromPageId(matcher.group(1));
        } catch (ConfigurationException | NumberFormatException e) {
            this.logger.error("Failed to get page for id [{}]", matcher.group(1), e);
        }
        return null;
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
}
