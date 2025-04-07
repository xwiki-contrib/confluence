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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverter;
import org.xwiki.model.reference.EntityReferenceSerializer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimally convert the rw-pagetree macro to a standard pagetree.
 * See <a href="https://help.refined.com/space/CONFDC/4704255589/Page+Tree+macro">Refined Page Tree</a>
 *
 * @version $Id$
 * @since 9.56.0
 */
@Component
@Named("rw-pagetree")
@Singleton
public class RwPagetreeMacroConverter extends AbstractMacroConverter
{
    private static final String ROOT = "root";

    @Inject
    private ConfluenceConverter confluenceConverter;

    @Inject
    private Logger logger;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return "documentTree";
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.NO;
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> parameters = new HashMap<>();
        String root = confluenceParameters.get(ROOT);
        if (StringUtils.isEmpty(root)) {
            String space = confluenceParameters.get("space");
            if (StringUtils.isNotEmpty(space)) {
                parameters.put(ROOT, confluenceConverter.convertSpaceReference(space));
            }
        } else {
            try {
                long rootId = Long.parseLong(root);
                String ref = confluenceConverter.convertDocumentReference(rootId);
                if (ref == null) {
                    parameters.put("confluencerootpageid", Long.toString(rootId));
                } else {
                    parameters.put(ROOT, "document:" + ref);
                }
            } catch (NumberFormatException e) {
                logger.warn("Could not convert the root parameter of the rw-pagetree macro");
            }
        }

        return parameters;
    }
}
