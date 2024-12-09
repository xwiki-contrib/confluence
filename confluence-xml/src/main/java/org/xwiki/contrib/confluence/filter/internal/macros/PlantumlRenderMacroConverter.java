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
 * Convert Confluence plantumlrender macros.
 *
 * @version $Id$
 * @since 9.65
 */
@Component
@Singleton
@Named(PlantumlRenderMacroConverter.PLANTUML_RENDER)
public class PlantumlRenderMacroConverter extends AbstractMacroConverter
{
    /**
     * The ID of the XWiki plantuml macro.
     */
    public static final String PLANTUML = "plantuml";

    /**
     * The ID of the Confluence plantuml render macro.
     */
    public static final String PLANTUML_RENDER = PLANTUML + "render";

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return PLANTUML;
    }
}
