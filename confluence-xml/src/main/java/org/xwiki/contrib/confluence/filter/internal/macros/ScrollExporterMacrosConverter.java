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

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.AbstractMacroConverter;
import org.xwiki.contrib.confluence.filter.ConversionException;

/**
 * Converts scroll- prefixed macros to export- prefixed macros.
 *
 * @version $Id$
 * @since 9.79.0
 */
@Singleton
@Component(hints = {
    "scroll-ignore", "scroll-indexterm", "scroll-only", "scroll-pagetitle", "scroll-pdf-ignore", "scroll-title",
    "scroll-pdf-only", "scroll-bookmark", "scroll-ignore-inline", "scroll-exportbutton", "scroll-only-inline",
    "scroll-pdf-ignore-inline", "scroll-content-block", "scroll-landscape", "scroll-portrait",
    "scroll-pagebreak", "scroll-tablelayout"
})
public class ScrollExporterMacrosConverter extends AbstractMacroConverter
{
    private static final String SCROLL_PREFIX = "scroll-";

    private static final String SUFFIX_INLINE = "-inline";

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        String macroName = confluenceId.replace(SCROLL_PREFIX + "pdf-", SCROLL_PREFIX);
        macroName = macroName.replace(SUFFIX_INLINE, "");
        return macroName.replace(SCROLL_PREFIX, "export-");
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content) throws ConversionException
    {
        return confluenceParameters;
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return id.endsWith(SUFFIX_INLINE) ? InlineSupport.YES : InlineSupport.NO;
    }
}
