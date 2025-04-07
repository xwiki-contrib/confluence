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
package org.xwiki.contrib.confluence.filter.internal.macros;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.MacroConverter;
import org.xwiki.contrib.confluence.filter.AbstractMacroConverter;

/**
 * Convert Confluence center macro.
 *
 * @version $Id$
 * @since 9.82.0
 */
@Component
@Singleton
@Named("center")
public class CenterMacroConverter extends AbstractMacroConverter
{
    private static final String CLASS = "class";

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return "center";
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> parameters = new HashMap<>(1);
        parameters.put(CLASS, confluenceParameters.get(CLASS));
        return parameters;
    }

    @Override
    public MacroConverter.InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return MacroConverter.InlineSupport.NO;
    }
}
