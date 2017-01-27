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
package org.xwiki.contrib.confluence.parser.confluence.internal;

import java.io.Reader;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Override of the 1.0 MediaWiki syntax parser using the 1.6 one.
 *
 * @version $Id: fcd59f6c7ae81ffec64f5df3ca333eca4eaf18b3 $
 * @since 9.0
 */
@Component
@Named(Confluence10OverrideStreamParser.CONFLUENCE_1_0_STRING)
@Singleton
public class Confluence10OverrideStreamParser implements StreamParser
{
    /**
     * The String version of the syntax.
     */
    public static final String CONFLUENCE_1_0_STRING = "confluence/1.0";

    @Inject
    @Named(ConfluenceParser.SYNTAX_STRING)
    private StreamParser parser;

    @Override
    public Syntax getSyntax()
    {
        return Syntax.CONFLUENCE_1_0;
    }

    @Override
    public void parse(Reader source, Listener listener) throws ParseException
    {
        this.parser.parse(source, listener);
    }
}
