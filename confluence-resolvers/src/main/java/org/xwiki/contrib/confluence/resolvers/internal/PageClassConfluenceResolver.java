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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.resolvers.ConfluenceSpaceResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageIdResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageTitleResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.contrib.confluence.resolvers.ConfluenceSpaceKeyResolver;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.apache.solr.client.solrj.response.QueryResponse;

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
    implements ConfluencePageIdResolver, ConfluencePageTitleResolver, ConfluenceSpaceKeyResolver,
    ConfluenceSpaceResolver
{
    private static final String SOLR = "solr";
    private static final String FULLNAME = "fullname";

    private static final String CONFLUENCE_PROP = "property.Confluence.Code.ConfluencePageClass.";
    private static final String SPACE = "space";
    private static final String TITLE = "title";
    private static final String ID = "id";

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localReferenceSerializer;

    @Inject
    private DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

    @Override
    public EntityReference getDocumentById(long id) throws ConfluenceResolverException
    {
        return getDocument(Map.of(ID, id, "stableId", id), false, false);
    }

    @Override
    public EntityReference getDocumentByTitle(String spaceKey, String title) throws ConfluenceResolverException
    {
        return getDocument(Map.of(SPACE, spaceKey, TITLE, title), false, true);
    }

    private String solrQuotes(Object v)
    {
        if (v instanceof Number) {
            return v.toString();
        }

        return '"' + v.toString().replace("\"", "") + '"';
    }

    private EntityReference getDocument(Map<String, Object> values, boolean smallest, boolean andOp)
        throws ConfluenceResolverException
    {
        // we use Solr search because it searches in all wikis. It is less comfortable than HQL because matches are not
        // exact so some extra work is needed to handle this fact of life.
        SolrDocumentList results;
        String queryString = values.entrySet().stream()
            .map(entry -> CONFLUENCE_PROP + entry.getKey() + ':' + solrQuotes(entry.getValue()))
            .collect(Collectors.joining(andOp ? " AND " : " OR "));
        try {
            Query query = queryManager.createQuery(queryString, SOLR)
                .bindValue("fq", "type:DOCUMENT")
                .setLimit(Integer.MAX_VALUE);
            results = ((QueryResponse) query.execute().get(0)).getResults();
        } catch (QueryException e) {
            throw new ConfluenceResolverException(e);
        }

        if (smallest) {
            // When looking for a space, it is the document with the given space key with the smallest full name
            return smallestMatching(results, values, andOp);
        }

        // We need to check for exact match because Solr can return documents whose title contains the title we are
        // looking for, and doesn't generally do an exact match, because properties are not indexed with exact values.
        return getFirstExactMatch(results, values, andOp);
    }

    private EntityReference smallestMatching(SolrDocumentList results, Map<String, Object> values, boolean andOp)
    {
        SolrDocument shortest = null;
        int length = 0;
        for (SolrDocument result: results) {
            if (resultExactlyMatches(result, values, andOp)) {
                String fullname = (String) result.get(FULLNAME);
                int len = fullname.length();
                if (StringUtils.isNotEmpty(fullname) && (shortest == null || len < length)) {
                    length = len;
                    shortest = result;
                }
            }
        }

        return shortest == null ? null : solrDocumentReferenceResolver.resolve(shortest);
    }

    EntityReference getFirstExactMatch(SolrDocumentList results, Map<String, Object> values, boolean andOp)
    {
        for (SolrDocument result : results) {
            if (resultExactlyMatches(result, values, andOp)) {
                return solrDocumentReferenceResolver.resolve(result);
            }
        }

        return null;
    }

    private boolean resultExactlyMatches(SolrDocument result, Map<String, Object> values, boolean andOp)
    {
        for (Map.Entry<String, Object> value : values.entrySet()) {
            if (value.getValue() instanceof String) {
                String v = (String) value.getValue();
                String valueKeyInSolr = CONFLUENCE_PROP + value.getKey() + "_string";
                if (!eq(v, result.get(valueKeyInSolr))) {
                    return false;
                }
                if (!andOp) {
                    // only one value needs to match
                    return true;
                }
            }
        }
        return true;
    }

    private boolean eq(String v, Object valueFromSolr)
    {
        if (valueFromSolr instanceof Collection) {
            return ((Collection<?>) valueFromSolr).contains(v);
        }
        if (valueFromSolr instanceof String) {
            return valueFromSolr.equals(v);
        }

        return false;
    }

    @Override
    public EntityReference getSpaceByKey(String spaceKey) throws ConfluenceResolverException
    {
        EntityReference spaceHome = getDocument(Collections.singletonMap(SPACE, spaceKey), true, true);
        if (spaceHome == null) {
            return null;
        }

        return spaceHome.getParent();
    }

    @Override
    public EntityReference getSpace(EntityReference reference) throws ConfluenceResolverException
    {
        String spaceKey = getSpaceKey(reference);
        if (StringUtils.isNotEmpty(spaceKey)) {
            return getSpaceByKey(spaceKey);
        }
        return null;
    }

    @Override
    public String getSpaceKey(EntityReference reference) throws ConfluenceResolverException
    {
        String fullName = localReferenceSerializer.serialize(reference);
        if (reference.getType() == EntityType.SPACE) {
            fullName += ".WebHome";
        }
        try {
            Query q = queryManager.createQuery(
                "select p.value from BaseObject o, StringProperty p where "
                    + "o.className = 'Confluence.Code.ConfluencePageClass' and p.id.id = o.id and "
                    + " o.name = :fullname and p.id.name = 'space'",
                HQL).bindValue(FULLNAME, fullName).setLimit(1);
            if (reference.getRoot().getType() == EntityType.WIKI) {
                q = q.setWiki(reference.getRoot().getName());
            }
            List<String> res = q.execute();
            if (!res.isEmpty()) {
                return res.get(0);
            }
        } catch (QueryException e) {
            throw new ConfluenceResolverException(String.format("Failed to find the space key of [%s]", reference), e);
        }
        return null;
    }
}
