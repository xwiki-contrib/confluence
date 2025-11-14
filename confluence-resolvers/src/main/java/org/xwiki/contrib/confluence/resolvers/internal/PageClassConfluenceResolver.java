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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.resolvers.ConfluenceSpaceResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageIdResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluencePageTitleResolver;
import org.xwiki.contrib.confluence.resolvers.ConfluenceResolverException;
import org.xwiki.contrib.confluence.resolvers.ConfluenceSpaceKeyResolver;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

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
    private static final String CONFLUENCE_PROP = "property.Confluence.Code.ConfluencePageClass.";
    private static final String SPACE = "space_string";
    private static final String SPACE_LOWER = "space_string_lowercase";
    private static final String TITLE = "title_string";
    private static final String TITLE_LOWER = "title_string_lowercase";
    private static final String ID = "id_long";
    private static final String SORT_CREATIONDATE = "creationdate asc";
    private static final String STABLE_ID = "stableId_long";

    @Inject
    private QueryManager queryManager;

    @Inject
    private SolrUtils solrUtils;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localReferenceSerializer;

    @Inject
    private DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

    @Inject
    private Solr solr;

    @Inject
    private Logger logger;

    private boolean caseInsensitiveSupported;
    private boolean caseInsensitiveSupportedInitialized;

    @Override
    public EntityReference getDocumentById(long id) throws ConfluenceResolverException
    {
        return getDocument(Map.of(ID, id, STABLE_ID, id), false);
    }

    @Override
    public EntityReference getDocumentByTitle(String spaceKey, String title) throws ConfluenceResolverException
    {
        return getDocument(
            isCaseInsensitiveSupported()
                ? Map.of(SPACE_LOWER, spaceKey.toLowerCase(), TITLE_LOWER, title.toLowerCase())
                : Map.of(SPACE, spaceKey, TITLE, title),
            true);
    }

    private boolean isCaseInsensitiveSupported()
    {
        if (caseInsensitiveSupportedInitialized) {
            return caseInsensitiveSupported;
        }

        try {
            // SolrClientInstance.CORE_NAME = "search"
            SolrClient client = solr.getClient("search");
            if (client == null) {
                logger.warn("Could not get the Solr client to check whether case insensitive Solr fields exist");
                caseInsensitiveSupported = false;
            } else {
                new SchemaRequest.DynamicField("*_string_lowercase").process(client);
                caseInsensitiveSupported = true;
            }
        } catch (org.apache.solr.common.SolrException e) {
            caseInsensitiveSupported = false;
        } catch (SolrException | SolrServerException | IOException e) {
            logger.error("Could not check whether case insensitive Solr fields exist", e);
            return false;
        }

        caseInsensitiveSupportedInitialized = true;
        return caseInsensitiveSupported;
    }

    private EntityReference getDocument(Map<String, Object> values, boolean andOp)
        throws ConfluenceResolverException
    {
        // we use Solr search because it searches in all wikis. It is less comfortable than HQL because matches are not
        // exact so some extra work is needed to handle this fact of life.
        Collection<SolrDocument> results;
        String queryString = values.entrySet().stream()
            .map(entry -> CONFLUENCE_PROP + entry.getKey() + ':' + solrUtils.toFilterQueryString(entry.getValue()))
            .collect(Collectors.joining(andOp ? " AND " : " OR "));
        try {
            Query query = queryManager.createQuery("*", "solr")
                .bindValue("fq", "type:DOCUMENT AND (" + queryString + ")")
                .bindValue("sort", SORT_CREATIONDATE)
                .setLimit(1);
            results = ((QueryResponse) query.execute().get(0)).getResults();
        } catch (QueryException e) {
            throw new ConfluenceResolverException(e);
        }

        for (SolrDocument result : results) {
            XWikiContext context = contextProvider.get();
            XWiki wiki = context.getWiki();
            DocumentReference ref = solrDocumentReferenceResolver.resolve(result);
            try {
                if (wiki.exists(ref, context)) {
                    return ref;
                }
            } catch (XWikiException e) {
                throw new ConfluenceResolverException(e);
            }
        }

        return null;
    }

    @Override
    public EntityReference getSpaceByKey(String spaceKey) throws ConfluenceResolverException
    {
        EntityReference oldestInThisSpace = getDocument(
            isCaseInsensitiveSupported()
                ? Map.of(SPACE_LOWER, spaceKey.toLowerCase())
                : Map.of(SPACE, spaceKey),
            true);
        if (oldestInThisSpace == null) {
            return null;
        }

        return getSpace(oldestInThisSpace);
    }

    @Override
    public EntityReference getSpace(EntityReference reference) throws ConfluenceResolverException
    {
        return getSpace(reference, null);
    }

    private EntityReference getSpace(EntityReference reference, String expectedSpace) throws ConfluenceResolverException
    {
        String spaceKey = getSpaceKey(reference);
        if (StringUtils.isEmpty(spaceKey) || (expectedSpace != null && !spaceKey.equals(expectedSpace))) {
            return null;
        }

        EntityReference xwikiSpace = reference.getParent();
        if (xwikiSpace != null) {
            EntityReference newRef = new EntityReference("WebHome", EntityType.DOCUMENT, xwikiSpace.getParent());

            EntityReference newSpace = getSpace(newRef, spaceKey);
            if (newSpace != null) {
                return newSpace;
            }
        }

        return xwikiSpace;
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
                HQL).bindValue("fullname", fullName).setLimit(1);
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
