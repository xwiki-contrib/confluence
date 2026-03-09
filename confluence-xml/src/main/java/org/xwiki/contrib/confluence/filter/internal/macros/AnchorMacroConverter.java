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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.AbstractMacroConverter;
import org.xwiki.contrib.confluence.filter.ConfluenceFilterReferenceConverter;
import org.xwiki.contrib.confluence.filter.ConversionException;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverter;
import org.xwiki.rendering.listener.Listener;

import static org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverter.getConfluenceServerAnchor;
import static org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverter.spacesToDash;

/**
 * Convert Confluence anchor macro.
 * 
 * @version $Id$
 * @since 9.1
 */
@Component
@Singleton
@Named("anchor")
public class AnchorMacroConverter extends AbstractMacroConverter
{
    @Inject
    private ConfluenceInputContext context;

    @Inject
    private ConfluenceFilterReferenceConverter confluenceConverter;


    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return null;
    }

    @Override
    protected void toXWiki(String confluenceId, Map<String, String> confluenceParameters,
        boolean inline, String confluenceContent, Listener listener) throws ConversionException
    {
        // Sometimes, anchors use <ac:parameter ac:name="">the-anchor</ac:parameter>
        // Sometimes, they use <ac:default-parameter>the-anchor</ac:default-parameter>
        // This leads to the two following possible parameter name.
        String anchor = confluenceParameters.get("");
        if (StringUtils.isEmpty(anchor)) {
            anchor = confluenceParameters.get("0");
        }
        if (StringUtils.isEmpty(anchor)) {
            throw new ConversionException("The anchor macro is missing its main parameter");
        }

        // FIXME find a way to expose cleanly these deprecated methods
        String currentPageTitle = ((ConfluenceConverter) confluenceConverter).getCurrentPageTitleForAnchor();

        if (context.isConfluenceCloud()) {
            String dashedName = spacesToDash(anchor);
            listener.onId(dashedName);

            if (currentPageTitle != null) {
                listener.onId(spacesToDash(currentPageTitle) + '-' + dashedName);
            }
        } else if (currentPageTitle != null) {
            String confluenceServerAnchor = getConfluenceServerAnchor(currentPageTitle, anchor);
            listener.onId(confluenceServerAnchor);
        }
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content) throws ConversionException
    {
        return null;
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.YES;
    }
}
