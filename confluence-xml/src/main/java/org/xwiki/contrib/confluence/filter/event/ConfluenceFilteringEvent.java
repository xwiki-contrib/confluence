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
package org.xwiki.contrib.confluence.filter.event;

import org.xwiki.observation.event.AbstractCancelableEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Event emitted after the confluence package has been read and the filtering process starts.
 *
 * <ul>
 * <li>source: the input filter stream that triggers the event.</li>
 * <li>data: the confluence package that has been read.</li>
 * </ul>
 *
 * @version $Id$
 * @since 9.21.0
 */
public class ConfluenceFilteringEvent extends AbstractCancelableEvent
{
    private Collection<Long> disabledSpaces;

    private Map<String, String> renamedSpaces;

    /**
     * Don't import the given space.
     * @param spaceId the space to disable
     * @since 9.35.0
     */
    public void disableSpace(long spaceId)
    {
        if (disabledSpaces == null) {
            disabledSpaces = new HashSet<>();
        }
        disabledSpaces.add(spaceId);
    }

    /**
     * Rename an imported space.
     * @param spaceKey the space to rename
     * @param newSpaceKey the new space name
     * @since 9.88.5
     */
    public void renameSpace(String spaceKey, String newSpaceKey)
    {
        if (renamedSpaces == null) {
            renamedSpaces = new HashMap<>();
        }
        renamedSpaces.put(spaceKey, newSpaceKey);
    }

    /**
     * @return the disabled spaces
     * @since 9.35.0
     */
    public Collection<Long> getDisabledSpaces()
    {
        return disabledSpaces == null ? Collections.emptyList() : disabledSpaces;
    }

    /**
     * @return the renamed spaces
     * @since 9.88.5
     */
    public Map<String, String> getRenamedSpaces()
    {
        return renamedSpaces == null ? new HashMap<>() : renamedSpaces;
    }
}
