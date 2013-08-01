//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The default collector used by JMustache.
 */
public class DefaultCollector extends BasicCollector
{
    @Override
    public Iterator<?> toIterator (final Object value) {
        Iterator<?> iter = super.toIterator(value);
        if (iter != null) return iter;

        if (value.getClass().isArray()) {
            return new AbstractList<Object>() {
                public int size () {
                    return Array.getLength(value);
                }
                public Object get (int idx) {
                    return Array.get(value, idx);
                }
            }.iterator();
        }
        return null;
    }

    @Override
    public Mustache.VariableFetcher createFetcher (Object ctx, String name) {
        Mustache.VariableFetcher fetcher = super.createFetcher(ctx, name);
        if (fetcher != null) return fetcher;

        Class<?> cclass = ctx.getClass();
        final Method m = getMethod(cclass, name);
        if (m != null) {
            return new Mustache.VariableFetcher() {
                public Object get (Object ctx, String name) throws Exception {
                    return m.invoke(ctx);
                }
            };
        }

        final Field f = getField(cclass, name);
        if (f != null) {
            return new Mustache.VariableFetcher() {
                public Object get (Object ctx, String name) throws Exception {
                    return f.get(ctx);
                }
            };
        }

        return null;
    }

    @Override
    public <K,V> Map<K,V> createFetcherCache () {
        return new ConcurrentHashMap<K,V>();
    }

    protected Method getMethod (Class<?> clazz, String name) {
        Method m;
        try {
            m = clazz.getDeclaredMethod(name);
            if (!m.getReturnType().equals(void.class)) {
                if (!m.isAccessible()) {
                    m.setAccessible(true);
                }
                return m;
            }
        } catch (Exception e) {
            // fall through
        }
        try {
            m = clazz.getDeclaredMethod(
                "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
            if (!m.getReturnType().equals(void.class)) {
                if (!m.isAccessible()) {
                    m.setAccessible(true);
                }
                return m;
            }
        } catch (Exception e) {
            // fall through
        }

        Class<?> sclass = clazz.getSuperclass();
        if (sclass != Object.class && sclass != null) {
            return getMethod(clazz.getSuperclass(), name);
        }
        return null;
    }

    protected Field getField (Class<?> clazz, String name) {
        Field f;
        try {
            f = clazz.getDeclaredField(name);
            if (!f.isAccessible()) {
                f.setAccessible(true);
            }
            return f;
        } catch (Exception e) {
            // fall through
        }

        Class<?> sclass = clazz.getSuperclass();
        if (sclass != Object.class && sclass != null) {
            return getField(clazz.getSuperclass(), name);
        }
        return null;
    }
}
