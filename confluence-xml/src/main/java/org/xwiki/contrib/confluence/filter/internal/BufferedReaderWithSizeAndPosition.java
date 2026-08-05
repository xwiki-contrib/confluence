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

import org.apache.commons.io.input.BoundedInputStream;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.Reader;

/**
 * A buffered reader that tracks the current position and length from the source input stream, so we can estimate
 * progress while reading something.
 * The provided reader must be reading from the provided input stream, but this can be indirect. For instance,
 * the reader can be providing a stream that's the result of some gunzipping operation performed by some intermediary
 * stream reader. bis is usually file backed.
 * We are not directly estimating the progress from the reader because we don't necessarily know its size in advance,
 * but we definitely know the size of the backing file.
 * @since 9.96.0
 * @version $Id$
 */
public class BufferedReaderWithSizeAndPosition extends BufferedReader implements Closeable
{
    private final BoundedInputStream bis;

    BufferedReaderWithSizeAndPosition(Reader reader, BoundedInputStream bis)
    {
        super(reader);
        this.bis = bis;
    }

    long position()
    {
        return bis.getCount();
    }

    long length()
    {
        return bis.getMaxLength();
    }
}
