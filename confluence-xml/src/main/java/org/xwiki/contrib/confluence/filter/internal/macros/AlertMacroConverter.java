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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.listener.Listener;

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

    @Inject
    private Logger logger;

    @Override
    public void toXWiki(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline, Listener listener)
    {
        // Changed the order of the calls to make sure that the type parameter is kept only in the case of an unknown
        // macro.
        String content = toXWikiContent(confluenceId, confluenceParameters, confluenceContent);
        Map<String, String> parameters = toXWikiParameters(confluenceId, confluenceParameters, confluenceContent);
        String macroID = toXWikiId(confluenceId, parameters, confluenceContent, inline);
        listener.onMacro(macroID, parameters, content, inline);
    }

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        String type = confluenceParameters.get(TYPE);
        confluenceParameters.remove(TYPE);
        switch (type) {
            case "Success":
                return "success";
            case "Error":
                return "error";
            case "Info":
                return INFO;
            case "Warning":
                return "warning";
            default:
                logger.warn(String.format("The type of alert is not supported: %s", confluenceParameters.get(TYPE)));
                // We add the type back to be able to identify this information in the future.
                confluenceParameters.put(TYPE, type);
                return INFO;
        }
    }

    @Override
    protected String toXWikiContent(String confluenceId, Map<String, String> parameters, String confluenceContent)
    {
        if (parameters.get(TITLE) != null) {
            StringBuilder builder = new StringBuilder();
            builder.append(parameters.get(TITLE));
            builder.append("\n");
            builder.append(confluenceContent);
            return builder.toString();
        }
        return confluenceContent;
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> parameters = new HashMap<>(confluenceParameters);
        parameters.remove(TITLE);
        return parameters;
    }
}
