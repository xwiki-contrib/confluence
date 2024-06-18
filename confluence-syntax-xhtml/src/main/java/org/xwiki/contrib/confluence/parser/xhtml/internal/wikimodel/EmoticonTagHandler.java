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
package org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel;

import org.xwiki.rendering.wikimodel.WikiParameter;
import org.xwiki.rendering.wikimodel.WikiParameters;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles emojis.
 * Preceding whitespaces are handled by adding ac:emoticon to EMPTYVISIBLE_ELEMENTS
 * in ConfluenceXHTMLWhitespaceXMLFilter.
 * @since 9.48.0
 * @version $Id$
 */
public class EmoticonTagHandler extends AbstractConfluenceTagHandler implements ConfluenceTagHandler
{
    /**
     * The map of Confluence emojis.
     * To build it:
     * 1. edit a page in Confluence
     * 2. type "/emoji"
     * 3. When you have the list of emojis you can insert:
     *   - right-click on it
     *   - inspect with the web inspector
     *   - right-click on the DOM element containing the whole list and select "Use in the console".
     *     We will assume the variable containing the DOM element is called temp0
     *   - type:
     *          emojiMap = {}
     *          t = setInterval(() => {
     *              [].forEach.call(
     *                  temp0.querySelectorAll("div [data-emoji-text]"),
     *                  ({dataset}) => {emojiMap[dataset.emojiShortName] = dataset.emojiText}
     *              )},
     *              100
     *          );
     *  4. scroll the whole list several times
     *  5. the mapping is in emojiMap. Save it and close the page or at least call clearInterval(t).
     */
    private static final Map<String, String> EMOJI_MAP = new HashMap<>();
    static {
        EMOJI_MAP.put(":smile:", "😄");
        EMOJI_MAP.put(":laughing:", "😆");
        EMOJI_MAP.put(":sweat_smile:", "😅");
        EMOJI_MAP.put(":joy:", "😂");
        EMOJI_MAP.put(":rofl:", "🤣");
        EMOJI_MAP.put(":relaxed:", "☺️");
        EMOJI_MAP.put(":blush:", "😊");
        EMOJI_MAP.put(":innocent:", "😇");
        EMOJI_MAP.put(":upside_down:", "🙃");
        EMOJI_MAP.put(":wink:", "😉");
        EMOJI_MAP.put(":relieved:", "😌");
        EMOJI_MAP.put(":smiling_face_with_tear:", "🥲");
        EMOJI_MAP.put(":heart_eyes:", "😍");
        EMOJI_MAP.put(":smiling_face_with_3_hearts:", "🥰");
        EMOJI_MAP.put(":kissing_heart:", "😘");
        EMOJI_MAP.put(":kissing:", "😗");
        EMOJI_MAP.put(":kissing_smiling_eyes:", "😙");
        EMOJI_MAP.put(":kissing_closed_eyes:", "😚");
        EMOJI_MAP.put(":yum:", "😋");
        EMOJI_MAP.put(":stuck_out_tongue:", "😛");
        EMOJI_MAP.put(":stuck_out_tongue_closed_eyes:", "😝");
        EMOJI_MAP.put(":stuck_out_tongue_winking_eye:", "😜");
        EMOJI_MAP.put(":zany_face:", "🤪");
        EMOJI_MAP.put(":face_with_raised_eyebrow:", "🤨");
        EMOJI_MAP.put(":face_with_monocle:", "🧐");
        EMOJI_MAP.put(":nerd:", "🤓");
        EMOJI_MAP.put(":sunglasses:", "😎");
        EMOJI_MAP.put(":star_struck:", "🤩");
        EMOJI_MAP.put(":partying_face:", "🥳");
        EMOJI_MAP.put(":smirk:", "😏");
        EMOJI_MAP.put(":unamused:", "😒");
        EMOJI_MAP.put(":disappointed:", "😞");
        EMOJI_MAP.put(":pensive:", "😔");
        EMOJI_MAP.put(":worried:", "😟");
        EMOJI_MAP.put(":confused:", "😕");
        EMOJI_MAP.put(":slight_frown:", "🙁");
        EMOJI_MAP.put(":frowning2:", "☹️");
        EMOJI_MAP.put(":persevere:", "😣");
        EMOJI_MAP.put(":confounded:", "😖");
        EMOJI_MAP.put(":tired_face:", "😫");
        EMOJI_MAP.put(":weary:", "😩");
        EMOJI_MAP.put(":pleading_face:", "🥺");
        EMOJI_MAP.put(":cry:", "😢");
        EMOJI_MAP.put(":sob:", "😭");
        EMOJI_MAP.put(":triumph:", "😤");
        EMOJI_MAP.put(":face_exhaling:", "😮‍💨");
        EMOJI_MAP.put(":smiley:", "😃");
        EMOJI_MAP.put(":grin:", "😁");
        EMOJI_MAP.put(":grinning:", "😀");
        EMOJI_MAP.put(":slight_smile:", "🙂");
    }

    /**
     * Default constructor.
     * @since 9.48.0
     */
    public EmoticonTagHandler()
    {
        super(false);
    }

    @Override
    protected void begin(TagContext context)
    {
        WikiParameters params = context.getParams();

        if (!sendFallback(context, params) && !sendShortName(context, params) && !sendName(context, params)) {
            context.getScannerContext().onMacro("confluence_emoticon", params, null, true);
        }

        super.begin(context);
    }

    private static boolean sendName(TagContext context, WikiParameters params)
    {
        WikiParameter nameParam = params.getParameter("ac:name");
        if (nameParam != null) {
            // Old confluence exports only contain a name, without columns. We don't have the mapping for this.
            // Let's try anyway.
            String name = nameParam.getValue();
            if (name != null && !name.isEmpty()) {
                String emoji = "warning".equals(name) ? "⚠️" : EMOJI_MAP.get(':' + name + ':');
                if (emoji != null && !emoji.isEmpty()) {
                    context.getScannerContext().onWord(emoji);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean sendShortName(TagContext context, WikiParameters params)
    {
        WikiParameter shortnameParam = params.getParameter("ac:emoji-shortname");
        if (shortnameParam != null) {
            String shortname = shortnameParam.getValue();
            if (shortname != null && !shortname.isEmpty()) {
                String emoji = EMOJI_MAP.get(shortname);
                if (emoji != null && !emoji.isEmpty()) {
                    context.getScannerContext().onWord(emoji);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean sendFallback(TagContext context, WikiParameters params)
    {
        WikiParameter fallbackParam = params.getParameter("ac:emoji-fallback");
        if (fallbackParam != null) {
            String emoji = fallbackParam.getValue();
            if (emoji != null && !emoji.isEmpty()) {
                context.getScannerContext().onWord(emoji);
                return true;
            }
        }
        return false;
    }
}
