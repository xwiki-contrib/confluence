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
package org.xwiki.contrib.confluence.parser.xhtml.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceXHTMLInputProperties;
import org.xwiki.filter.input.AbstractBeanInputFilterStreamFactory;
import org.xwiki.rendering.listener.Listener;

/**
 * Create MediaWiki Syntax format input filters.
 * 
 * @version $Id: 20ffe2c38296482590f986268a1b87f547beebae $
 * @since 9.3
 */
@Component
@Named(ConfluenceXHTMLInputProperties.FILTER_STREAM_TYPE_STRING)
@Singleton
public class ConfluenceXHTMLInputFilterStreamFactory
    extends AbstractBeanInputFilterStreamFactory<ConfluenceXHTMLInputProperties, Listener>
{
    /**
     * The default constructor.
     */
    public ConfluenceXHTMLInputFilterStreamFactory()
    {
        super(ConfluenceXHTMLInputProperties.FILTER_STREAM_TYPE);

        setName("Confluence XHTML Syntax input stream");
        setDescription("Generates rendering events from Confluence XHTML Syntax content.");
    }
}
