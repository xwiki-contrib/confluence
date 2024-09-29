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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.parser.confluence.internal.wikimodel.ConfluenceWikiParser;
import org.xwiki.rendering.internal.parser.wikimodel.AbstractWikiModelParser;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.rendering.wikimodel.IWikiParser;

/**
 * Parser for Confluence syntax.
 *
 * @version $Id$
 * @since 9.0
 */
@Component
@Named(ConfluenceParser.SYNTAX_STRING)
@Singleton
public class ConfluenceParser extends AbstractWikiModelParser implements Cloneable
{
    /**
     * The identifier of the syntax.
     */
    public static final String SYNTAX_STRING = "confluence/1.1";

    /**
     * The syntax object.
     */
    public static final Syntax SYNTAX = new Syntax(SyntaxType.CONFLUENCE, "1.1");

    /**
     * @see #getLinkReferenceParser()
     */
    @Inject
    @Named("default/link")
    private ResourceReferenceParser referenceParser;

    /**
     * @see #getImageReferenceParser()
     */
    @Inject
    @Named("default/image")
    private ResourceReferenceParser imageReferenceParser;

    @Override
    public Syntax getSyntax()
    {
        return SYNTAX;
    }

    @Override
    public IWikiParser createWikiModelParser()
    {
        return new ConfluenceWikiParser();
    }

    @Override
    public ResourceReferenceParser getLinkReferenceParser()
    {
        return this.referenceParser;
    }

    /**
     * Sets the link reference parser to be used for this instance.
     * This should only be called on clones of the component (unless
     * one really wants to change the reference parser globally).
     *
     * @see #clone()
     * @param referenceParser the reference parser to be used by this instance
     */
    public void setLinkReferenceParser(ResourceReferenceParser referenceParser)
    {
        this.referenceParser = referenceParser;
    }

    @Override
    public ResourceReferenceParser getImageReferenceParser()
    {
        return this.imageReferenceParser;
    }

    /**
     * Return a (shallow) copy of this parser.
     * This is needed to change the link reference parser
     * (and possibly the image reference parser) in clones of the singleton
     * component for special cases.
     * @return a shallow copy of this instance
     * @throws CloneNotSupportedException as inherited from base class
     */
    public ConfluenceParser clone() throws CloneNotSupportedException
    {
        return (ConfluenceParser) super.clone();
    }
}
