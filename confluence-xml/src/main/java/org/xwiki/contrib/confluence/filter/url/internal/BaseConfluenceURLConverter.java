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
package org.xwiki.contrib.confluence.filter.url.internal;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverter;
import org.xwiki.contrib.confluence.filter.url.AbstractConfluenceURLConverter;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convert the common Confluence URLs.
 * This component should probably be split to have one component per URL pattern, but it doesn't hurt too much having
 * only one component.
 * @version $Id$
 * @since 9.76.0
 */
@Component
@Singleton
@Named("base")
public class BaseConfluenceURLConverter extends AbstractConfluenceURLConverter
{
    private static final Pattern PATTERN_URL_DISPLAY = Pattern.compile("^display/([^/?]+)/([^?/#]+)(\\?.*)?$");

    private static final Pattern PATTERN_URL_VIEWPAGE =
        Pattern.compile("^pages/viewpage.action\\?pageId=(\\d+)(&.*)?$");

    private static final Pattern PATTERN_TINY_LINK = Pattern.compile("^x/([^?#]+)(\\?.*)?$");

    private static final Pattern PATTERN_URL_SPACES = Pattern.compile("^spaces/(.+)/pages/\\d+/([^?#]+)(\\?.*)?$");

    private static final Pattern PATTERN_URL_ATTACHMENT =
        Pattern.compile("^download/(?:attachments|thumbnails)/(\\d+)/([^?#]+)(\\?.*)?$");

    private static final Pattern PATTERN_URL_EMOTICON =
        Pattern.compile("^images/icons/emoticons/([^?#]+)(\\....)(\\?.*)?$");

    @Inject
    private ConfluenceConverter converter;

    @Inject
    private Logger logger;

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

    private ResourceReference simpleDocRef(Matcher m, String urlAnchor)
    {
        String spaceKey = decode(m.group(1));
        String pageTitle = decode(m.group(2));
        return converter.getResourceReference(spaceKey, pageTitle, "", urlAnchor);
    }

    private long tinyPartToPageId(String part)
    {
        // FIXME copy-pasted from ConfluenceShortURLMapper
        // Reverse-engineered and inspired by https://confluence.atlassian.com/x/2EkGOQ
        // not sure the replaceChars part is necessary, but it shouldn't hurt
        String base64WithoutPadding = StringUtils.replaceChars(part, "-_/", "/+\n");

        byte[] decoded = new byte[8];
        Base64.getUrlDecoder().decode(base64WithoutPadding.getBytes(), decoded);
        return ByteBuffer.wrap(decoded).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    private ResourceReference convertPageIdToResourceReference(List<String[]> urlParameters, String urlAnchor,
        long pageId)
    {
        // Clean id parameter
        urlParameters.removeIf(parameter -> parameter[0].equals("pageId"));
        return converter.getResourceReference(pageId, "", urlAnchor);
    }

    private List<String[]> parseURLParameters(String queryString)
    {
        if (queryString == null) {
            return Collections.emptyList();
        }

        String[] elements = StringUtils.split(queryString, '&');

        List<String[]> parameters = new ArrayList<>(elements.length);

        for (String element : elements) {
            parameters.add(StringUtils.split(element, '='));
        }

        return parameters;
    }

    private ResourceReference fixReference(String path, List<String[]> urlParameters, String urlAnchor)
    {
        return ObjectUtils.firstNonNull(
            // Try /display
            tryPattern(PATTERN_URL_DISPLAY, path, matcher -> simpleDocRef(matcher, urlAnchor)),

            // Try /spaces
            tryPattern(PATTERN_URL_SPACES, path, matcher -> simpleDocRef(matcher, urlAnchor)),

            // Try viewpage.action
            tryPattern(PATTERN_URL_VIEWPAGE, path, matcher -> {
                long pageId = Long.parseLong(matcher.group(1));
                return convertPageIdToResourceReference(urlParameters, urlAnchor, pageId);
            }),

            // Try short URL
            tryPattern(PATTERN_TINY_LINK, path, matcher -> {
                long pageId;
                try {
                    pageId = tinyPartToPageId(matcher.group(1));
                } catch (IllegalArgumentException e) {
                    logger.warn("Failed to decode the short link [{}]", path, e);
                    return null;
                }

                return convertPageIdToResourceReference(urlParameters, urlAnchor, pageId);
            }),

            // Try attachments
            tryPattern(PATTERN_URL_ATTACHMENT, path, matcher -> {
                long pageId = Long.parseLong(matcher.group(1));
                String filename = decode(matcher.group(2));
                return converter.getResourceReference(pageId, filename, urlAnchor);
            }),

            // emoticons
            tryPattern(PATTERN_URL_EMOTICON, path, m -> new ResourceReference(decode(m.group(1)), ResourceType.ICON))
        );
    }

    @Override
    public ResourceReference convertPath(String path)
    {
        URI uri;
        try {
            uri = new URI(path);
        } catch (URISyntaxException e) {
            // Should never happen
            this.logger.error("Wrong URI [{}]", path, e);
            return null;
        }

        List<String[]> urlParameters = parseURLParameters(uri.getQuery());
        String urlAnchor = uri.getFragment();

        return fixReference(path, urlParameters, urlAnchor);
    }
}
