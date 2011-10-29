//
// $Id$

package com.samskivert.mustache;

import java.util.Iterator;
import java.util.Map;

/**
 * A collector that does not use reflection and can be used in GWT.
 */
public class BasicCollector implements Mustache.Collector
{
    @Override
    public Iterator<?> toIterator (final Object value) {
        if (value instanceof Iterable<?>) {
            return ((Iterable<?>)value).iterator();
        }
        if (value instanceof Iterator<?>) {
            return (Iterator<?>)value;
        }
        return null;
    }

    @Override
    public Mustache.VariableFetcher createFetcher (Object ctx, String name)
    {
        // support both .name and this.name to fetch members
        if (name == Template.DOT_NAME || name == Template.THIS_NAME) {
            return THIS_FETCHER;
        }

        if (ctx instanceof Map<?,?>) {
            return MAP_FETCHER;
        }

        return null;
    }

    protected static final Mustache.VariableFetcher MAP_FETCHER = new Mustache.VariableFetcher() {
        @Override public Object get (Object ctx, String name) throws Exception {
            return ((Map<?,?>)ctx).get(name);
        }
    };

    protected static final Mustache.VariableFetcher THIS_FETCHER = new Mustache.VariableFetcher() {
        @Override public Object get (Object ctx, String name) throws Exception {
            return ctx;
        }
    };
}
