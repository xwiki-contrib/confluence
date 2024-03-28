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

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.MacroConverter;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceMacroSupport;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Confluence XML implementation of ConfluenceMacroSupport.
 * @since 9.43.0
 * @version $Id$
 */
@Component (roles = ConfluenceXMLMacroSupport.class)
@Singleton
public class ConfluenceXMLMacroSupport implements ConfluenceMacroSupport
{
    @Inject
    private MacroConverter macroConverter;

    @Override
    public boolean supportsInlineMode(String macroId, Map<String, String> parameters, String content)
    {
        return !MacroConverter.InlineSupport.NO.equals(macroConverter.supportsInlineMode(macroId, parameters, content));
    }
}
