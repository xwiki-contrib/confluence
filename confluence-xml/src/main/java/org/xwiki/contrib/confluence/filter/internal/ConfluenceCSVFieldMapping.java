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

import java.util.HashMap;

/**
 * There are several Confluence export formats, and they don't use the same field names.
 * This class is used to map CSV fields to what's used internally in ConfluenceXMLPackage, which closely matches the
 * field names used in the XML export format. It's also used to determine which column is used as a unique identifier.
 * @since 9.96.0
 * @version $Id$
 */
public class ConfluenceCSVFieldMapping extends HashMap<String, String>
{
    private final String csvFieldId;

    ConfluenceCSVFieldMapping(String csvFieldId, String... xmlAndCsvFieldPairs)
    {
        this.csvFieldId = csvFieldId;

        for (int i = 0; i < xmlAndCsvFieldPairs.length; i += 2) {
            put(xmlAndCsvFieldPairs[i], xmlAndCsvFieldPairs[i + 1]);
        }
    }

    /**
     * @return the name of the column that identifies records
     */
    public String getCSVFieldId()
    {
        return csvFieldId;
    }
}
