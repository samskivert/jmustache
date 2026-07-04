//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE
//

package com.samskivert.mustache;


/**
 * Cache key used for storing variable fetchers.
 *
 * A fetcher is cached based on:
 *
 * 1. Object class
 * 2. Variable name
 */
class Key {


    public final Class<?> cclass;

    public final String name;



    Key(
        Class<?> cclass,
        String name
    ) {

        this.cclass = cclass;
        this.name = name;
    }



    @Override
    public int hashCode() {

        return cclass.hashCode() * 31
            + name.hashCode();
    }



    @Override
    public boolean equals(Object other) {


        if(!(other instanceof Key)) {
            return false;
        }


        Key okey = (Key) other;


        return okey.cclass == cclass
            && okey.name.equals(name);
    }



    @Override
    public String toString() {

        return cclass.getName()
            + ":"
            + name;
    }
}
