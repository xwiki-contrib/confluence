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

import com.github.luben.zstd.ZstdInputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Read the bodycontent.csv.gz file, abstracting away the fact that the body field might be compressed with zstd and
 * then base64-encoded (which has been seen in site exports and not space exports).
 * @since 9.96.0
 * @version $Id$
 */
class BodyContentCSVFile extends ConfluenceCSVFile
{
    private static final Class ZSTDJNI = com.github.luben.zstd.ZstdInputStream.class;
    /**
     * @param backupFolder    the root of the backup directory in which to find the file
     * @param filename        the filename
     */
    BodyContentCSVFile(File backupFolder, String filename)
    {
        super(backupFolder, filename);
    }

    @Override
    public String get(String field) throws IOException
    {
        String v = super.get(field);
        // 'KLUv/' looks like the beginning of a zstd content that's encoded in base64
        return (v != null && "body".equals(field) && v.startsWith("KLUv/"))
            ? decodeBodyContent(v)
            : v;
    }

    private static String decodeBodyContent(String v) throws IOException
    {
        byte[] decoded = Base64.getDecoder().decode(v.getBytes(StandardCharsets.UTF_8));
        ZstdInputStream stream = new ZstdInputStream(new ByteArrayInputStream(decoded));
        byte[] bytes = stream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
