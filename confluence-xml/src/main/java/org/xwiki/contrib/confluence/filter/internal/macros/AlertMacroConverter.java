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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;

/**
 * Convert the alert macro.
 *
 * @version $Id$
 * @since 9.53.0
 */
@Singleton
@Component
@Named("alert")
public class AlertMacroConverter extends AbstractMacroConverter
{
    private static final String INFO = "info";

    private static final String TITLE = "title";

    private static final String TYPE = "type";

    private static final List<String> KNOWN_TYPES = List.of("Success", "Info", "Error", "Warning");

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        String type = confluenceParameters.getOrDefault(TYPE, "");
        if (KNOWN_TYPES.contains(type)) {
            return type.toLowerCase();
        }

        markUnhandledParameterValue(confluenceParameters, TYPE);
        return INFO;
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.NO;
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        String title = confluenceParameters.get(TITLE);
        if (StringUtils.isEmpty(title)) {
            return Collections.emptyMap();
        }
        return Map.of(TITLE, title);
    }
}
