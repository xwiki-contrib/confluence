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
package org.xwiki.contrib.confluence.filter.internal.macros;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.RandomStringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverterListener;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.reference.UserResourceReference;

/**
 * Convert mention macro.
 *
 * @version $Id$
 * @since 9.19
 */
@Component
@Singleton
@Named("mention")
public class MentionMacroConverter extends AbstractMacroConverter
{
    private static final String REFERENCE_PARAMETER_KEY = "reference";

    @Override
    public void toXWiki(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline, Listener listener)
    {
        UserResourceReference userReference =
            new UserResourceReference(confluenceParameters.get(REFERENCE_PARAMETER_KEY));

        String stringReference =
            ((ConfluenceConverterListener) listener).resolveUserReference(userReference).getReference();

        confluenceParameters.put(REFERENCE_PARAMETER_KEY, stringReference);
        confluenceParameters.put("style", "FULL_NAME");
        confluenceParameters.put("anchor", createAnchor(stringReference));

        super.toXWiki(confluenceId, confluenceParameters, confluenceContent, inline, listener);
    }

    protected String createAnchor(String stringReference)
    {
        return stringReference.replace('.', '-') + '-' + RandomStringUtils.random(5, true, false);
    }
}
