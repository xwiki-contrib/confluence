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
    private static final Pattern MAILTO_PATTERN = Pattern.compile("mailto:(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_PATTERN = Pattern.compile("(https?|file):.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPACE_PATTERN = Pattern.compile("([a-zA-Z0-9]+):");
    private static final Pattern PAGE_PATTERN = Pattern.compile("([^:^\"]+)");
    private static final Pattern PAGE_ON_SPACE_PATTERN = Pattern.compile("([a-zA-Z0-9]+):([^^]+)");
    private static final Pattern ATTACHMENT_PATTERN = Pattern.compile("\\^([^^]+)");
    private static final Pattern ATTACHMENT_ON_SPACE_PAGE_PATTERN = Pattern.compile("([a-zA-Z0-9]+):([^^]+)\\^([^^]+)");
    private static final Pattern ATTACHMENT_ON_PAGE_PATTERN = Pattern.compile("([^^]+)\\^([^^]+)");
    private static final Pattern USER_PATTERN = Pattern.compile("^~(.+)");

    @Inject
    private Logger logger;

    @Override
    public ResourceReference parse(String rawRef)
    {
        Matcher m;
        m = MAILTO_PATTERN.matcher(rawRef);
        if (m.matches()) {
            return getResourceReference(ResourceType.MAILTO, m.group(1));
        }

        m = URL_PATTERN.matcher(rawRef);
        if (m.matches()) {
            return getResourceReference(ResourceType.URL, rawRef);
        }

        m = SPACE_PATTERN.matcher(rawRef);
        if (m.matches()) {
            ConfluenceResourceReference ref = new ConfluenceResourceReference(rawRef, ResourceType.SPACE);
            ref.setTyped(true);
            ref.setSpaceKey(m.group(1));
            return ref;
        }

        m = PAGE_ON_SPACE_PATTERN.matcher(rawRef);
        if (m.matches()) {
            ConfluenceResourceReference ref = new ConfluenceResourceReference(rawRef, ResourceType.DOCUMENT);
            ref.setTyped(true);
            ref.setSpaceKey(m.group(1));
            ref.setPageTitle(m.group(2));
            return ref;
        }

        m = ATTACHMENT_ON_SPACE_PAGE_PATTERN.matcher(rawRef);
        if (m.matches()) {
            ConfluenceResourceReference ref = new ConfluenceResourceReference(rawRef, ResourceType.ATTACHMENT);
            ref.setSpaceKey(m.group(1));
            ref.setPageTitle(m.group(2));
            ref.setFilename(m.group(3));
            ref.setTyped(true);
            return ref;
        }

        m = ATTACHMENT_ON_PAGE_PATTERN.matcher(rawRef);
        if (m.matches()) {
            ConfluenceResourceReference ref = new ConfluenceResourceReference(rawRef, ResourceType.ATTACHMENT);
            ref.setPageTitle(m.group(1));
            ref.setFilename(m.group(2));
            ref.setTyped(true);
            return ref;
        }

        m = ATTACHMENT_PATTERN.matcher(rawRef);
        if (m.matches()) {
            ConfluenceResourceReference ref = new ConfluenceResourceReference(rawRef, ResourceType.ATTACHMENT);
            ref.setTyped(true);
            ref.setFilename(m.group(1));
            return ref;
        }

        m = USER_PATTERN.matcher(rawRef);
        if (m.matches()) {
            return getResourceReference(ResourceType.USER, m.group(1));
        }

        // do this last: anything that does not contain any special chars is normally a page reference
        m = PAGE_PATTERN.matcher(rawRef);
        if (m.matches()) {
            ConfluenceResourceReference ref = new ConfluenceResourceReference(rawRef, ResourceType.DOCUMENT);
            ref.setTyped(true);
            ref.setPageTitle(rawRef);
            return ref;
        }

        logger.warn("Could not parse reference [{}]", rawRef);
        ResourceReference unknownRef = new ResourceReference(rawRef, ResourceType.UNKNOWN);
        unknownRef.setTyped(false);
        return unknownRef;
    }

    private static ResourceReference getResourceReference(ResourceType type, String ref)
    {
        ResourceReference result = new ResourceReference(ref, type);
        result.setTyped(true);
        return result;
    }
}
