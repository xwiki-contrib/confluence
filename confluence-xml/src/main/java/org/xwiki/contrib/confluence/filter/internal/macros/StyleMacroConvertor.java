package org.xwiki.contrib.confluence.filter.internal.macros;

import java.util.Collections;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * Convert the style macro.
 *
 * @version $Id$
 * @since 9.52.0
 */
@Component
@Singleton
@Named("style")
public class StyleMacroConvertor extends AbstractMacroConverter
{
    private static final String IMPORT = "import";

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return "html";
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        return Collections.singletonMap("clean", "false");
    }

    @Override
    protected String toXWikiContent(String confluenceId, Map<String, String> parameters, String confluenceContent)
    {
        StringBuilder stringBuilder = new StringBuilder();

        if (confluenceContent != null && !confluenceContent.isEmpty()) {
            stringBuilder.append("<style>");
            stringBuilder.append(confluenceContent);
            stringBuilder.append("</style>");
        }

        if (parameters.containsKey(IMPORT)) {
            stringBuilder.append("<link rel=\"stylesheet\"");
            stringBuilder.append("href=");
            stringBuilder.append(parameters.get(IMPORT));
            stringBuilder.append(">");
        }
        return stringBuilder.toString();
    }
}


