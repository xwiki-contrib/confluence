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
package org.xwiki.contrib.confluence.filter.input;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceLinkMappingReceiver;
import org.xwiki.model.reference.EntityReference;

import java.util.Map;

/**
 * Generate a link mapping from a Confluence package.
 * @since 9.40.0
 * @version $Id$
 */
@Role
public interface LinkMapper
{
    /**
     * @return the link mapping. Keys are space keys, values are maps mapping document titles to document references.
     * Since version 9.42.0, entries with a "[spacekey]:ids" key contains a mapping from page IDs (as string) to
     * document references.
     */
    Map<String, Map<String, EntityReference>> getLinkMapping();

    /**
     * Fill the given object with the link mapping.
     * @param mapper the object to fill
     */
    default void getLinkMapping(ConfluenceLinkMappingReceiver mapper)
    {
        for (Map.Entry<String, Map<String, EntityReference>> entry : getLinkMapping().entrySet()) {
            String spaceKey = entry.getKey();
            boolean isPageIds = spaceKey.endsWith(":ids");
            if (isPageIds) {
                spaceKey = spaceKey.substring(0, spaceKey.length() - 4);
            }

            for (Map.Entry<String, EntityReference> mapping: entry.getValue().entrySet()) {
                if (isPageIds) {
                    mapper.addPage(spaceKey, Long.parseLong(mapping.getKey()), mapping.getValue());
                } else {
                    mapper.addPage(spaceKey, mapping.getKey(), mapping.getValue());
                }
            }
        }
    }
}
