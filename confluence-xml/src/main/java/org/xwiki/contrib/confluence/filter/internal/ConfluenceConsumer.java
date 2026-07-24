package org.xwiki.contrib.confluence.filter.internal;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.xwiki.filter.FilterException;

@FunctionalInterface
public interface ConfluenceConsumer
{
    void accept(ConfluenceObjectReader objectReader) throws ConfigurationException, FilterException;
}
