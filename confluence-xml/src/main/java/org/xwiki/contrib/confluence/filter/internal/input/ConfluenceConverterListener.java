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
import java.util.ArrayList;
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
import org.xwiki.contrib.confluence.filter.input.ConfluenceProperties;
import org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rendering.listener.WrappingListener;
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

    @Override
    public void onMacro(String id, Map<String, String> parameters, String content, boolean inline)
    {
        this.macroConverter.toXWiki(id, parameters, content, inline, this);
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
        ResourceReference fixedReference = reference;

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
            reference.setReference(confluenceConverter.convert(reference.getReference(), EntityType.DOCUMENT));
        } else if (Objects.equals(reference.getType(), ResourceType.ATTACHMENT)) {
            // Make sure the reference follows the configured rules of conversion
            reference.setReference(confluenceConverter.convert(reference.getReference(), EntityType.ATTACHMENT));
        }

        return fixedReference;
    }

    private LocalDocumentReference fromPageId(String id) throws NumberFormatException, ConfigurationException
    {
        // Document name

        ConfluenceXMLPackage confluencePackage = context.getConfluencePackage();

        ConfluenceProperties pageProperties = confluencePackage.getPageProperties(Long.parseLong(id), false);

        if (pageProperties != null) {
            String documentName;
            if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_HOMEPAGE)) {
                documentName = context.getProperties().getSpacePageName();
            } else {
                documentName = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE);
            }

            // Space name

            Long spaceId = pageProperties.getLong("space", null);

            String spaceKey = confluencePackage.getSpaceKey(spaceId);

            // Reference

            return new LocalDocumentReference(confluenceConverter.toEntityName(spaceKey),
                confluenceConverter.toEntityName(documentName));
        }

        return null;
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
        LocalDocumentReference documentReference = new LocalDocumentReference(
            confluenceConverter.toEntityName(decode(m.group(1))), confluenceConverter.toEntityName(decode(m.group(2))));

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
                LocalDocumentReference documentReference;
                try {
                    documentReference = fromPageId(matcher.group(1));
                } catch (Exception e) {
                    this.logger.error("Failed to get page for id [{}]", matcher.group(1), e);
                    return null;
                }

                // Clean id parameter
                urlParameters.removeIf(parameter -> parameter[0].equals("pageId"));

                return createDocumentResourceReference(documentReference, urlParameters, urlAnchor);
            }),

            // Try attachments
            tryPattern(PATTERN_URL_ATTACHMENT, pattern, matcher -> {
                LocalDocumentReference documentReference;
                try {
                    documentReference = fromPageId(matcher.group(1));
                } catch (Exception e) {
                    this.logger.error("Failed to get attachment page for id [{}]", matcher.group(1), e);
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

    @Override
    public void beginLink(ResourceReference reference, boolean freestanding, Map<String, String> parameters)
    {
        if (reference instanceof UserResourceReference) {
            // Resolve proper user reference
            ResourceReference userRef = confluenceConverter.resolveUserReference((UserResourceReference) reference);

            super.beginLink(userRef, freestanding, parameters);
        } else {
            // Fix and optimize the link reference according to various rules
            super.beginLink(convert(reference), freestanding, parameters);
        }
    }

    @Override
    public void endLink(ResourceReference reference, boolean freestanding, Map<String, String> parameters)
    {
        if (reference instanceof UserResourceReference) {
            // Resolve proper user reference
            ResourceReference userRef = confluenceConverter.resolveUserReference((UserResourceReference) reference);

            super.endLink(userRef, freestanding, parameters);
        } else {
            // Fix and optimize the link reference according to various rules
            super.endLink(convert(reference), freestanding, parameters);
        }
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
