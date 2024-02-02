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
package org.xwiki.contrib.confluence.filter.internal.idrange;

/**
 * Confluence ID range state.
 *
 * @version $Id$
 * @since 9.35.0
 */
public enum ConfluenceIdRangeState
{
    /**
     * No id has been accepted yet by this range.
     */
    BEFORE,

    /**
     * The id equals to the exclusive left bound has been seen. If the next id is before the right bound, it will be
     * accepted. Or if it is equal to the inclusive right bound.
     */
    RIGHT_BEFORE,

    /**
     * Last seen id has been accepted.
     */
    ACCEPTED,

    /**
     * Last seen id has been accepted, and it will be the last to be ever accepted by this range because it was
     * equal to the inclusive right bound. A range should not actually ever be in this state, but its pushId() method
     * can return it.
     */
    ACCEPTED_END,

    /**
     * No ids will ever be accepted anymore by this range.
     */
    AFTER
}
