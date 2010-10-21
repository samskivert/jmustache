//
// $Id$

package com.samskivert.mustache;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a compiled template. Templates are executed with a <em>context</em> to generate
 * output. The context can be any tree of objects. Variables are resolved against the context.
 * Given a name {@code foo}, the following mechanisms are supported for resolving its value
 * (and are sought in this order):
 * <ul>
 * <li>If the object is a {@link Map}, {@link Map#get} will be called with the string {@code foo}
 * as the key.
 * <li>A method named {@code foo} in the supplied object (with non-void return value).
 * <li>A method named {@code getFoo} in the supplied object (with non-void return value).
 * <li>A field named {@code foo} in the supplied object.
 * </ul>
 * <p> The field type, method return type, or map value type should correspond to the desired
 * behavior if the resolved name corresponds to a section. {@link Boolean} is used for showing or
 * hiding sections without binding a sub-context. Arrays, {@link Iterator} and {@link Iterable}
 * implementations are used for sections that repeat, with the context bound to the elements of the
 * array, iterator or iterable. Lambdas are current unsupported, though they would be easy enough
 * to add if desire exists. See the <a href="http://mustache.github.com/mustache.5.html">Mustache
 * documentation</a> for more details on section behavior. </p>
 */
public class Template
{
    /**
     * Executes this template with the given context, writing the results to the supplied writer.
     * @throws MustacheException if an error occurs while executing or writing the template.
     */
    public void execute (Object context, Writer out) throws MustacheException
    {
        for (Segment seg : _segs) {
            seg.execute(this, context, out);
        }
    }

    /**
     * Executes this template with the given context, returning the results as a string.
     * @throws MustacheException if an error occurs while executing or writing the template.
     */
    public String execute (Object context) throws MustacheException
    {
        StringWriter out = new StringWriter();
        execute(context, out);
        return out.toString();
    }

    protected Template (Segment[] segs)
    {
        _segs = segs;
    }

    /**
     * Called by executing segments to obtain the value of the specified variable in the supplied
     * context.
     */
    protected Object getValue (Object ctx, String name)
    {
        if (ctx == null) {
            throw new NullPointerException("Null context for variable '" + name + "'");
        }

        Key key = new Key(ctx.getClass(), name);
        VariableFetcher fetcher = _fcache.get(key);
        if (fetcher != null) {
            try {
                return fetcher.get(ctx, name);
            } catch (Exception e) {
                // zoiks! non-monomorphic call site, update the cache and try again
                _fcache.put(key, fetcher = createFetcher(key));
            }
        } else {
            fetcher = createFetcher(key);
        }

        try {
            Object value = fetcher.get(ctx, name);
            _fcache.put(key, fetcher);
            return value;
        } catch (Exception e) {
            throw new MustacheException("Failure fetching variable '" + name + "'", e);
        }
    }

    protected final Segment[] _segs;
    protected final Map<Key, VariableFetcher> _fcache =
        new ConcurrentHashMap<Key, VariableFetcher>();

    protected static VariableFetcher createFetcher (Key key)
    {
        if (Map.class.isAssignableFrom(key.cclass)) {
            return MAP_FETCHER;
        }

        final Method m = getMethod(key.cclass, key.name);
        if (m != null) {
            return new VariableFetcher() {
                public Object get (Object ctx, String name) throws Exception {
                    return m.invoke(ctx);
                }
            };
        }

        final Field f = getField(key.cclass, key.name);
        if (f != null) {
            return new VariableFetcher() {
                public Object get (Object ctx, String name) throws Exception {
                    return f.get(ctx);
                }
            };
        }

        throw new MustacheException("No method or field with appropriate name and not Map " +
                                    "[var=" + key.name + ", ctx=" + key.cclass.getName() + "]");
    }

    protected static Method getMethod (Class<?> clazz, String name)
    {
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

    protected static Field getField (Class<?> clazz, String name)
    {
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

    /** A template is broken into segments. */
    protected static abstract class Segment
    {
        abstract void execute (Template tmpl, Object ctx, Writer out);

        protected static void write (Writer out, String data) {
            try {
                out.write(data);
            } catch (IOException ioe) {
                throw new MustacheException(ioe);
            }
        }
    }

    /** Used to cache variable fetchers for a given context class, name combination. */
    protected static class Key
    {
        public final Class<?> cclass;
        public final String name;

        public Key (Class<?> cclass, String name) {
            this.cclass = cclass;
            this.name = name.intern();
        }

        @Override public int hashCode () {
            return cclass.hashCode() * 31 + name.hashCode();
        }

        @Override public boolean equals (Object other) {
            Key okey = (Key)other;
            return okey.cclass == cclass && okey.name == name;
        }
    }

    protected static abstract class VariableFetcher {
        abstract Object get (Object ctx, String name) throws Exception;
    }

    protected static final VariableFetcher MAP_FETCHER = new VariableFetcher() {
        public Object get (Object ctx, String name) throws Exception {
            return ((Map<?,?>)ctx).get(name);
        }
    };
}
