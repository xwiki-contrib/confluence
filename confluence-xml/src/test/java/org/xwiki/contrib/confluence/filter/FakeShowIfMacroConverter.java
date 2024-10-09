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
package org.xwiki.contrib.confluence.filter;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverter;
import org.xwiki.contrib.confluence.filter.internal.macros.AbstractMacroConverter;

@Component
@Singleton
@Named("show-if")
public class FakeShowIfMacroConverter extends AbstractMacroConverter
{
    private static final String GROUP_ID_PARAM = "groupIds";

    @Inject
    private ConfluenceConverter converter;

    @Override
    protected String toXWikiParameterValue(String confluenceParameterName, String confluenceParameterValue,
        String confluenceId, Map<String, String> parameters, String confluenceContent)
    {
        if (confluenceParameterName.equals(GROUP_ID_PARAM)) {
            String groupRef = converter.convertGroupId(confluenceParameterValue);
            return groupRef == null ? confluenceParameterValue : groupRef;
        }
        return super.toXWikiParameterValue(confluenceParameterName, confluenceParameterValue, confluenceId, parameters,
            confluenceContent);
    }
}
