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

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceURLConverter;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

/**
 * Convert Confluence img macro to XWiki syntax.
 *
 * @version $Id$
 * @since 9.56.0
 */
@Singleton
@Component
@Named("img")
public class ImgMacroConverter extends AbstractMacroConverter
{
    private static final String SRC = "src";

    @Inject
    private ConfluenceURLConverter urlConverter;

    @Override
    public void toXWiki(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline, Listener listener)
    {
        String url = confluenceParameters.get(SRC);
        ResourceReference reference = urlConverter.convertURL(url);
        if (reference == null) {
            reference = new ResourceReference(url, ResourceType.URL);
        }
        Map<String, String> parameters = new HashMap<>(confluenceParameters);
        parameters.remove(SRC);
        listener.onImage(reference, false, parameters);
    }
}
