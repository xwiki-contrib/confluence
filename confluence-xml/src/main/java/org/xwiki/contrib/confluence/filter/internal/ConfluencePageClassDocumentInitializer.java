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
package org.xwiki.contrib.confluence.filter.internal;

import java.util.Arrays;

import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.objects.classes.TextAreaClass;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Update Confluence.Code.ConfluencePageClass document with all required information.
 *
 * @version $Id$
 * @since 9.13
 */
@Component
@Named("Confluence.Code.ConfluencePageClass")
@Singleton
public class ConfluencePageClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * Local reference of the XWikiUsers class document.
     */
    public static final LocalDocumentReference CONFLUENCE_PAGE_CLASS_DOCUMENT_REFERENCE =
        new LocalDocumentReference(Arrays.asList("Confluence", "Code"), "ConfluencePageClass");

    /**
     * Default constructor.
     */
    public ConfluencePageClassDocumentInitializer()
    {
        super(CONFLUENCE_PAGE_CLASS_DOCUMENT_REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addNumberField("id", "Id", 30, "long");
        xclass.addTextField("url", "URL", 30);
        xclass.addTextField("space", "Space", 30);
        xclass.addTextAreaField("title", "Title", 30, 15, TextAreaClass.ContentType.PURE_TEXT);
    }
}
