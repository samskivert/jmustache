//
// $Id$

package com.samskivert.mustache;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A collector used when running in GWT.
 */
public class DefaultCollector extends BasicCollector
{
    @Override
    public Iterator<?> toIterator (final Object value) {
        Iterator<?> iter = super.toIterator(value);
        if (iter != null) return iter;

        if (value.getClass().isArray()) {
            return Arrays.asList((Object[])value).iterator();
        }
        return null;
    }

    @Override
    public <K,V> Map<K,V> createFetcherCache () {
        return new HashMap<K,V>();
    }

    // TODO: override createFetcher and do some magic for JavaScript/JSON objects
}
