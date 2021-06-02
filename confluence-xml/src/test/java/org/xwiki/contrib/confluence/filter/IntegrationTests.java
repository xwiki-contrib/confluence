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
package org.xwiki.contrib.confluence.filter;

import java.io.File;
import java.util.Date;

import org.junit.runner.RunWith;
import org.xwiki.environment.Environment;
import org.xwiki.filter.test.integration.FilterTestSuite;
import org.xwiki.observation.EventListener;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.mockito.Mockito.when;

/**
 * Run all tests found in the classpath. These {@code *.test} files must follow the conventions described in
 * {@link org.xwiki.filter.test.integration.TestDataParser}.
 * 
 * @version $Id$
 */
@RunWith(FilterTestSuite.class)
@AllComponents
@FilterTestSuite.Scope(value = "confluencexml"/* , pattern = "content.test" */)
public class IntegrationTests
{
    @FilterTestSuite.Initialized
    public void initialized(MockitoComponentManager componentManager) throws Exception
    {
        Environment environment = componentManager.registerMockComponent(Environment.class);
        // Unregister this component which is not needed for testing.
        componentManager.unregisterComponent(EventListener.class, "default");

        File tmpDir = new File("target/test-" + new Date().getTime()).getAbsoluteFile();
        tmpDir.mkdirs();
        when(environment.getTemporaryDirectory()).thenReturn(tmpDir);
    }
}
