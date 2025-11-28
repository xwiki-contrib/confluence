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
import org.xwiki.contrib.confluence.filter.AbstractMacroConverter;
import org.xwiki.contrib.confluence.filter.ConversionException;

/**
 * Converts auimessage to either info, success, warning or error macros.
 * <a href="https://aui.atlassian.com/aui/7.9/docs/messages.html">documentation of the macro</a>
 *
 * @version $Id$
 * @since 9.51.1
 */
@Component
@Named("auimessage")
@Singleton
public class AUIMessageConverter extends AbstractMacroConverter
{
    private static final String INFO = "info";
    private static final String CONFLUENCE_AUIMESSAGE = "confluence_auimessage";
    private static final String TYPE = "type";

    @Inject
    private Logger logger;

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        String type = confluenceParameters.getOrDefault(TYPE, "");
        switch (type) {
            case "error":
            case "warning":
            case "success":
                return type;
            case "":
            case "generic":
            case "hint":
            case INFO:
                return INFO;
            default:
                return CONFLUENCE_AUIMESSAGE;
        }
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.YES;
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content) throws ConversionException
    {
        if (CONFLUENCE_AUIMESSAGE.equals(toXWikiId(confluenceId, confluenceParameters, content, false))) {
            if (logger.isErrorEnabled()) {
                logger.error("Unhandled type [{}] for macro auimessage", confluenceParameters.get(TYPE));
            }
            throw new ConversionException("Unhandled type for macro auimessage");
        }
        Map<String, String> xwikiParameters = new HashMap<>();

        saveParameter(confluenceParameters, xwikiParameters, "title", false);
        saveParameter(confluenceParameters, xwikiParameters, "class", "cssClass", false);

        return xwikiParameters;
    }
}
