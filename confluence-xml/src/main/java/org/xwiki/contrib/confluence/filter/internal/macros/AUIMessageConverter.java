package org.xwiki.contrib.confluence.filter.internal.macros;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * Converts auimessage to either info, success, warning or error macros.
 * https://aui.atlassian.com/aui/7.9/docs/messages.html
 *
 * @since 9.51.1
 */
@Component
@Named("auimessage")
@Singleton
public class AUIMessageConverter extends AbstractMacroConverter
{
    private static final String PARAM_KEY_TITLE = "title";

    private static final String PARAM_KEY_CLASS = "class";

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        switch (confluenceParameters.getOrDefault("type", "")) {
            case "":
            case "generic":
            case "hint":
                return "info";
            default:
                return confluenceParameters.get("type");
        }
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> newParams = new HashMap<>();

        if (confluenceParameters.containsKey(PARAM_KEY_TITLE)) {
            newParams.put(PARAM_KEY_TITLE, confluenceParameters.get(PARAM_KEY_TITLE));
        }

        if (confluenceParameters.containsKey(PARAM_KEY_CLASS)) {
            newParams.put("cssClass", confluenceParameters.get(PARAM_KEY_CLASS));
        }

        return newParams;
    }
}
