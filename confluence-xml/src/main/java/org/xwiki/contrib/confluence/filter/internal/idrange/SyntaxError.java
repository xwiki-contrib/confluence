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
 * A syntax error while parsing a Confluence id range.
 *
 * @version $Id$
 * @since 9.35.0
 */
public class SyntaxError extends Exception
{
    private final int position;

    private final String message;

    /**
     * Construct a syntax error.
     *
     * @param message  the reason of the error.
     * @param position the character position at which the error occurred.
     */
    public SyntaxError(String message, int position)
    {
        super(message + " at " + position);
        this.message = message;
        this.position = position;
    }

    /**
     * Construct a syntax error at an undefined position.
     *
     * @param message the reason of the error.
     */
    public SyntaxError(String message)
    {
        this(message, -1);
    }

    /**
     * @return the position at which the syntax error happened.
     * NOTE: the reported error can be 1 character after the actual error.
     */
    public int getPosition()
    {
        return position;
    }

    @Override
    public String getMessage()
    {
        return message;
    }
}
