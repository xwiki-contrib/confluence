package org.xwiki.contrib.confluence.filter.internal;

import org.xwiki.contrib.confluence.filter.input.ConfluenceProperties;
import org.xwiki.filter.FilterException;

public interface ConfluenceObjectReader
{
    Object readObjectProperties(ConfluenceProperties properties, ConfluenceObjectFields fields) throws FilterException;
}
