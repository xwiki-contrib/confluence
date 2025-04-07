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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Collections;
import java.util.Map;

import org.xwiki.contrib.confluence.filter.AbstractMacroConverter;

/**
 * Convert Confluence warning macro.
 *
 * @version $Id$
 * @since 9.24.0
 */
@Component
@Singleton
@Named("warning")
public class WarningMacroConverter extends AbstractMacroConverter
{
    private static final String TITLE = "title";

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return "error";
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        String title = confluenceParameters.get(TITLE);
        if (StringUtils.isEmpty(title)) {
            return Collections.emptyMap();
        }
        return Collections.singletonMap(TITLE, title);
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.NO;
    }
}
