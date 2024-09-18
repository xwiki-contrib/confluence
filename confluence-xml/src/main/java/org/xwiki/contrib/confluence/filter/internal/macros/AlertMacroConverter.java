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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
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
    private static final String TYPE = "type";

    @Inject
    private Logger logger;

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        switch (confluenceParameters.get(TYPE)) {
            case "Success":
                return "success";
            case "Error":
                return "error";
            case "Info":
                return "info";
            case "Warning":
                return "warning";
            default:
                logger.warn("The type of alert is not supported");
                return confluenceParameters.get(TYPE);
        }
    }
}
