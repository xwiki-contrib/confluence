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

import java.lang.reflect.Type;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.Mapping;
import org.xwiki.properties.converter.AbstractConverter;

/**
 * Converter for {@link Mapping}.
 * 
 * @version $Id$
 * @since 9.11
 */
@Component
@Singleton
public class MappingConverter extends AbstractConverter<Mapping>
{
    @Inject
    private Logger logger;

    @Override
    protected Mapping convertToType(Type targetType, Object value)
    {
        if (value == null) {
            return null;
        }

        if (value instanceof Map) {
            return new Mapping((Map) value);
        }

        return convertToMapping(value.toString());
    }

    private Mapping convertToMapping(String value)
    {
        Mapping mapping = new Mapping();

        if (value.trim().length() > 0) {
            char[] buffer = value.trim().toCharArray();
            boolean escaped = false;
            StringBuilder pair = new StringBuilder(value.length());
            for (int i = 0; i < buffer.length; ++i) {
                char c = buffer[i];

                if (escaped) {
                    pair.append(c);
                    escaped = false;
                } else {
                    if (c == '\\') {
                        escaped = true;
                    } else if (c == '|' || c == '\n') {
                        addPair(pair.toString(), mapping);
                        pair.setLength(0);
                    } else {
                        pair.append(c);
                    }
                }
            }

            if (pair.length() > 0) {
                addPair(pair.toString(), mapping);
            }
        }

        return mapping;
    }

    private void addPair(String pair, Mapping mapping)
    {
        int splitIndex = pair.indexOf('=');

        if (splitIndex < 1) {
            this.logger.warn("Error parsing mapping element [{}], missing [=] separator. Ignoring it.", pair);
        } else {
            String key = pair.substring(0, splitIndex);
            String value = pair.substring(splitIndex + 1);

            mapping.put(key, value);
        }
    }
}
