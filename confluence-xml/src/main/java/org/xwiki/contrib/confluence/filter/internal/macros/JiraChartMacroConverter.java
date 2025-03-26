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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * Convert Confluence jira chart macro.
 *
 * @version $Id$
 * @since 9.81.0
 */
@Component
@Singleton
@Named("jirachart")
public class JiraChartMacroConverter extends AbstractMacroConverter
{
    private static final Map<String, String> VERSION_LABEL_MAP = Map.of(
        "all", "ALL",
        "major", "ONLY_MAJOR",
        "none", "NONE"
    );

    private static final Pattern JQL_FILTER_PATTERN = Pattern.compile("^filter\\s*=\\s*(\\d+)$");

    private static final String CUSTOM = "CUSTOM";

    private static final String FALSE = "false";

    private static final String TRUE = "true";

    private static final String PARAM_CHART_TYPE = "chartType";

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        String type = confluenceParameters.get(PARAM_CHART_TYPE);
        switch (type) {
            case "pie":
                return "jiraPieChart";
            case "createdvsresolved":
                return "jiraCreatedVsResolvedChart";
            case "twodimensional":
                return "jiraBiDimensionalGridChart";
            default:
                return confluenceId;
        }
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> parameters = new LinkedHashMap<>(confluenceParameters.size());

        for (Map.Entry<String, String> entry : confluenceParameters.entrySet()) {
            String name = entry.getKey();
            String confluenceValue = entry.getValue();
            String xwikiValue = toXWikiParameterValue(name, confluenceValue, confluenceId, parameters, content);

            switch (name) {
                // Parameter used for all charts
                case PARAM_CHART_TYPE:
                    // Not needed as in XWiki we have a specific macro for each type
                    break;
                case "server":
                    parameters.put("id", xwikiValue);
                    break;
                case "jql":
                    xwikiValue = URLDecoder.decode(xwikiValue, StandardCharsets.UTF_8);
                    Matcher jqlFilterMatcher = JQL_FILTER_PATTERN.matcher(xwikiValue);
                    if (jqlFilterMatcher.find()) {
                        String filterId = "filter-" + jqlFilterMatcher.group(1);
                        parameters.put("filterId", filterId);
                    } else {
                        parameters.put("query", xwikiValue);
                    }
                    break;

                // Pie chart related parameters
                case "statType":
                    parameters.put("type", xwikiValue);
                    break;

                // CreatedVsResolved chart related parameters
                case "daysprevious":
                    parameters.put("daysPreviously", xwikiValue);
                    break;
                case "periodName":
                    parameters.put("period", xwikiValue.toUpperCase());
                    break;
                case "isCumulative":
                    // Need to inverse the boolean
                    String boolValue = TRUE.equalsIgnoreCase(xwikiValue) ? FALSE : TRUE;
                    parameters.put("count", boolValue);
                    break;
                case "showUnresolvedTrend":
                    parameters.put("displayTrend", xwikiValue);
                    break;
                case "versionLabel":
                    xwikiValue = VERSION_LABEL_MAP.getOrDefault(xwikiValue, xwikiValue);
                    parameters.put("displayVersion", xwikiValue);
                    break;

                // Bi Dimensional Grid chart related parameters
                case "xstattype":
                    parameters.put("xAxisField", xwikiValue);
                    break;
                case "ystattype":
                    parameters.put("yAxisField", xwikiValue);
                    break;
                case "numberToShow":
                    parameters.put("numberOfResults", xwikiValue);
                    break;

                default:
                    String parameterName = toXWikiParameterName(name, confluenceId, parameters, content);
                    parameters.put(parameterName, xwikiValue);
                    break;
            }
        }
        return parameters;
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.NO;
    }
}
