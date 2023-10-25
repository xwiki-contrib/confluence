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
package org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel;

import org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.MacroTagHandler.ConfluenceMacro;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Base class for various kind of macro parameters.
 * 
 * @version $Id$
 * @since 9.1
 */
public abstract class AbstractMacroParameterTagHandler extends AbstractConfluenceTagHandler
    implements ConfluenceTagHandler
{
    protected static final String IN_CONFLUENCE_PARAMETER = "in-confluence-parameter";

    /**
     * Default constructor.
     */
    public AbstractMacroParameterTagHandler()
    {
        super(true);
    }

    @Override
    protected void begin(TagContext context)
    {
        setAccumulateContent(true);
        context.getTagStack().pushStackParameter(IN_CONFLUENCE_PARAMETER, true);
    }

    @Override
    protected void end(TagContext context)
    {
        context.getTagStack().popStackParameter(IN_CONFLUENCE_PARAMETER);
        ConfluenceMacro macro = (ConfluenceMacro) context.getTagStack().getStackParameter(CONFLUENCE_CONTAINER);

        if (macro != null) {
            setParameter(macro, context);
        }
    }

    protected abstract void setParameter(ConfluenceMacro macro, TagContext context);
}
