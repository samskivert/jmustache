//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE
//

package com.samskivert.mustache;


/**
 * Represents the execution context of a template.
 *
 * Stores current data object and parent context information.
 */
class Context {

    public final Object data;
    public final Context parent;

    public final int index;

    public final boolean onFirst;
    public final boolean onLast;



    Context(
        Object data,
        Context parent,
        int index,
        boolean onFirst,
        boolean onLast
    ) {

        this.data = data;
        this.parent = parent;
        this.index = index;
        this.onFirst = onFirst;
        this.onLast = onLast;
    }



    Context nest(Object data) {

        return new Context(
            data,
            this,
            index,
            onFirst,
            onLast
        );
    }



    Context nest(
        Object data,
        int index,
        boolean onFirst,
        boolean onLast
    ) {

        return new Context(
            data,
            this,
            index,
            onFirst,
            onLast
        );
    }
}
