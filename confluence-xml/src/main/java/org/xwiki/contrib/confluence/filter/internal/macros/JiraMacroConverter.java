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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * Convert Confluence jira macro.
 * 
 * @version $Id$
 * @since 9.5.2
 */
@Component
@Singleton
@Named("jira")
public class JiraMacroConverter extends AbstractMacroConverter
{
    @Override
    protected String toXWikiParameterName(String confluenceParameterName, String id,
        Map<String, String> confluenceParameters, String confluenceContent)
    {
        if (confluenceParameterName.equals("server")) {
            return "id";
        }

        return super.toXWikiParameterName(confluenceParameterName, id, confluenceParameters, confluenceContent);
    }
    
    @Override
    protected String toXWikiContent(String confluenceId, Map<String, String> parameters, String confluenceContent)
    {
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (entry.getKey().equals("key")) {
                // return key value as content
                return entry.getValue();
            }
        }
        
        // return (empty) confluence content by default
        return confluenceContent;
    }
}
