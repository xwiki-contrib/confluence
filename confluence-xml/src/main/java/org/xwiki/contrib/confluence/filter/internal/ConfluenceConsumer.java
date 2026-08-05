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

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceCanceledException;
import org.xwiki.filter.FilterException;

/**
 * Represents a function that reads a Confluence record (regardless the export format it's coming from; fields name
 * are normalized).
 * @since 9.96.0
 * @version $Id$
 */
@FunctionalInterface
public interface ConfluenceConsumer
{
    /**
     * @param r the record to consume
     * @throws ConfigurationException if something wrong happens
     * @throws FilterException if something wrongs happens
     * @throws ConfluenceCanceledException if something wrong happens
     */
    void accept(ConfluenceRecordReader r) throws ConfigurationException, FilterException, ConfluenceCanceledException;
}
