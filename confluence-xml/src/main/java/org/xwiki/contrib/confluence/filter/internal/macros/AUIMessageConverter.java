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

/**
 * Converts auimessage to either info, success, warning or error macros.
 * https://aui.atlassian.com/aui/7.9/docs/messages.html
 *
 * @version $Id$
 * @since 9.51.1
 */
@Component
@Named("auimessage")
@Singleton
public class AUIMessageConverter extends AbstractMacroConverter
{
    private static final String PARAM_KEY_TITLE = "title";

    private static final String PARAM_KEY_CLASS = "class";

    private static final String PARAM_KEY_TYPE = "type";

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        switch (confluenceParameters.getOrDefault(PARAM_KEY_TYPE, "")) {
            case "":
            case "generic":
            case "hint":
                return "info";
            default:
                return confluenceParameters.get(PARAM_KEY_TYPE);
        }
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> newParams = new HashMap<>();

        if (confluenceParameters.containsKey(PARAM_KEY_TITLE)) {
            newParams.put(PARAM_KEY_TITLE, confluenceParameters.get(PARAM_KEY_TITLE));
        }

        if (confluenceParameters.containsKey(PARAM_KEY_CLASS)) {
            newParams.put("cssClass", confluenceParameters.get(PARAM_KEY_CLASS));
        }

        return newParams;
    }
}
