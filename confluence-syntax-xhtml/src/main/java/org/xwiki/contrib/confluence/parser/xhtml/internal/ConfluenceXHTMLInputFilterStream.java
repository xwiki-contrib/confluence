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

import java.io.IOException;
import java.io.Reader;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceXHTMLInputProperties;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.AbstractBeanInputFilterStream;
import org.xwiki.filter.input.ReaderInputSource;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;

/**
 * @version $Id: 41df1dab66b03111214dbec56fee8dbd44747638 $
 * @since 9.3
 */
@Component
@Named(ConfluenceXHTMLInputProperties.FILTER_STREAM_TYPE_STRING)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ConfluenceXHTMLInputFilterStream
    extends AbstractBeanInputFilterStream<ConfluenceXHTMLInputProperties, Listener>
{
    @Inject
    @Named(ConfluenceXHTMLParser.SYNTAX_STRING)
    private StreamParser confluenceXHTMLParser;

    @Inject
    private RenderingContext renderingContext;

    @Override
    public void close() throws IOException
    {
        this.properties.getSource().close();
    }

    private Reader getSource() throws FilterException
    {
        if (this.properties.getSource() instanceof ReaderInputSource) {
            return ((ReaderInputSource) this.properties.getSource()).getReader();
        } else {
            throw new FilterException("Unknown source type [" + this.properties.getSource().getClass() + "]");
        }
    }

    private Syntax getSyntax()
    {
        Syntax targetSyntax = this.properties.getMacroContentSyntax();

        // If the syntax is not provided try the rendering syntax
        if (targetSyntax == null) {
            targetSyntax = this.renderingContext.getTargetSyntax();
        }

        return targetSyntax;
    }

    @Override
    protected void read(Object filter, Listener proxyFilter) throws FilterException
    {
        Listener listener = (Listener) filter;

        if (this.confluenceXHTMLParser instanceof ConfluenceXHTMLParser) {
            Syntax targetSyntax = getSyntax();

            if (targetSyntax != null) {
                try {
                    ((ConfluenceXHTMLParser) this.confluenceXHTMLParser).setMacroContentSyntax(targetSyntax);
                } catch (ComponentLookupException e) {
                    throw new FilterException("Failed to initialize the Confluence XHTML input filter", e);
                }
            } else {
                this.renderingContext.getTargetSyntax();
            }
        }

        try {
            this.confluenceXHTMLParser.parse(getSource(), listener);
        } catch (ParseException e) {
            throw new FilterException("Failed to parse Confluence XHTML content", e);
        }
    }
}
