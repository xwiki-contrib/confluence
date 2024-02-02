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

import org.xwiki.stability.Unstable;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Represents a list of Confluence id ranges.
 *
 * A Confluence range list is stateful because it changes the state of its individual ranges as a list of ids to accept
 * or reject is evaluated against it. Important: spaces should not be evaluated with these ranges.
 *
 * Usage: {@snippet :
 *     var ranges = new ConfluenceIdRangeList("[2,9], [10,7]");
 *
 *     for (long objectId : objectIds) {
 *         if (range.pushId(objectId)) {
 *             sendPage(pageId);
 *         }
 *     }
 *  }
 *
 * @since 9.35.0
 * @version $Id$
 */
@Unstable
public class ConfluenceIdRangeList
{
    private static final String COMMA = ",";

    private int currentRange;
    private List<ConfluenceIdRange> ranges;

    /**
     * Construct a confluence object ID range list.
     * @param ranges a string representation of the list
     * @throws SyntaxError if ranges cannot be parsed.
     */
    public ConfluenceIdRangeList(String ranges) throws SyntaxError
    {
        this.ranges = new ArrayList<>();
        this.currentRange = 0;

        try (Scanner s = new Scanner(ranges)) {
            while (s.hasNext()) {
                this.ranges.add(new ConfluenceIdRange(s, false));
                if (s.hasNext(COMMA)) {
                    s.next();
                } else if (s.hasNext()) {
                    throw new SyntaxError("Expected a comma, found '" + s.nextByte() + "'");
                } else {
                    break;
                }
            }
        }
    }

    /**
     * Construct a confluence object ID range list.
     * @param ranges a list of ranges. It is assumed that ranges are in their initial state.
     */
    public ConfluenceIdRangeList(List<ConfluenceIdRange> ranges)
    {
        this.ranges = ranges;
    }

    /**
     * @return the next id that will make this range list change state.
     */
    public Long getNextId()
    {
        if (currentRange >= ranges.size()) {
            return null;
        }

        return ranges.get(currentRange).getNextId();
    }

    /**
     * Update the state with a new id.
     * @param id the id to evaluate
     * @return whether the pushed id is accepted. If the given type isn't considered in this range list, it is accepted.
     */
    public boolean pushId(Long id)
    {
        if (currentRange >= ranges.size()) {
            return false;
        }

        ConfluenceIdRangeState state = ranges.get(currentRange).pushId(id);
        boolean accepted;
        switch (state) {
            case BEFORE:
            case RIGHT_BEFORE:
                accepted = false;
                break;
            case ACCEPTED_END:
                // Make sure the next id goes to the next range, we know it does not belong to the current one.
                currentRange++;
                /* fall through */
            case ACCEPTED:
                accepted = true;
                break;
            case AFTER:
                // try the next range. Note: several ranges can be skipped like this if some are empty.
                currentRange++;
                accepted = pushId(id);
                break;
            default:
                // should not happen. In doubt, let's not skip objects.
                accepted = true;
        }
        return accepted;
    }

    @Override
    public String toString()
    {
        return ranges.stream().map(ConfluenceIdRange::toString).collect(Collectors.joining(", "));
    }
}
