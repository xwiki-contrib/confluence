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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;
import org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceInputFilterStream;
import org.xwiki.environment.Environment;
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.test.integration.FilterTestSuite;
import org.xwiki.model.validation.EntityNameValidation;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.XWikiTempDirUtil;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Run all tests found in the classpath. These {@code *.test} files must follow the conventions described in
 * {@link org.xwiki.filter.test.integration.TestDataParser}.
 * 
 * @version $Id$
 */
@RunWith(FilterTestSuite.class)
@AllComponents
@FilterTestSuite.Scope(value = "confluencexml"/*, pattern = "images.test"*/)
public class IntegrationTests
{
    @FilterTestSuite.Initialized
    public void initialized(MockitoComponentManager componentManager) throws Exception
    {
        Environment environment = componentManager.registerMockComponent(Environment.class);
        when(environment.getTemporaryDirectory()).thenReturn(XWikiTempDirUtil.createTemporaryDirectory());

        EntityNameValidationManager validationManager =
            componentManager.registerMockComponent(EntityNameValidationManager.class);
        EntityNameValidation validation = mock(EntityNameValidation.class);
        when(validationManager.getEntityReferenceNameStrategy()).thenReturn(validation);
        when(validation.transform(anyString())).thenAnswer(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return (String) invocation.getArgument(0);
            }
        });
        when(validation.transform("spacetovalidate")).thenReturn("validatedspace");
        when(validation.transform("pagetovalidate")).thenReturn("validatedpage");

        // Unregister all listeners since they are not needed for testing
        componentManager.registerMockComponent(ObservationManager.class);
        // Unregister the instance input filter stream factory since we don't need it here
        componentManager.registerMockComponent(InputFilterStreamFactory.class, "xwiki+instance");

        // Many INFO-level logs are to inform the user about the progression of migrations but they spam the tests.
        Logger logger = (Logger) LoggerFactory.getLogger(ConfluenceXMLPackage.class);
        logger.setLevel(Level.WARN);
        logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ConfluenceInputFilterStream.class);
        logger.setLevel(Level.WARN);
    }
}
