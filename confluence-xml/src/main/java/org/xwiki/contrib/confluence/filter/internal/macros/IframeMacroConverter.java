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
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.listener.Listener;

/**
 * Convert the iframe macro.
 *
 * @version $Id$
 * @since 9.51.1
 */
@Component
@Singleton
@Named("iframe")
public class IframeMacroConverter extends AbstractMacroConverter
{

    private static final String HTML = "html";

    private static final String SEPARATOR = "\"";

    @Override
    public void toXWiki(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline, Listener listener)
    {
        String iframe = this.createIframe(confluenceParameters);
        listener.beginMacroMarker(HTML, Collections.emptyMap(), iframe, false);
        listener.endMacroMarker(HTML, Collections.emptyMap(), iframe, false);
    }

    private String createIframe(Map<String, String> confluenceParameter)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("<iframe ");
        for (Map.Entry<String, String> entry : confluenceParameter.entrySet()) {
            builder.append(entry.getKey());
            builder.append("=");
            builder.append(SEPARATOR);
            builder.append(entry.getValue());
            builder.append(SEPARATOR);
            builder.append(" ");
        }
        builder.append("></iframe>");
        return builder.toString();
    }
}
