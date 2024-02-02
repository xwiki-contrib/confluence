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

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Represents a Confluence id range. It describes a sequence of Confluence objects in the order they are parsed.
 * It is possible to include or exclude edges. Note that the usual number ordering is irrelevant, it doesn't matter that
 * the first number of the range is "bigger" than the second one since an object of "bigger" id can be parsed before an
 * object of "lower" id.
 *
 * Examples of string representations:
 *  - [2, 9] - from object id 2 to object id 9, both included
 *  - (8, 2] - from object id 8 excluded to object id 2 included
 *  - [8, 2) - from object id 8 included to object id 2 excluded
 *  - (8, 2) - objects between object id 8 and object id 2, both excluded
 *  - [2,] - object id 2 included and all the following objects
 *  - (4,] - all the objects following object id 4 excluded
 *  - [,6] - all the following objects until object 6 included
 *  - [,6) - all the following objects until object 6 excluded
 *  - [42] - object id 42
 *  - [] - everything.
 *  - [,] - everything.
 *  - (] - everything.
 *  - (,] - everything.
 *  - () - empty range.
 *  - (,) - empty range.
 *  - [) - empty range.
 *  - [,) - empty range.
 *  - (42] - empty range.
 *  - [42) - empty range.
 *
 * A Confluence range is stateful: it possibly "rejects" ids, then accepts some, and then rejects them again, forever.
 *
 * Usage:
 *  var range = new ConfluenceIdRange("[2, 9]")
 *  for (long id : ids) {
 *      var state = range.pushId(id)
 *      if (state == ACCEPTED || state == ACCEPTED_END) {
 *          // Accepted.
 *          // ACCEPTED_END means it's the last id that will ever be accepted. ACCEPTED_END is not guaranteed to be
 *          // returned, and is currently returned when the right bound is inclusive.
 *          // The distinction is useful when deciding whether an id is to be accepted using a list of ranges
 *          // @see ConfluenceIdRangeList
 *      }
 *  }
 *
 * @since 9.35.0
 * @version $Id$
 */
@Unstable
public final class ConfluenceIdRange
{
    private static final String UNEXPECTED_END = "Unexpected end";

    private static final Pattern WHITESPACE = Pattern.compile("\\s", Pattern.UNICODE_CHARACTER_CLASS);

    private ConfluenceIdRangeState state;

    private final Long fromId;

    private final boolean fromIncluded;

    private final Long toId;

    private final boolean toIncluded;

    /**
     * Construct a Confluence ID range.
     * @param fromId the left bound
     * @param fromIncluded whether the left bound is included
     * @param toId the right bound
     * @param toIncluded whether the right bound is included
     */
    public ConfluenceIdRange(Long fromId, boolean fromIncluded, Long toId, boolean toIncluded)
    {
        this.fromId = fromId;
        this.fromIncluded = fromIncluded;
        this.toId = toId;
        this.toIncluded = toIncluded;
        initState();
    }

    /**
     * Construct a Confluence Id range from a Scanner. This is useful when reading a list of ranges.
     * @param s a Scanner from which a confluence Id range will be read from. The Scanner will be left after the range
     *          that as been read, and after the following whitespace characters, if any.
     * @param requireEnd whether to require that the string is fully read after parsing
     * @throws SyntaxError if a range could not be read, or if requireEnd is true and characters remain after parsing.
     */
    public ConfluenceIdRange(Scanner s, boolean requireEnd) throws SyntaxError
    {
        s.useDelimiter("");
        ensureNotTheEnd(s);

        this.fromIncluded = isFromIncluded(s);
        this.fromId = getId(s);

        ensureNotTheEnd(s);

        Long id = null;
        char c = nextChar(s);
        if (c == ',') {
            id = getId(s);
            skipWhitespace(s);
            c = nextChar(s);
        } else {
            id = fromId;
        }
        this.toId = id;

        this.toIncluded = isToIncluded(s, c);
        skipWhitespace(s);
        if (requireEnd && s.hasNext()) {
            throw new SyntaxError("Expected end");
        }
        initState();
    }

    /**
     * Construct a Confluence Id range from a String.
     * @param s a string representing a Confluence Id range will be read from.
     * @throws SyntaxError if a range could not be read or if the string is not fully read (if there is garbage after)
     */
    public ConfluenceIdRange(String s) throws SyntaxError
    {
        this(new Scanner(s), true);
    }

    private void initState()
    {
        if (this.fromId == null) {
            if (this.toId == null) {
                if (this.toIncluded) {
                    this.state = ConfluenceIdRangeState.ACCEPTED;
                } else {
                    this.state = ConfluenceIdRangeState.AFTER;
                }
            } else {
                this.state = ConfluenceIdRangeState.ACCEPTED;
            }
        } else {
            this.state = ConfluenceIdRangeState.BEFORE;
        }
    }

    private static Long getId(Scanner s)
    {
        Long id = null;
        skipWhitespace(s);
        if (!s.hasNext("[0-9]")) {
            return id;
        }
        s.useDelimiter("[,\\])\\s]");
        if (s.hasNextLong()) {
            id = s.nextLong();
        }
        s.useDelimiter("");
        return id;
    }

    private static void skipWhitespace(Scanner s)
    {
        try {
            s.skip(WHITESPACE);
        } catch (NoSuchElementException e) {
            // ignore
        }
    }

    private static void ensureNotTheEnd(Scanner s) throws SyntaxError
    {
        skipWhitespace(s);
        if (!s.hasNext()) {
            throw new SyntaxError(UNEXPECTED_END);
        }
    }

    private static boolean isFromIncluded(Scanner s) throws SyntaxError
    {
        boolean localFromIncluded = false;
        char c = nextChar(s);
        switch (c) {
            case '[':
                localFromIncluded = true;
                break;
            case '(':
                localFromIncluded = false;
                break;
            default:
                unexpected(c, s);
        }
        return localFromIncluded;
    }

    private static char nextChar(Scanner s)
    {
        return s.next().charAt(0);
    }

    private static boolean isToIncluded(Scanner s, char c) throws SyntaxError
    {
        boolean localToIncluded = false;
        switch (c) {
            case ']':
                localToIncluded = true;
                break;
            case ')':
                localToIncluded = false;
                break;
            default:
                unexpected(c, s);
        }
        return localToIncluded;
    }

    private static void unexpected(char c, Scanner s) throws SyntaxError
    {
        throw new SyntaxError("Unexpected character '" + c + "'", s.match().start());
    }

    /**
     * Update the state with a new id.
     * @param id the id to evaluate
     * @return a state corresponding to this id.
     * @see ConfluenceIdRangeState
     */
    public ConfluenceIdRangeState pushId(Long id)
    {
        switch (state) {
            case BEFORE:
                if (Objects.equals(id, fromId)) {
                    state = (fromId == null || fromIncluded)
                        ? ConfluenceIdRangeState.ACCEPTED
                        : ConfluenceIdRangeState.RIGHT_BEFORE;
                }
                break;
            case RIGHT_BEFORE:
                state = ConfluenceIdRangeState.ACCEPTED;
                /* fall through */
            case ACCEPTED:
                if (Objects.equals(id, toId)) {
                    state = ConfluenceIdRangeState.AFTER;
                    if (toIncluded) {
                        return ConfluenceIdRangeState.ACCEPTED_END;
                    }
                }
                /* fall through */
            case AFTER:
                // nothing
            case ACCEPTED_END:
                // should not happen
            default:
                // should not happen
        }
        return state;
    }

    /**
     * @return the id "expected" by this range (the id that will make this range leave the before state).
     */
    public Long getNextId()
    {
        if (ConfluenceIdRangeState.BEFORE.equals(state)) {
            return fromId;
        }

        return null;
    }

    @Override
    public String toString()
    {
        return (fromIncluded ? '[' : '(')
            +  (fromId == null ? "" : fromId.toString())
            +  (Objects.equals(fromId, toId) ? "" : ("," + (toId == null ? "" : toId.toString())))
            +  (toIncluded ? ']' : ')');
    }
}
