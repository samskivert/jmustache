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
        if (value.getClass().isArray()) {
            final ArrayHelper helper = arrayHelper(value);
            return new Iterator<Object>() {
                private int _count = helper.length(value), _idx;
                @Override public boolean hasNext () { return _idx < _count; }
                @Override public Object next () { return helper.get(value, _idx++); }
                @Override public void remove () { throw new UnsupportedOperationException(); }
            };
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
            if (ctx.getClass().isArray()) return arrayHelper(ctx);
        }

        return null;
    }

    /** This should return a thread-safe map, either {@link Collections#synchronizedMap} called on
     * a standard {@link Map} implementation or something like {@code ConcurrentHashMap}. */
    public abstract <K,V> Map<K,V> createFetcherCache ();

    protected static ArrayHelper arrayHelper (Object ctx) {
        if (ctx instanceof Object[]) return OBJECT_ARRAY_HELPER;
        if (ctx instanceof boolean[]) return BOOLEAN_ARRAY_HELPER;
        if (ctx instanceof byte[]) return BYTE_ARRAY_HELPER;
        if (ctx instanceof char[]) return CHAR_ARRAY_HELPER;
        if (ctx instanceof short[]) return SHORT_ARRAY_HELPER;
        if (ctx instanceof int[]) return INT_ARRAY_HELPER;
        if (ctx instanceof long[]) return LONG_ARRAY_HELPER;
        if (ctx instanceof float[]) return FLOAT_ARRAY_HELPER;
        if (ctx instanceof double[]) return DOUBLE_ARRAY_HELPER;
        return null;
    }

    protected static final Mustache.VariableFetcher MAP_FETCHER = new Mustache.VariableFetcher() {
        public Object get (Object ctx, String name) throws Exception {
            Map<?,?> map = (Map<?,?>)ctx;
            if (map.containsKey(name)) return map.get(name);
            // special case to allow map entry set to be iterated over
            if (name == "entrySet") return map.entrySet();
            return Template.NO_FETCHER_FOUND;
        }
        @Override public String toString () {
            return "MAP_FETCHER";
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
        @Override public String toString () {
            return "LIST_FETCHER";
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
        @Override public String toString () {
            return "ITER_FETCHER";
        }
    };

    protected static final Mustache.VariableFetcher THIS_FETCHER = new Mustache.VariableFetcher() {
        public Object get (Object ctx, String name) throws Exception {
            return ctx;
        }
        @Override public String toString () {
            return "THIS_FETCHER";
        }
    };

    protected static abstract class ArrayHelper implements Mustache.VariableFetcher {
        public Object get (Object ctx, String name) throws Exception {
            try {
                return get(ctx, Integer.parseInt(name));
            } catch (NumberFormatException nfe) {
                return Template.NO_FETCHER_FOUND;
            } catch (ArrayIndexOutOfBoundsException e) {
                return Template.NO_FETCHER_FOUND;
            }
        }
        public abstract int length (Object ctx);
        protected abstract Object get (Object ctx, int index);
    }

    protected static final ArrayHelper OBJECT_ARRAY_HELPER = new ArrayHelper() {
        @Override protected Object get (Object ctx, int index) { return ((Object[])ctx)[index]; }
        @Override public int length (Object ctx) { return ((Object[])ctx).length; }
    };
    protected static final ArrayHelper BOOLEAN_ARRAY_HELPER = new ArrayHelper() {
        @Override protected Object get (Object ctx, int index) { return ((boolean[])ctx)[index]; }
        @Override public int length (Object ctx) { return ((boolean[])ctx).length; }
    };
    protected static final ArrayHelper BYTE_ARRAY_HELPER = new ArrayHelper() {
        @Override protected Object get (Object ctx, int index) { return ((byte[])ctx)[index]; }
        @Override public int length (Object ctx) { return ((byte[])ctx).length; }
    };
    protected static final ArrayHelper CHAR_ARRAY_HELPER = new ArrayHelper() {
        @Override protected Object get (Object ctx, int index) { return ((char[])ctx)[index]; }
        @Override public int length (Object ctx) { return ((char[])ctx).length; }
    };
    protected static final ArrayHelper SHORT_ARRAY_HELPER = new ArrayHelper() {
        @Override protected Object get (Object ctx, int index) { return ((short[])ctx)[index]; }
        @Override public int length (Object ctx) { return ((short[])ctx).length; }
    };
    protected static final ArrayHelper INT_ARRAY_HELPER = new ArrayHelper() {
        @Override protected Object get (Object ctx, int index) { return ((int[])ctx)[index]; }
        @Override public int length (Object ctx) { return ((int[])ctx).length; }
    };
    protected static final ArrayHelper LONG_ARRAY_HELPER = new ArrayHelper() {
        @Override protected Object get (Object ctx, int index) { return ((long[])ctx)[index]; }
        @Override public int length (Object ctx) { return ((long[])ctx).length; }
    };
    protected static final ArrayHelper FLOAT_ARRAY_HELPER = new ArrayHelper() {
        @Override protected Object get (Object ctx, int index) { return ((float[])ctx)[index]; }
        @Override public int length (Object ctx) { return ((float[])ctx).length; }
    };
    protected static final ArrayHelper DOUBLE_ARRAY_HELPER = new ArrayHelper() {
        @Override protected Object get (Object ctx, int index) { return ((double[])ctx)[index]; }
        @Override public int length (Object ctx) { return ((double[])ctx).length; }
    };
}
