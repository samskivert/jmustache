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
 *
 * REFACTORING CHANGES:
 *
 * 1. Extract Method Refactoring
 *    - createFetcher() was doing multiple responsibilities.
 *    - Logic separated into smaller methods.
 *
 * 2. Duplicate Code Removal
 *    - Method, Field and Interface fetcher creation
 *      had repeated anonymous classes.
 *    - Common helper methods introduced.
 */
public class DefaultCollector extends BasicCollector
{


    private final boolean _allowAccessCoercion;



    public DefaultCollector ()
    {
        this(true);
    }



    public DefaultCollector (boolean allowAccessCoercion)
    {
        _allowAccessCoercion = allowAccessCoercion;
    }




    /*
     * =====================================================
     *
     * CHANGE 1:
     *
     * BEFORE:
     *
     * createFetcher() handled:
     *
     * - BasicCollector search
     * - Getter search
     * - Field search
     * - Interface search
     *
     * One method had multiple responsibilities.
     *
     *
     * AFTER:
     *
     * Responsibilities extracted into:
     *
     * findGetterFetcher()
     * findFieldFetcher()
     * findInterfaceFetcher()
     *
     * =====================================================
     */

    @Override
    public Mustache.VariableFetcher createFetcher (
            Object ctx,
            String name)
    {


        // First check parent collector
        Mustache.VariableFetcher fetcher =
                super.createFetcher(ctx,name);


        if(fetcher != null)
        {
            return fetcher;
        }



        // Second check getter method
        fetcher =
                findGetterFetcher(ctx,name);


        if(fetcher != null)
        {
            return fetcher;
        }




        // Third check field access
        fetcher =
                findFieldFetcher(ctx,name);


        if(fetcher != null)
        {
            return fetcher;
        }




        // Last check interface default method
        return findInterfaceFetcher(ctx,name);
    }






    /*
     * =====================================================
     *
     * CHANGE 2:
     *
     * Extracted getter searching logic.
     *
     * Previous code was inside createFetcher()
     *
     * =====================================================
     */


    private Mustache.VariableFetcher findGetterFetcher(
            Object ctx,
            String name)
    {

        Class<?> clazz =
                ctx.getClass();


        final Method method =
                getMethod(clazz,name);



        if(method == null)
        {
            return null;
        }



        return createMethodFetcher(method);
    }






    /*
     * =====================================================
     *
     * CHANGE 3:
     *
     * Extracted field searching logic.
     *
     * =====================================================
     */


    private Mustache.VariableFetcher findFieldFetcher(
            Object ctx,
            String name)
    {


        Class<?> clazz =
                ctx.getClass();



        final Field field =
                getField(clazz,name);



        if(field == null)
        {
            return null;
        }



        return createFieldFetcher(field);
    }






    /*
     * =====================================================
     *
     * CHANGE 4:
     *
     * Extracted interface method searching logic.
     *
     * =====================================================
     */


    private Mustache.VariableFetcher findInterfaceFetcher(
            Object ctx,
            String name)
    {


        Class<?> clazz =
                ctx.getClass();



        final Method method =
                getIfaceMethod(clazz,name);



        if(method == null)
        {
            return null;
        }



        return createMethodFetcher(method);
    }







    /*
     * =====================================================
     *
     * CHANGE 5:
     *
     * Duplicate anonymous VariableFetcher removed.
     *
     * Previously:
     *
     * new Mustache.VariableFetcher()
     * {
     *     return method.invoke(ctx);
     * }
     *
     * existed multiple times.
     *
     * Now one reusable method.
     *
     * =====================================================
     */


    private Mustache.VariableFetcher createMethodFetcher(
            final Method method)
    {


        return new Mustache.VariableFetcher()
        {

            public Object get(
                    Object ctx,
                    String name)
                    throws Exception
            {

                return method.invoke(ctx);
            }
        };
    }






    private Mustache.VariableFetcher createFieldFetcher(
            final Field field)
    {


        return new Mustache.VariableFetcher()
        {

            public Object get(
                    Object ctx,
                    String name)
                    throws Exception
            {

                return field.get(ctx);
            }
        };
    }







    @Override
    public <K,V> Map<K,V> createFetcherCache ()
    {

        return new ConcurrentHashMap<K,V>();
    }







    protected Method getMethod (
            Class<?> clazz,
            String name)
    {


        if(_allowAccessCoercion)
        {


            for(
                Class<?> cc = clazz;
                cc != null && cc != Object.class;
                cc = cc.getSuperclass())
            {


                Method method =
                        getMethodOn(cc,name);


                if(method != null)
                {
                    return method;
                }
            }


        }
        else
        {

            try
            {
                return clazz.getMethod(name);
            }
            catch(Exception e)
            {

            }
        }


        return null;
    }







    protected Method getIfaceMethod(
            Class<?> clazz,
            String name)
    {


        Set<Class<?>> interfaces =
                new LinkedHashSet<Class<?>>();



        for(
            Class<?> cc = clazz;
            cc != null && cc != Object.class;
            cc = cc.getSuperclass())
        {

            addIfaces(
                    interfaces,
                    cc,
                    false);
        }




        for(Class<?> iface : interfaces)
        {

            Method method =
                    getMethodOn(
                            iface,
                            name);


            if(method != null)
            {
                return method;
            }
        }


        return null;
    }







    private void addIfaces(
            Set<Class<?>> ifaces,
            Class<?> clazz,
            boolean isIface)
    {


        if(isIface)
        {
            ifaces.add(clazz);
        }



        for(Class<?> iface : clazz.getInterfaces())
        {

            addIfaces(
                    ifaces,
                    iface,
                    true);
        }
    }







    protected Method getMethodOn(
            Class<?> clazz,
            String name)
    {


        Method method;


        try
        {

            method =
                    clazz.getDeclaredMethod(name);


            if(!method.getReturnType()
                    .equals(void.class))
            {

                return makeAccessible(method);
            }

        }
        catch(Exception e)
        {

        }




        String upperName =
                Character.toUpperCase(
                        name.charAt(0))
                +
                name.substring(1);




        try
        {

            method =
                    clazz.getDeclaredMethod(
                            "get"+upperName);



            if(!method.getReturnType()
                    .equals(void.class))
            {

                return makeAccessible(method);
            }

        }
        catch(Exception e)
        {

        }





        try
        {

            method =
                    clazz.getDeclaredMethod(
                            "is"+upperName);



            if(method.getReturnType()
                    .equals(boolean.class)
                    ||
               method.getReturnType()
                    .equals(Boolean.class))
            {

                return makeAccessible(method);
            }

        }
        catch(Exception e)
        {

        }


        return null;
    }







    private Method makeAccessible(Method method)
    {


        if(method.isAccessible())
        {
            return method;
        }


        else if(!_allowAccessCoercion)
        {
            return null;
        }


        method.setAccessible(true);


        return method;
    }







    protected Field getField(
            Class<?> clazz,
            String name)
    {


        if(!_allowAccessCoercion)
        {

            try
            {
                return clazz.getField(name);
            }

            catch(Exception e)
            {
                return null;
            }

        }




        Field field;


        try
        {

            field =
                    clazz.getDeclaredField(name);



            if(!field.isAccessible())
            {
                field.setAccessible(true);
            }


            return field;

        }
        catch(Exception e)
        {

        }





        Class<?> parent =
                clazz.getSuperclass();



        if(parent != Object.class
                &&
           parent != null)
        {

            return getField(
                    parent,
                    name);
        }



        return null;
    }

}