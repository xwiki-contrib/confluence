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

import org.xwiki.contrib.confluence.filter.input.ConfluenceProperties;
import org.xwiki.filter.FilterException;

/**
 * Represents a Confluence record, which has a unique identifier, properties and values.
 * This interface helps abstract the fact that there are different Confluence export formats.
 * An implementation can represent a whole file instead of a unique record, in which case readObjectProperties read the
 * next record.
 * @since 9.96.0
 * @version $Id$
 */
public interface ConfluenceRecordReader
{
    /**
     * Reads the record. If the implementation backs a whole file, read the next record.
     * @return the unique identifier of the record being read
     * @param properties the object receiving the values of the record being read.
     * @throws FilterException if something wrong happens
     */
    Object readRecord(ConfluenceProperties properties) throws FilterException;

    /**
     * @return the type of the record
     */
    String getType();
}
