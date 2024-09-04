package org.xwiki.contrib.confluence.filter.internal.macros;

import java.util.Map;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * Converts a macro call into a group, using the value of the "text" parameter as its content.
 */
@Component(hints = { "cfm-footnote", "cfm-tooltip", "tooltip" })
@Singleton
public class TextParamToContentConverter extends MacroToContentConverter
{
    @Override
    protected String toXWikiContent(String confluenceId, Map<String, String> parameters, String confluenceContent)
    {
        return parameters.getOrDefault("text", "");
    }
}
