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
package org.xwiki.contrib.confluence.resolvers.internal;

import java.util.List;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageIdResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageTitleResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.contrib.confluence.resolvers.ConfluenceSpaceKeyResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.xwiki.query.Query.HQL;

/**
 * Attempts to find a document from its confluence ID using Confluence.Code.ConfluencePageClass objects optionally
 * pushed by the confluence-xml package.
 * @since 9.54.0
 * @version $Id$
 */
@Component
@Named("confluencepageclass")
@Singleton
@Priority(900)
public class PageClassConfluenceResolver
    implements ConfluencePageIdResolver, ConfluencePageTitleResolver, ConfluenceSpaceKeyResolver
{
    // The following HQL statement was translated from the following XWQL statement:
    // ---
    // select doc from Document doc, doc.object(Confluence.Code.ConfluencePageClass) o where o.id = :id
    // ---
    // This is because XWQL requires Confluence.Code.ConfluencePageClass to be present in the wiki
    // while the translated HQL does not. This makes the query a little bit more robust.

    private static final String VALUE = "value";
    private static final String SPACE = "space";

    private static final String CONFLUENCEPAGECLASS_HQL_TEMPLATE = "select doc "
        + "from XWikiDocument doc, BaseObject o, "
        + "%1$s "
        + "where "
        + "%2$sProp.value = :value and "
        + "doc.fullName = o.name and "
        + "o.className = 'Confluence.Code.ConfluencePageClass' and "
        + "%2$sProp.id.id = o.id and "
        + "%2$sProp.id.name = '%2$s'";

    private static final String ID_USING_CONFLUENCEPAGECLASS =
        String.format(CONFLUENCEPAGECLASS_HQL_TEMPLATE, "LongProperty idProp", "id");

    private static final String TITLE_USING_CONFLUENCEPAGECLASS =
        String.format(CONFLUENCEPAGECLASS_HQL_TEMPLATE,
            "LargeStringProperty titleProp, StringProperty spaceProp", "title")
            + " and spaceProp.id.id = o.id and spaceProp.id.name = '" + SPACE + "' and spaceProp.value = :space";

    private static final String SPACE_USING_CONFLUENCEPAGECLASS =
        String.format(CONFLUENCEPAGECLASS_HQL_TEMPLATE, "StringProperty spaceProp", SPACE)
            + " order by length(doc.fullName) asc";

    @Inject
    private QueryManager queryManager;

    @Inject
    private Logger logger;

    @Override
    public EntityReference getDocumentById(long id) throws ConfluenceResolverException
    {
        try {
            return getDocument(queryManager.createQuery(ID_USING_CONFLUENCEPAGECLASS, HQL)
                .bindValue(VALUE, id));
        } catch (QueryException e) {
            throw new ConfluenceResolverException(e);
        }
    }

    @Override
    public EntityReference getDocumentByTitle(String spaceKey, String title) throws ConfluenceResolverException
    {
        try {
            return getDocument(queryManager.createQuery(TITLE_USING_CONFLUENCEPAGECLASS, HQL)
                .bindValue(SPACE, spaceKey)
                .bindValue(VALUE, title));
        } catch (QueryException e) {
            throw new ConfluenceResolverException(e);
        }
    }

    private EntityReference getDocument(Query query) throws ConfluenceResolverException
    {
        List<Object> results;
        try {
            results = query.setLimit(1).execute();
        } catch (QueryException e) {
            throw new ConfluenceResolverException(e);
        }

        if (results.isEmpty()) {
            return null;
        }

        return ((XWikiDocument) results.get(0)).getDocumentReference();
    }

    @Override
    public EntityReference getSpaceByKey(String spaceKey) throws ConfluenceResolverException
    {
        try {
            EntityReference spaceHome = getDocument(queryManager.createQuery(SPACE_USING_CONFLUENCEPAGECLASS, HQL)
                .bindValue(VALUE, spaceKey));
            if (spaceHome == null) {
                return null;
            }

            return spaceHome.getParent();
        } catch (QueryException e) {
            throw new ConfluenceResolverException(e);
        }
    }
}
