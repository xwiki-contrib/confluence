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
package org.xwiki.contrib.confluence.filter.url.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.url.AbstractConfluenceURLConverter;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

import javax.inject.Named;

/**
 * Test the modularity of URL conversion.
 * @since 9.76.0
 * @version $Id$
 */
@Component
@Named("fake")
public class FakeConfluenceURLConverter extends AbstractConfluenceURLConverter
{
    @Override
    protected ResourceReference convertPath(String path)
    {
        if ("fake/path".equals(path)) {
            return new ResourceReference("http://perdu.con", ResourceType.URL);
        }
        return null;
    }
}
