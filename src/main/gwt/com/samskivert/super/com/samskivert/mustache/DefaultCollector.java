//
// $Id$

package com.samskivert.mustache;

import java.util.HashMap;
import java.util.Map;

/**
 * A collector used when running in GWT.
 */
public class DefaultCollector extends BasicCollector
{
    @Override
    public <K,V> Map<K,V> createFetcherCache () {
        return new HashMap<K,V>();
    }

    // TODO: override createFetcher and do some magic for JavaScript/JSON objects
}
