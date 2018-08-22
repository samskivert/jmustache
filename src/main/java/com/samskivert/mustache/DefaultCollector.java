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
    @Override
    public Mustache.VariableFetcher createFetcher (Object ctx, String name) {
        Mustache.VariableFetcher fetcher = super.createFetcher(ctx, name);
        if (fetcher != null) return fetcher;

        // first check for a getter which provides the value
        Class<?> cclass = ctx.getClass();
        final Method m = getMethod(cclass, name);
        if (m != null) {
            return new Mustache.VariableFetcher() {
                public Object get (Object ctx, String name) throws Exception {
                    return m.invoke(ctx);
                }
            };
        }

        // next check for a getter which provides the value
        final Field f = getField(cclass, name);
        if (f != null) {
            return new Mustache.VariableFetcher() {
                public Object get (Object ctx, String name) throws Exception {
                    return f.get(ctx);
                }
            };
        }

        // finally check for a default interface method which provides the value (this is left to
        // last because it's much more expensive and hopefully something already matched above)
        final Method im = getIfaceMethod(cclass, name);
        if (im != null) {
            return new Mustache.VariableFetcher() {
                public Object get (Object ctx, String name) throws Exception {
                    return im.invoke(ctx);
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
        // first check up the superclass chain
        for (Class<?> cc = clazz; cc != null && cc != Object.class; cc = cc.getSuperclass()) {
            Method m = getMethodOn(cc, name);
            if (m != null) return m;
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
        if (!m.isAccessible()) m.setAccessible(true);
        return m;
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
