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
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.confluence.filter.MacroConverter;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.contrib.confluence.filter.internal.ConfluenceXMLPackage;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.WrappingListener;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

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

    private static final Pattern PATTERN_URL_VIEWPAGE = Pattern.compile("^/pages/viewpage.action\\?pageId=(\\d+)(&.*)?$");

    private static final Pattern PATTERN_URL_ATTACHMENT =
        Pattern.compile("^/download/attachments/(\\d+)/([^\\?#]+)(\\?.*)?$");

    private static final Pattern PATTERN_URL_EMOTICON = Pattern.compile("^/images/icons/emoticons/([^\\?#]+)(\\....)(\\?.*)?$");

    @Inject
    private MacroConverter macroConverter;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private Logger logger;

    private ConfluenceXMLPackage confluencePackage;

    private ConfluenceInputProperties properties;

    /**
     * @param confluencePackage the Confluence data
     * @param properties the input properties
     * @param listener the listener
     */
    public void initialize(ConfluenceXMLPackage confluencePackage, ConfluenceInputProperties properties,
        Listener listener)
    {
        this.confluencePackage = confluencePackage;
        this.properties = properties;

        setWrappedListener(listener);
    }

    @Override
    public void onMacro(String id, Map<String, String> parameters, String content, boolean inline)
    {
        this.macroConverter.toXWiki(id, parameters, content, inline, getWrappedListener());
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

        PropertiesConfiguration pageProperties = this.confluencePackage.getPageProperties(Long.valueOf(id), false);

        if (pageProperties != null) {
            String documentName;
            if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_HOMEPAGE)) {
                documentName = this.properties.getSpacePageName();
            } else {
                documentName = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE);
            }

            // Space name

            Long spaceId = pageProperties.getLong("space", null);

            String spaceName = this.confluencePackage.getSpaceName(spaceId);

            // Reference

            return new LocalDocumentReference(spaceName, documentName);
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

    private ResourceReference fixReference(String pattern, List<String[]> urlParameters, String urlAnchor)
    {
        // Try /display
        Matcher matcher = PATTERN_URL_DISPLAY.matcher(pattern);
        if (matcher.matches()) {
            LocalDocumentReference documentReference = new LocalDocumentReference(matcher.group(1), matcher.group(2));

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
                new EntityReference(matcher.group(2), EntityType.ATTACHMENT, documentReference);

            return createAttachmentResourceReference(attachmentReference, urlParameters, urlAnchor);
        }

        // emoticons
        matcher = PATTERN_URL_EMOTICON.matcher(pattern);
        if (matcher.matches()) {
            return new ResourceReference(matcher.group(1), ResourceType.ICON);
        }

        return null;
    }

    @Override
    public void beginLink(ResourceReference reference, boolean freestanding, Map<String, String> parameters)
    {
        // Fix URL entered by mistake instead of wiki links
        fixInternalLinks(reference, freestanding, parameters, true);
    }

    @Override
    public void endLink(ResourceReference reference, boolean freestanding, Map<String, String> parameters)
    {
        // Fix URL entered by mistake instead of wiki links
        fixInternalLinks(reference, freestanding, parameters, false);
    }
}
