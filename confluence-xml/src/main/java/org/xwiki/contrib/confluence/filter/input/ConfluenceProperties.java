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
package org.xwiki.contrib.confluence.filter.input;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DisabledListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;

/**
 * @version $Id$
 * @since 9.16
 */
public class ConfluenceProperties extends PropertiesConfiguration
{
    private static final String PROPERTY_CLASS_SUFFIX = "--class";

    private FileBasedConfigurationBuilder<ConfluenceProperties> builder;

    /**
     * Default constructor.
     */
    public ConfluenceProperties()
    {
        setInterpolator(null);
    }

    /**
     * @param file the file to load/save
     * @return the new {@link ConfluenceProperties}
     * @throws ConfigurationException when failing to create the {@link ConfluenceProperties}
     */
    public static ConfluenceProperties create(File file) throws ConfigurationException
    {
        URL url;
        try {
            url = file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Could not get the URL of the properties file", e);
        }

        FileBasedConfigurationBuilder<ConfluenceProperties> builder =
            new FileBasedConfigurationBuilder<>(ConfluenceProperties.class, null, true)
                .configure(new Parameters()
                    .properties()
                    .setFile(file)
                    .setURL(url)
                    .setEncoding("UTF-8")
                    .setIOFactory(new JupIOFactory(false)));

        ConfluenceProperties properties = builder.getConfiguration();
        properties.builder = builder;

        return properties;
    }

    /**
     * Disable the list delimiter support.
     */
    public void disableListDelimiter()
    {
        setListDelimiterHandler(DisabledListDelimiterHandler.INSTANCE);
    }

    /**
     * @throws ConfigurationException when failing to save the properties
     */
    public void save() throws ConfigurationException
    {
        this.builder.save();
    }

    /**
     * @param attributeName the attribute name
     * @param className the class name
     * @since 9.96.0
     */
    public void setAttributeClass(String attributeName, String className)
    {
        setProperty(attributeName + PROPERTY_CLASS_SUFFIX, className);
    }

    /**
     * @param attributeName the attribute of which to get the class
     * @return the class of this attribute, or null if unknown
     * @since 9.96.0
     */
    public String getAttributeClass(String attributeName)
    {
        return getString(attributeName + PROPERTY_CLASS_SUFFIX, null);
    }

    @Override
    public Long getLong(String key, Long defaultValue)
    {
        try {
            return super.getLong(key, defaultValue);
        } catch (Exception e) {
            // Usually mean the field does not have the expected format
            return defaultValue;
        }
    }

    @Override
    public long getLong(String key, long defaultValue)
    {
        return getLong(key, (Long) defaultValue);
    }

    @Override
    @Deprecated(since = "9.96.0")
    public long getLong(String key)
    {
        return super.getLong(key);
    }
}
