//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The default collector used by JMustache.
 */
public class DefaultCollector extends BasicCollector
{
    private final boolean _allowAccessCoercion;

    public DefaultCollector () {
        this(true);
    }

    public DefaultCollector (boolean allowAccessCoercion) {
        _allowAccessCoercion = allowAccessCoercion;
    }

    @Override
    public Mustache.VariableFetcher createFetcher (Object ctx, String name) {
        Mustache.VariableFetcher fetcher = super.createFetcher(ctx, name);
        if (fetcher != null) return fetcher;

        if (ctx == null || name == null || name.isEmpty()) return null;

        // first check for a getter which provides the value
        Class<?> cclass = ctx.getClass();
        final Method m = getMethod(cclass, name);
        if (m != null) {
            return createFetcher(m);
        }

        // next check for a getter which provides the value
        final Field f = getField(cclass, name);
        if (f != null) {
            return createFetcher(f);
        }

        // finally check for a default interface method which provides the value (this is left to
        // last because it's much more expensive and hopefully something already matched above)
        final Method im = getIfaceMethod(cclass, name);
        if (im != null) {
            return createFetcher(im);
        }

        return null;
    }

    private Mustache.VariableFetcher createFetcher (final Method method) {
        if (method == null) return null;
        return (ctx, name) -> {
            try {
                return method.invoke(ctx);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke method: " + method.getName(), e);
            }
        };
    }

    private Mustache.VariableFetcher createFetcher (final Field field) {
        if (field == null) return null;
        return (ctx, name) -> {
            try {
                return field.get(ctx);
            } catch (Exception e) {
                throw new RuntimeException("Failed to access field: " + field.getName(), e);
            }
        };
    }

    @Override
    public <K,V> Map<K,V> createFetcherCache () {
        return new ConcurrentHashMap<K,V>();
    }

    protected Method getMethod (Class<?> clazz, String name) {
        if (_allowAccessCoercion) {
            // first check up the superclass chain
            for (Class<?> cc = clazz; cc != null && cc != Object.class; cc = cc.getSuperclass()) {
                Method m = getMethodOn(cc, name);
                if (m != null) return m;
            }
        } else {
            // if we only allow access to accessible methods, then we can just let the JVM handle
            // searching superclasses for the method
            try {
                return clazz.getMethod(name);
            } catch (Exception e) {
                // fall through
            }
        }
        return null;
    }

    protected Method getIfaceMethod (Class<?> clazz, String name) {
        // enumerate the transitive closure of all interfaces implemented by clazz
        Set<Class<?>> ifaces = new LinkedHashSet<Class<?>>();
        for (Class<?> cc = clazz; cc != null && cc != Object.class; cc = cc.getSuperclass()) {
            addIfaces(ifaces, cc, false);
        }
        // now search those in the order that we found them
        for  (Class<?> iface : ifaces) {
            Method m = getMethodOn(iface, name);
            if (m != null) return m;
        }
        return null;
    }

    private void addIfaces (Set<Class<?>> ifaces, Class<?> clazz, boolean isIface) {
        if (isIface) ifaces.add(clazz);
        for (Class<?> iface : clazz.getInterfaces()) addIfaces(ifaces, iface, true);
    }

    protected Method getMethodOn (Class<?> clazz, String name) {
        Method m;
        try {
            m = clazz.getDeclaredMethod(name);
            if (!m.getReturnType().equals(void.class)) return makeAccessible(m);
        } catch (Exception e) {
            // fall through
        }

        String upperName = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        try {
            m = clazz.getDeclaredMethod("get" + upperName);
            if (!m.getReturnType().equals(void.class)) return makeAccessible(m);
        } catch (Exception e) {
            // fall through
        }

        try {
            m = clazz.getDeclaredMethod("is" + upperName);
            if (m.getReturnType().equals(boolean.class) ||
                m.getReturnType().equals(Boolean.class)) return makeAccessible(m);
        } catch (Exception e) {
            // fall through
        }

        return null;
    }

    private Method makeAccessible (Method m) {
        if (m == null) return null;
        if (!_allowAccessCoercion) return null;
        try {
            m.setAccessible(true);
            return m;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Field makeAccessible (Field f) {
        if (f == null) return null;
        if (!_allowAccessCoercion) return null;
        try {
            f.setAccessible(true);
            return f;
        } catch (RuntimeException e) {
            return null;
        }
    }

    protected Field getField (Class<?> clazz, String name) {
        if (!_allowAccessCoercion) {
            try {
                return clazz.getField(name);
            } catch (Exception e) {
                return null;
            }
        }

        Field f;
        try {
            f = clazz.getDeclaredField(name);
            return makeAccessible(f);
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
