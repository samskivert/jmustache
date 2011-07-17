//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a compiled template. Templates are executed with a <em>context</em> to generate
 * output. The context can be any tree of objects. Variables are resolved against the context.
 * Given a name {@code foo}, the following mechanisms are supported for resolving its value
 * (and are sought in this order):
 * <ul>
 * <li>If the variable has the special name {@code this} the context object itself will be
 * returned. This is useful when iterating over lists.
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
     * Executes this template with the given context, returning the results as a string.
     * @throws MustacheException if an error occurs while executing or writing the template.
     */
    public String execute (Object context) throws MustacheException
    {
        StringWriter out = new StringWriter();
        execute(context, out);
        return out.toString();
    }

    /**
     * Executes this template with the given context, writing the results to the supplied writer.
     * @throws MustacheException if an error occurs while executing or writing the template.
     */
    public void execute (Object context, Writer out) throws MustacheException
    {
        executeSegs(new Context(context, null, 0, Mode.OTHER), out);
    }

    protected Template (Segment[] segs, Mustache.Compiler compiler)
    {
        _segs = segs;
        _compiler = compiler;
    }

    protected void executeSegs (Context ctx, Writer out) throws MustacheException
    {
        for (Segment seg : _segs) {
            seg.execute(this, ctx, out);
        }
    }

    /**
     * Called by executing segments to obtain the value of the specified variable in the supplied
     * context.
     *
     * @param ctx the context in which to look up the variable.
     * @param name the name of the variable to be resolved, which must be an interned string.
     * @param missingIsNull whether to fail if a variable cannot be resolved, or to return null in
     * that case.
     *
     * @return the value associated with the supplied name or null if no value could be resolved.
     */
    protected Object getValue (Context ctx, String name, int line, boolean missingIsNull)
    {
        if (!_compiler.standardsMode) {
            // if we're dealing with a compound key, resolve each component and use the result to
            // resolve the subsequent component and so forth
            if (name != DOT_NAME && name.indexOf(DOT_NAME) != -1) {
                String[] comps = name.split("\\.");
                // we want to allow the first component of a compound key to be located in a parent
                // context, but once we're selecting sub-components, they must only be resolved in the
                // object that represents that component
                Object data = getValue(ctx, comps[0].intern(), line, missingIsNull);
                for (int ii = 1; ii < comps.length; ii++) {
                    if (data == NO_FETCHER_FOUND) {
                        throw new NullPointerException(
                            "Missing context for compound variable '" + name + "' on line " + line +
                            ". '" + comps[ii - 1] + "' was not found.");
                    } else if (data == null) {
                        throw new NullPointerException(
                            "Null context for compound variable '" + name + "' on line " + line +
                            ". '" + comps[ii - 1] + "' resolved to null.");
                    }
                    // once we step into a composite key, we drop the ability to query our parent
                    // contexts; that would be weird and confusing
                    data = getValueIn(data, comps[ii].intern(), line);
                }
                return checkForMissing(name, line, missingIsNull, data);
            }
        }

        // handle our special variables
        if (name == FIRST_NAME) {
            return ctx.mode == Mode.FIRST;
        } else if (name == LAST_NAME) {
            return ctx.mode == Mode.LAST;
        } else if (name == INDEX_NAME) {
            return ctx.index;
        }

        // if we're in standards mode, we don't search our parent contexts
        if (_compiler.standardsMode) {
            return checkForMissing(name, line, missingIsNull, getValueIn(ctx.data, name, line));
        }

        boolean variableMissing = true;
        while (ctx != null) {
            Object value = getValueIn(ctx.data, name, line);
            if (value == NO_FETCHER_FOUND) {
                // preserve variableMissing
            } else if (value == null) {
                // we found a fetcher, and it returned null; so we keep searching our parents, but
                // we won't freak out about a missing variable if we have a nullValue configured
                variableMissing = false;
            } else {
                return value;
            }
            ctx = ctx.parent;
        }
        // we've popped off the top of our stack of contexts, if we never actually found a fetcher
        // for our variable, we need to let checkForMissing() know
        return checkForMissing(name, line, missingIsNull, variableMissing ? NO_FETCHER_FOUND : null);
    }

    /**
     * Returns the value of the specified variable, noting that it is intended to be used as the
     * contents for a segment. Presently this does not do anything special, but eventually this
     * will be the means by which we enact configured behavior for sections that reference null or
     * missing variables. Right now, all such variables result in a length 0 section.
     */
    protected Object getSectionValue (Context ctx, String name, int line)
    {
        // TODO: configurable behavior on missing values
        Object value = getValue(ctx, name, line, _compiler.missingIsNull);
        // TODO: configurable behavior on null values
        return (value == null) ? Collections.emptyList() : value;
    }

    /**
     * Returns the value for the specified variable, or the configured default value if the
     * variable resolves to null. See {@link #getValue}.
     */
    protected Object getValueOrDefault (Context ctx, String name, int line)
    {
        Object value = getValue(ctx, name, line, _compiler.missingIsNull);
        // getValue will raise MustacheException if a variable cannot be resolved and missingIsNull
        // is not configured; so we're safe to assume that any null that makes it up to this point
        // can be converted to nullValue
        return (value == null) ? _compiler.nullValue : value;
    }

    protected Object getValueIn (Object data, String name, int line)
    {
        if (data == null) {
            throw new NullPointerException(
                "Null context for variable '" + name + "' on line " + line);
        }

        Key key = new Key(data.getClass(), name);
        VariableFetcher fetcher = _fcache.get(key);
        if (fetcher != null) {
            try {
                return fetcher.get(data, name);
            } catch (Exception e) {
                // zoiks! non-monomorphic call site, update the cache and try again
                fetcher = createFetcher(key);
            }
        } else {
            fetcher = createFetcher(key);
        }

        // if we were unable to create a fetcher, just return null and our caller can either try
        // the parent context, or do le freak out
        if (fetcher == null) {
            return NO_FETCHER_FOUND;
        }

        try {
            Object value = fetcher.get(data, name);
            _fcache.put(key, fetcher);
            return value;
        } catch (Exception e) {
            throw new MustacheException(
                "Failure fetching variable '" + name + "' on line " + line, e);
        }
    }

    protected Object checkForMissing (String name, int line, boolean missingIsNull, Object value)
    {
        if (value == NO_FETCHER_FOUND) {
            if (missingIsNull) {
                return null;
            } else {
                throw new MustacheException(
                    "No method or field with name '" + name + "' on line " + line);
            }
        } else {
            return value;
        }
    }

    protected final Segment[] _segs;
    protected final Mustache.Compiler _compiler;
    protected final Map<Key, VariableFetcher> _fcache =
        new ConcurrentHashMap<Key, VariableFetcher>();

    protected static VariableFetcher createFetcher (Key key)
    {
        // support both .name and this.name to fetch members
        if (key.name == DOT_NAME || key.name == THIS_NAME) {
            return THIS_FETCHER;
        }

        if (Map.class.isAssignableFrom(key.cclass)) {
            return MAP_FETCHER;
        }

        final Method m = getMethod(key.cclass, key.name);
        if (m != null) {
            return new VariableFetcher() {
                @Override
                public Object get (Object ctx, String name) throws Exception {
                    return m.invoke(ctx);
                }
            };
        }

        final Field f = getField(key.cclass, key.name);
        if (f != null) {
            return new VariableFetcher() {
                @Override
                public Object get (Object ctx, String name) throws Exception {
                    return f.get(ctx);
                }
            };
        }

        return null;
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

    protected static enum Mode { FIRST, OTHER, LAST };

    protected static class Context
    {
        public final Object data;
        public final Context parent;
        public final int index;
        public final Mode mode;

        public Context (Object data, Context parent, int index, Mode mode) {
            this.data = data;
            this.parent = parent;
            this.index = index;
            this.mode = mode;
        }

        public Context nest (Object data, int index, Mode mode) {
            return new Context(data, this, index, mode);
        }
    }

    /** A template is broken into segments. */
    protected static abstract class Segment
    {
        abstract void execute (Template tmpl, Context ctx, Writer out);

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
            this.name = name;
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
        @Override
        public Object get (Object ctx, String name) throws Exception {
            return ((Map<?,?>)ctx).get(name);
        }
    };

    protected static final VariableFetcher THIS_FETCHER = new VariableFetcher() {
        @Override
        public Object get (Object ctx, String name) throws Exception {
            return ctx;
        }
    };

    protected static final Object NO_FETCHER_FOUND = new Object();

    protected static final String DOT_NAME = ".".intern();
    protected static final String THIS_NAME = "this".intern();
    protected static final String FIRST_NAME = "-first".intern();
    protected static final String LAST_NAME = "-last".intern();
    protected static final String INDEX_NAME = "-index".intern();
}
