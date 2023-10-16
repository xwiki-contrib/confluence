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

package org.xwiki.contrib.confluence.filter.internal;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reader removing the ASCII BS (8) character from the provided input stream.
 * This is necessary because we have seen Confluence exports containing such characters which break XML parsing.
 * @version $Id$
 * @since 9.24.0
 */
public class WithoutBackSpacesReader extends InputStream
{
    private InputStream is;

    /**
     * Convert this input stream into a reader that skips the BS characters.
     * @param is the input stream from which to ignore the BS characters.
     */
    public WithoutBackSpacesReader(InputStream is)
    {
        this.is = is;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        for (int i = 0; i < len; i++) {
            int c = read();
            if (c == -1) {
                if (i == 0) {
                    return -1;
                }
                return i;
            }
            b[off + i] = (byte) c;
        }
        return len;
    }

    @Override
    public int read() throws IOException
    {
        int c1 = is.read();
        if (c1 == 8) {
            // Ignore the backspace character
            return read();
        }
        return c1;
    }

    @Override
    public void close() throws IOException
    {
        is.close();
    }
}
