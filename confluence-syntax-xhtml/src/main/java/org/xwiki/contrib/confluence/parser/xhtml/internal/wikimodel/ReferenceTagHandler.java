package org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel;

import org.xwiki.rendering.internal.parser.xhtml.wikimodel.XWikiReferenceTagHandler;
import org.xwiki.rendering.wikimodel.WikiParameter;
import org.xwiki.rendering.wikimodel.xhtml.handler.TagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagStack;

import static org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.AbstractMacroParameterTagHandler.IN_CONFLUENCE_PARAMETER;
import static org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel.ConfluenceTagHandler.CONFLUENCE_CONTAINER;

/**
 * Handles <a> tags present in confluence parameters.
 * Example:
 * <ac:structured-macro ac:name="macroName">
 *   <ac:parameter ac:name="linkParam">
 *     <a href="https://www.bing.com/images/search?q=icon">https://www.bing.com/images/search?q=icon</a>
 *   </ac:parameter>
 * </ac:structured-macro>
 */
public class ReferenceTagHandler extends TagHandler
{
    private final XWikiReferenceTagHandler tagHandler;

    public ReferenceTagHandler(XWikiReferenceTagHandler tagHandler)
    {
        super(tagHandler.isContentContainer());
        this.tagHandler = tagHandler;
    }

    @Override
    public void initialize(TagStack stack)
    {
        this.tagHandler.initialize(stack);
    }

    @Override
    protected void begin(TagContext context)
    {
        if (context.getTagStack().getStackParameter(IN_CONFLUENCE_PARAMETER) != null) {
            setAccumulateContent(true);
        } else {
            this.tagHandler.beginElement(context);
        }
    }

    @Override
    protected void end(TagContext context)
    {
        if (context.getTagStack().getStackParameter(IN_CONFLUENCE_PARAMETER) != null) {
            MacroTagHandler.ConfluenceMacro macro =
                (MacroTagHandler.ConfluenceMacro) context.getTagStack().getStackParameter(CONFLUENCE_CONTAINER);

            if (macro != null) {
                WikiParameter href = context.getParams().getParameter("href");
                WikiParameter name = context.getParent().getParams().getParameter("ac:name");
                if (href != null && name != null) {
                    macro.parameters = macro.parameters.addParameter(name.getValue(), href.getValue());
                }
            }
        } else {
            this.tagHandler.endElement(context);
        }
    }
}
