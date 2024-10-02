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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.parser.confluence.internal.ConfluenceParser;
import org.xwiki.rendering.parser.ResourceReferenceParser;

/**
 * Parser for Confluence syntax and references.
 *
 * Similar to the {@link ConfluenceParser} but also resolves references the Confluence way.
 *
 * @version $Id$
 * @since 9.54.0
 */
@Component
@Named(ConfluenceInputStreamParser.COMPONENT_NAME)
@Singleton
public class ConfluenceInputStreamParser extends ConfluenceParser
{
    /**
     * The name of the component.
     */
    public static final String COMPONENT_NAME = "confluence/1.1+xml";

    /**
     * @see #getLinkReferenceParser()
     */
    @Inject
    @Named("confluence/link")
    private ResourceReferenceParser referenceParser;

    @Override
    public ResourceReferenceParser getLinkReferenceParser()
    {
        return this.referenceParser;
    }
}
