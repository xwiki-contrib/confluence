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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.confluence.filter.MacroConverter;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.contrib.confluence.filter.input.ConfluenceProperties;
import org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
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
    private static final Pattern PATTERN_URL_DISPLAY = Pattern.compile("^/display/(.+)/([^\\?#]+)(\\?.*)?$");

    private static final Pattern PATTERN_URL_VIEWPAGE =
        Pattern.compile("^/pages/viewpage.action\\?pageId=(\\d+)(&.*)?$");

    private static final Pattern PATTERN_URL_SPACES = Pattern.compile("^/spaces/(.+)/pages/\\d+/([^\\?#]+)(\\?.*)?$");

    private static final Pattern PATTERN_URL_ATTACHMENT =
        Pattern.compile("^/download/attachments/(\\d+)/([^\\?#]+)(\\?.*)?$");

    private static final Pattern PATTERN_URL_EMOTICON =
        Pattern.compile("^/images/icons/emoticons/([^\\?#]+)(\\....)(\\?.*)?$");

    @Inject
    private MacroConverter macroConverter;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    @Named("relative")
    private EntityReferenceResolver<String> explicitResolver;

    @Inject
    private Logger logger;

    private ConfluenceXMLPackage confluencePackage;

    private ConfluenceInputProperties properties;

    private ConfluenceInputFilterStream stream;

    /**
     * @param confluencePackage the Confluence data
     * @param properties the input properties
     * @since 9.10
     */
    public void initialize(ConfluenceXMLPackage confluencePackage, ConfluenceInputFilterStream stream,
        ConfluenceInputProperties properties)
    {
        this.confluencePackage = confluencePackage;
        this.stream = stream;
        this.properties = properties;
    }

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

    private void fixInternalLinks(ResourceReference reference, boolean freestanding, Map<String, String> parameters,
        boolean begin)
    {
        ResourceReference fixedReference = reference;
        Map<String, String> fixedParameters = parameters;

        if (CollectionUtils.isNotEmpty(this.properties.getBaseURLs())
            && Objects.equals(reference.getType(), ResourceType.URL)) {
            for (URL baseURL : this.properties.getBaseURLs()) {
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

                    String pattern = urlString.substring(baseURLString.length());
                    if (pattern.isEmpty() || pattern.charAt(0) != '/') {
                        pattern = "/" + pattern;
                    }

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
            if (StringUtils.isNotEmpty(reference.getReference())) {
                // Parse the reference
                EntityReference documentReference =
                    this.explicitResolver.resolve(reference.getReference(), EntityType.DOCUMENT);

                // Fix the reference according to entity conversion rules
                EntityReference newDocumentReference = null;
                for (EntityReference enityElement : documentReference.getReversedReferenceChain()) {
                    newDocumentReference = new EntityReference(this.stream.toEntityName(enityElement.getName()),
                        enityElement.getType(), newDocumentReference);
                }

                // Serialize the fixed reference
                reference.setReference(this.serializer.serialize(newDocumentReference));
            }
        }

        if (begin) {
            super.beginLink(fixedReference, freestanding, fixedParameters);
        } else {
            super.endLink(fixedReference, freestanding, fixedParameters);
        }
    }

    private LocalDocumentReference fromPageId(String id) throws NumberFormatException, ConfigurationException
    {
        // Document name

        ConfluenceProperties pageProperties = this.confluencePackage.getPageProperties(Long.valueOf(id), false);

        if (pageProperties != null) {
            String documentName;
            if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_HOMEPAGE)) {
                documentName = this.properties.getSpacePageName();
            } else {
                documentName = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE);
            }

            // Space name

            Long spaceId = pageProperties.getLong("space", null);

            String spaceKey = this.confluencePackage.getSpaceKey(spaceId);

            // Reference

            return new LocalDocumentReference(this.stream.toEntityName(spaceKey),
                this.stream.toEntityName(documentName));
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

    private void removeParameter(String parameterName, List<String[]> urlParameters)
    {
        for (ListIterator<String[]> it = urlParameters.listIterator(); it.hasNext();) {
            String[] parameter = it.next();

            if (parameter[0].equals(parameterName)) {
                it.remove();
            }
        }
    }

    private String decode(String encoded)
    {
        try {
            return URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            // If this happen we are is big trouble...
            throw new RuntimeException(e);
        }
    }

    private ResourceReference fixReference(String pattern, List<String[]> urlParameters, String urlAnchor)
    {
        // Try /display
        Matcher matcher = PATTERN_URL_DISPLAY.matcher(pattern);
        if (matcher.matches()) {
            LocalDocumentReference documentReference = new LocalDocumentReference(
                this.stream.toEntityName(decode(matcher.group(1))), this.stream.toEntityName(decode(matcher.group(2))));

            return createDocumentResourceReference(documentReference, urlParameters, urlAnchor);
        }

        // Try /spaces
        matcher = PATTERN_URL_SPACES.matcher(pattern);
        if (matcher.matches()) {
            LocalDocumentReference documentReference = new LocalDocumentReference(
                this.stream.toEntityName(decode(matcher.group(1))), this.stream.toEntityName(decode(matcher.group(2))));

            return createDocumentResourceReference(documentReference, urlParameters, urlAnchor);
        }

        // Try viewpage.action
        matcher = PATTERN_URL_VIEWPAGE.matcher(pattern);
        if (matcher.matches()) {
            LocalDocumentReference documentReference;
            try {
                documentReference = fromPageId(matcher.group(1));
            } catch (Exception e) {
                this.logger.error("Failed to get page for id [{}]", matcher.group(1), e);
                return null;
            }

            // Clean id parameter
            removeParameter("pageId", urlParameters);

            return createDocumentResourceReference(documentReference, urlParameters, urlAnchor);
        }

        // Try attachments
        matcher = PATTERN_URL_ATTACHMENT.matcher(pattern);
        if (matcher.matches()) {
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
        }

        // emoticons
        matcher = PATTERN_URL_EMOTICON.matcher(pattern);
        if (matcher.matches()) {
            return new ResourceReference(decode(matcher.group(1)), ResourceType.ICON);
        }

        return null;
    }

    @Override
    public void beginLink(ResourceReference reference, boolean freestanding, Map<String, String> parameters)
    {
        if (reference instanceof UserResourceReference) {
            // Resolve proper user reference
            ResourceReference userReference = resolveUserReference((UserResourceReference) reference);

            super.beginLink(userReference, freestanding, parameters);
        } else {
            // Fix URL entered by mistake instead of wiki links
            fixInternalLinks(reference, freestanding, parameters, true);
        }
    }

    /**
     * @param reference the reference of a user that can be either a username or a user key.
     * @return a XWiki user reference.
     * @since 9.19
     */
    public ResourceReference resolveUserReference(UserResourceReference reference)
    {
        String userReference = reference.getReference();

        if (this.properties.isUserReferences()) {
            // Keep the UserResourceReference

            // Clean the user id
            String userName =
                this.stream.toUserReferenceName(this.stream.resolveUserName(userReference, userReference));

            reference.setReference(userName);

            return reference;
        }

        // Convert to link to user profile
        // FIXME: would not really been needed if the XWiki Instance output filter was taking care of that when
        // receiving a user reference

        String userName = this.stream.toUserReference(this.stream.resolveUserName(userReference, userReference));
        DocumentResourceReference documentReference = new DocumentResourceReference(userName);

        documentReference.setParameters(reference.getParameters());

        return documentReference;
    }

    @Override
    public void endLink(ResourceReference reference, boolean freestanding, Map<String, String> parameters)
    {
        if (reference instanceof UserResourceReference) {
            // Resolve proper user reference
            ResourceReference userReference = resolveUserReference((UserResourceReference) reference);

            super.endLink(userReference, freestanding, parameters);
        } else {
            // Fix URL entered by mistake instead of wiki links
            fixInternalLinks(reference, freestanding, parameters, false);
        }
    }
}
