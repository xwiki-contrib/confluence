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
package org.xwiki.contrib.confluence.filter.url;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceURLConverter;
import org.xwiki.rendering.listener.reference.ResourceReference;

import javax.inject.Inject;
import java.net.URL;

/**
 * Abstract Confluence URL Converter that provide a convenience convertPath abstract method and handles the baseURL
 * part which ought to be common to all URL converters.
 * @version $Id$
 * @since 9.76.0
 */
public abstract class AbstractConfluenceURLConverter implements ConfluenceURLConverter
{
    @Inject
    private ConfluenceInputContext context;

    @Override
    public ResourceReference convertURL(String url)
    {
        for (URL baseURL : context.getProperties().getBaseURLs()) {
            String baseURLString = baseURL.toExternalForm();

            if (url.startsWith(baseURLString)) {
                // Fix the URL if the format is known
                String path = StringUtils.removeStart(url.substring(baseURLString.length()), "/");
                ResourceReference convertedPath = convertPath(path);
                if (convertedPath != null) {
                    return convertedPath;
                }
            }
        }
        return null;
    }

    /**
     * @param path the url to convert, without the domain and the slashes at the start
     * @return the converted URL.
     */
    protected abstract ResourceReference convertPath(String path);
}
