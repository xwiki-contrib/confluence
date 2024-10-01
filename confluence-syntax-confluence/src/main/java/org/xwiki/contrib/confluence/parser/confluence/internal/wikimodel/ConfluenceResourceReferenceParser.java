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
package org.xwiki.contrib.confluence.parser.confluence.internal.wikimodel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.internal.parser.reference.AbstractResourceReferenceParser;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

/**
 * Converts reference strings into typed references.
 * @version $Id$
 * @since 9.54.0
 */
@Component
@Named("confluence/link")
@Singleton
public class ConfluenceResourceReferenceParser extends AbstractResourceReferenceParser
{

    private static final Pattern ESCAPE_PAGE_CHARS = Pattern.compile("[.:@\\\\]");
    private static final Pattern ESCAPE_ATTACHMENT_CHARS = Pattern.compile("[\\\\^@]");

    private static final Pattern MAILTO_PATTERN = Pattern.compile("mailto:(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_PATTERN = Pattern.compile("(https?|file):.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPACE_PATTERN = Pattern.compile("([^:]+):");
    private static final Pattern PAGE_PATTERN = Pattern.compile("([^:^\"]+)");
    private static final Pattern PAGE_ON_SPACE_PATTERN = Pattern.compile("([^:]+):([^^]+)");
    private static final Pattern ATTACHMENT_PATTERN = Pattern.compile("\\^([^^]+)");
    private static final Pattern ATTACHMENT_ON_PAGE_PATTERN = Pattern.compile("(.+?)\\^([^^]+)");
    private static final Pattern USER_PATTERN = Pattern.compile("^~(.+)");

    @Inject
    private Logger logger;

    @Override
    public ResourceReference parse(String rawReference)
    {
        String actualReference;
        ResourceType type;
        boolean isTyped = true;

        // not nice: use a loop to allow "break"
        // only to avoid too many nested ifs
        do {
            Matcher m;
            m = MAILTO_PATTERN.matcher(rawReference);
            if (m.matches()) {
                type = ResourceType.MAILTO;
                actualReference = m.group(1);
                break;
            }
            m = URL_PATTERN.matcher(rawReference);
            if (m.matches()) {
                type = ResourceType.URL;
                actualReference = rawReference;
                break;
            }
            m = SPACE_PATTERN.matcher(rawReference);
            if (m.matches()) {
                type = ResourceType.SPACE;
                actualReference = m.group(1);
                break;
            }
            m = PAGE_ON_SPACE_PATTERN.matcher(rawReference);
            if (m.matches()) {
                type = ResourceType.DOCUMENT;
                actualReference = escapePage(m.group(1)) + '.' + escapePage(m.group(2));
                break;
            }
            m = ATTACHMENT_PATTERN.matcher(rawReference);
            if (m.matches()) {
                type = ResourceType.ATTACHMENT;
                actualReference = escapeAttachment(m.group(1));
                break;
            }
            m = ATTACHMENT_ON_PAGE_PATTERN.matcher(rawReference);
            if (m.matches()) {
                String pageName = m.group(1);
                ResourceReference page = parse(pageName);
                if (page.getType() == ResourceType.DOCUMENT) {
                    type = ResourceType.ATTACHMENT;
                    actualReference = page.getReference() + '@' + escapeAttachment(m.group(2));
                    break;
                }
            }
            m = USER_PATTERN.matcher(rawReference);
            if (m.matches()) {
                type = ResourceType.USER;
                actualReference = m.group(1);
                break;
            }
            // do this last: anything that does not contain any special chars is normally a page reference
            m = PAGE_PATTERN.matcher(rawReference);
            if (m.matches()) {
                type = ResourceType.DOCUMENT;
                actualReference = escapePage(rawReference);
                break;
            }
            actualReference = rawReference;
            type = ResourceType.URL;
            isTyped = false;
        } while (false);

        ResourceReference result = new ResourceReference(actualReference, type);
        result.setTyped(isTyped);
        logger.debug("parsing [{}] results in [{}]", rawReference, result);

        return result;
    }

    static final String escapePage(String text)
    {
        return escapeByRegexp(ESCAPE_PAGE_CHARS, text);
    }

    static final String escapeAttachment(String text)
    {
        return escapeByRegexp(ESCAPE_ATTACHMENT_CHARS, text);
    }

    private static String escapeByRegexp(Pattern escaping, String text)
    {
        return escaping.matcher(text).replaceAll("\\\\$0");
    }
}
