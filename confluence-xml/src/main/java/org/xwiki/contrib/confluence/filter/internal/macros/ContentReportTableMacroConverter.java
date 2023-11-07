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

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverter;
import org.xwiki.model.EntityType;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Convert Confluence content-report-table.
 *
 * @version $Id$
 * @since 9.27.0
 */
@Component
@Singleton
@Named("content-report-table")
public class ContentReportTableMacroConverter extends AbstractMacroConverter
{
    private static final String SPACES = "spaces";
    private static final String COMMA = ",";
    @Inject
    private ConfluenceConverter confluenceConverter;

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> parameters = new LinkedHashMap<>(1);
        parameters.put(SPACES, Arrays.stream(
            confluenceParameters.get(SPACES).split(COMMA)).map(spaceRef ->
                confluenceConverter.convert(spaceRef, EntityType.SPACE)).collect(Collectors.joining(COMMA)));
        return parameters;
    }
}
