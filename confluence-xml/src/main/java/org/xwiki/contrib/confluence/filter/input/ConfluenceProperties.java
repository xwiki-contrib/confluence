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
    private FileBasedConfigurationBuilder<ConfluenceProperties> builder;

    /**
     * @param file the file to load/save
     * @return the new {@link ConfluenceProperties}
     * @throws ConfigurationException when failing to create the {@link ConfluenceProperties}
     */
    public static ConfluenceProperties create(File file) throws ConfigurationException
    {
        FileBasedConfigurationBuilder<ConfluenceProperties> builder =
            new FileBasedConfigurationBuilder<>(ConfluenceProperties.class, null, true)
                .configure(new Parameters().properties().setFile(file));

        ConfluenceProperties properties = builder.getConfiguration();
        properties.builder = builder;

        // Disable interpolation
        properties.setInterpolator(null);

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
}
