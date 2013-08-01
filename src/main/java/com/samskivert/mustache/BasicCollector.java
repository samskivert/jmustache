//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * A collector that does not use reflection and can be used in GWT.
 */
public abstract class BasicCollector implements Mustache.Collector
{
    public Iterator<?> toIterator (final Object value) {
        if (value instanceof Iterable<?>) {
            return ((Iterable<?>)value).iterator();
        }
        if (value instanceof Iterator<?>) {
            return (Iterator<?>)value;
        }
        return null;
    }

    public Mustache.VariableFetcher createFetcher (Object ctx, String name) {
        // support both .name and this.name to fetch members
        if (name == Template.DOT_NAME || name == Template.THIS_NAME) {
            return THIS_FETCHER;
        }

        if (ctx instanceof Map<?,?>) {
            return MAP_FETCHER;
        }

        return null;
    }

    /** This should return a thread-safe map, either {@link Collections#synchronizedMap} called on
     * a standard {@link Map} implementation or something like {@code ConcurrentHashMap}. */
    public abstract <K,V> Map<K,V> createFetcherCache ();

    protected static final Mustache.VariableFetcher MAP_FETCHER = new Mustache.VariableFetcher() {
        public Object get (Object ctx, String name) throws Exception {
            Map<?,?> map = (Map<?,?>)ctx;
            return map.containsKey(name) ? map.get(name) : Template.NO_FETCHER_FOUND;
        }
    };

    protected static final Mustache.VariableFetcher THIS_FETCHER = new Mustache.VariableFetcher() {
        public Object get (Object ctx, String name) throws Exception {
            return ctx;
        }
    };
}
