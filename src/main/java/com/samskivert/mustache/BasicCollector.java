//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A collector that does not use reflection and can be used with GWT.
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
        if (name == Template.DOT_NAME || name == Template.THIS_NAME) return THIS_FETCHER;

        if (ctx instanceof Map<?,?>) return MAP_FETCHER;

        // if the name looks like a number, potentially use one of our 'indexing' fetchers
        char c = name.charAt(0);
        if (c >= '0' && c <= '9') {
            if (ctx instanceof List<?>) return LIST_FETCHER;
            if (ctx instanceof Iterator<?>) return ITER_FETCHER;
            if (ctx instanceof Object[]) return OBJECT_ARRAY_FETCHER;
            if (ctx instanceof boolean[]) return BOOLEAN_ARRAY_FETCHER;
            if (ctx instanceof byte[]) return BYTE_ARRAY_FETCHER;
            if (ctx instanceof char[]) return CHAR_ARRAY_FETCHER;
            if (ctx instanceof short[]) return SHORT_ARRAY_FETCHER;
            if (ctx instanceof int[]) return INT_ARRAY_FETCHER;
            if (ctx instanceof long[]) return LONG_ARRAY_FETCHER;
            if (ctx instanceof float[]) return FLOAT_ARRAY_FETCHER;
            if (ctx instanceof double[]) return DOUBLE_ARRAY_FETCHER;
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

    protected static final Mustache.VariableFetcher LIST_FETCHER = new Mustache.VariableFetcher() {
        public Object get (Object ctx, String name) throws Exception {
            try {
                return ((List<?>)ctx).get(Integer.parseInt(name));
            } catch (NumberFormatException nfe) {
                return Template.NO_FETCHER_FOUND;
            } catch (IndexOutOfBoundsException e) {
                return Template.NO_FETCHER_FOUND;
            }
        }
    };

    protected static final Mustache.VariableFetcher ITER_FETCHER = new Mustache.VariableFetcher() {
        public Object get (Object ctx, String name) throws Exception {
            try {
                Iterator<?> iter = (Iterator<?>)ctx;
                for (int ii = 0, ll = Integer.parseInt(name); ii < ll; ii++) iter.next();
                return iter.next();
            } catch (NumberFormatException nfe) {
                return Template.NO_FETCHER_FOUND;
            } catch (NoSuchElementException e) {
                return Template.NO_FETCHER_FOUND;
            }
        }
    };

    protected static abstract class ArrayFetcher implements Mustache.VariableFetcher {
        public Object get (Object ctx, String name) throws Exception {
            try {
                return get(ctx, Integer.parseInt(name));
            } catch (NumberFormatException nfe) {
                return Template.NO_FETCHER_FOUND;
            } catch (ArrayIndexOutOfBoundsException e) {
                return Template.NO_FETCHER_FOUND;
            }
        }
        protected abstract Object get (Object ctx, int index);
    }

    protected static final ArrayFetcher OBJECT_ARRAY_FETCHER = new ArrayFetcher() {
        @Override protected Object get (Object ctx, int index) { return ((Object[])ctx)[index]; }
    };
    protected static final ArrayFetcher BOOLEAN_ARRAY_FETCHER = new ArrayFetcher() {
        @Override protected Object get (Object ctx, int index) { return ((boolean[])ctx)[index]; }
    };
    protected static final ArrayFetcher BYTE_ARRAY_FETCHER = new ArrayFetcher() {
        @Override protected Object get (Object ctx, int index) { return ((byte[])ctx)[index]; }
    };
    protected static final ArrayFetcher CHAR_ARRAY_FETCHER = new ArrayFetcher() {
        @Override protected Object get (Object ctx, int index) { return ((char[])ctx)[index]; }
    };
    protected static final ArrayFetcher SHORT_ARRAY_FETCHER = new ArrayFetcher() {
        @Override protected Object get (Object ctx, int index) { return ((short[])ctx)[index]; }
    };
    protected static final ArrayFetcher INT_ARRAY_FETCHER = new ArrayFetcher() {
        @Override protected Object get (Object ctx, int index) { return ((int[])ctx)[index]; }
    };
    protected static final ArrayFetcher LONG_ARRAY_FETCHER = new ArrayFetcher() {
        @Override protected Object get (Object ctx, int index) { return ((long[])ctx)[index]; }
    };
    protected static final ArrayFetcher FLOAT_ARRAY_FETCHER = new ArrayFetcher() {
        @Override protected Object get (Object ctx, int index) { return ((float[])ctx)[index]; }
    };
    protected static final ArrayFetcher DOUBLE_ARRAY_FETCHER = new ArrayFetcher() {
        @Override protected Object get (Object ctx, int index) { return ((double[])ctx)[index]; }
    };

    protected static final Mustache.VariableFetcher THIS_FETCHER = new Mustache.VariableFetcher() {
        public Object get (Object ctx, String name) throws Exception {
            return ctx;
        }
    };
}
