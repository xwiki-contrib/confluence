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

import java.io.Reader;

import org.xwiki.contrib.confluence.parser.confluence.internal.wikimodel.javacc.ConfluenceWikiScanner;
import org.xwiki.contrib.confluence.parser.confluence.internal.wikimodel.javacc.ParseException;
import org.xwiki.rendering.wikimodel.IWemListener;
import org.xwiki.rendering.wikimodel.IWikiParser;
import org.xwiki.rendering.wikimodel.WikiParserException;

/**
 * Parse the Confluence wiki syntax.
 * <pre>
 * http://confluence.atlassian.com/renderer/notationhelp.action?section=all
 * </pre>
 *
 * @since 9.0
 * @version $Id$
 */
public class ConfluenceWikiParser implements IWikiParser
{
    /**
     * Indicate if {noformat} macro should be seen as a macro or a verbatim block.
     */
    private boolean fNoformatAsMacro = true;

    /**
     * Default constructor.
     */
    public ConfluenceWikiParser()
    {
    }

    /**
     * Construct a ConfluenceWikiParser, specifying how to handle the {noformat} Confluence macro.
     * @param noformatAsMacro whether the {noformat} macro should be seen as a macro or a verbatim block
     * @see ConfluenceWikiScanner
     */
    public ConfluenceWikiParser(boolean noformatAsMacro)
    {
        fNoformatAsMacro = noformatAsMacro;
    }

    /**
     * Parse.
     * @param reader the reader from which to parse.
     * @param listener the listener to use.
     * @see org.xwiki.rendering.wikimodel.IWikiParser#parse(java.io.Reader,
     *      org.xwiki.rendering.wikimodel.IWemListener)
     */
    public void parse(Reader reader, IWemListener listener) throws WikiParserException
    {
        try {
            ConfluenceWikiScanner scanner = new ConfluenceWikiScanner(reader);
            scanner.setNoformatAsMacro(fNoformatAsMacro);
            ConfluenceWikiScannerContext context = new ConfluenceWikiScannerContext(listener);
            scanner.parse(context);
        } catch (ParseException e) {
            throw new WikiParserException(e);
        }
    }
}
