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

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.properties.converter.ConversionException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Converter that converts a value into a Link Mapping object.
 *
 * @version $Id$
 * @since 9.40.0
 */
@Component
@Singleton
public class LinkMappingConverter extends AbstractConverter<Map<String, Map<String, EntityReference>>>
{
    private final TypeReference<Map<String, Map<String, EntityReference>>> typeRef =
        new TypeReference<Map<String, Map<String, EntityReference>>>() { };

    @Inject
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    protected Map<String, Map<String, EntityReference>> convertToType(Type targetType, Object value)
    {
        if (value == null) {
            return null;
        }

        if (value instanceof Map) {
            // We hope the type is correct
            return (Map<String, Map<String, EntityReference>>) value;
        }

        if (!(value instanceof String)) {
            throw new  ConversionException("Unsupported type");
        }

        String json = (String) value;
        if (StringUtils.isEmpty(json)) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(EntityReference.class, new JsonDeserializer<EntityReference>()
        {
            @Override
            public EntityReference deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException
            {
                return entityReferenceResolver.resolve(
                    deserializationContext.readValue(jsonParser, String.class), EntityType.DOCUMENT);
            }
        });
        mapper.registerModule(simpleModule);
        try {
            return mapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new ConversionException(e);
        }
    }

    @Override
    protected String convertToString(Map<String, Map<String, EntityReference>>  value)
    {
        if (value == null) {
            return "";
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            SimpleModule simpleModule = new SimpleModule();
            simpleModule.addSerializer(EntityReference.class, new JsonSerializer<EntityReference>()
            {
                @Override
                public void serialize(EntityReference entityReference, JsonGenerator gen, SerializerProvider provider)
                    throws IOException
                {
                    gen.writeString(entityReferenceSerializer.serialize(entityReference));
                }
            });
            mapper.registerModule(simpleModule);
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ConversionException(e);
        }
    }
}
