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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.contrib.confluence.filter.Mapping;
import org.xwiki.properties.converter.Converter;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Validate {@link MappingConverter}.
 * 
 * @version $Id$
 */
public class MappingConverterTest
{
    @Rule
    public MockitoComponentMockingRule<Converter<Mapping>> mocker =
        new MockitoComponentMockingRule<>(MappingConverter.class);

    @Test
    public void testConvertFromString() throws ComponentLookupException
    {
        assertNull(this.mocker.getComponentUnderTest().convert(Mapping.class, null));
        assertEquals(new Mapping(), this.mocker.getComponentUnderTest().convert(Mapping.class, ""));

        Mapping mapping = new Mapping();
        mapping.put("key", "value");
        mapping.put("key2", "value2");
        assertEquals(mapping, this.mocker.getComponentUnderTest().convert(Mapping.class, "key=value|key2=value2"));
        assertEquals(mapping, this.mocker.getComponentUnderTest().convert(Mapping.class, "key=value\nkey2=value2"));
        assertEquals(mapping, this.mocker.getComponentUnderTest().convert(Mapping.class, "key=value\nkey2=value2|key3"));
    }
}
