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

import java.util.Map;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * Convert Confluence viewport redirect macro to XWiki redirect macro.
 *
 * @version $Id$
 * @since 9.70.0
 */
@Singleton
@Component(hints = { "viewport-redirect", "viewport-url-redirect" })
public class ViewportRedirectMacroConverter extends AbstractMacroConverter
{
    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        markHandledParameter(confluenceParameters, "delay", true);
        markHandledParameter(confluenceParameters, "visible", true);
        return "redirect";
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.NO;
    }

    @Override
    protected String toXWikiParameterName(String confluenceParameterName, String confluenceId,
        Map<String, String> parameters, String confluenceContent)
    {
        if ("redirectTo".equals(confluenceParameterName)) {
            return "reference";
        }
        return confluenceParameterName;
    }
}
