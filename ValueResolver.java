
//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE
//

package com.samskivert.mustache;

import java.util.Map;


/**
 * Handles variable resolution for templates.
 *
 * Extracted from Template to reduce Template class complexity.
 *
 * Responsibilities:
 *
 * - Resolve variables
 * - Search parent contexts
 * - Resolve compound variables
 * - Handle missing values
 * - Use fetcher cache
 */
class ValueResolver {


    private final Mustache.Compiler compiler;

    private final Map<Key, Mustache.VariableFetcher> fetcherCache;



    ValueResolver(
        Mustache.Compiler compiler,
        Map<Key, Mustache.VariableFetcher> fetcherCache
    ) {

        this.compiler = compiler;
        this.fetcherCache = fetcherCache;
    }





    Object getValue(
        Context ctx,
        String name,
        int line,
        boolean missingIsNull
    ) {


        // special variables

        if(name.equals(Template.FIRST_NAME)) {
            return ctx.onFirst;
        }


        if(name.equals(Template.LAST_NAME)) {
            return ctx.onLast;
        }


        if(name.equals(Template.INDEX_NAME)) {
            return ctx.index;
        }





        // standards mode

        if(compiler.standardsMode) {


            Object value =
                getValueIn(
                    ctx.data,
                    name,
                    line
                );


            return checkForMissing(
                name,
                line,
                missingIsNull,
                value
            );
        }





        // search current and parent contexts

        for(Context pctx = ctx;
            pctx != null;
            pctx = pctx.parent) {


            Object value =
                getValueIn(
                    pctx.data,
                    name,
                    line
                );


            if(value != Template.NO_FETCHER_FOUND) {

                return value;
            }
        }






        // compound key

        if(!Template.DOT_NAME.equals(name)
           && name.indexOf(Template.DOT_NAME) != -1) {


            return getCompoundValue(
                ctx,
                name,
                line,
                missingIsNull
            );
        }





        return checkForMissing(
            name,
            line,
            missingIsNull,
            Template.NO_FETCHER_FOUND
        );
    }







    private Object getCompoundValue(
        Context ctx,
        String name,
        int line,
        boolean missingIsNull
    ) {


        String[] components =
            name.split("\\.");



        Object data =
            getValue(
                ctx,
                components[0],
                line,
                missingIsNull
            );




        for(int i = 1;
            i < components.length;
            i++) {



            if(data == Template.NO_FETCHER_FOUND) {


                if(!missingIsNull) {


                    throw new MustacheException.Context(

                        "Missing context for compound variable '"
                        + name
                        + "' on line "
                        + line
                        + ". '"
                        + components[i - 1]
                        + "' was not found.",

                        name,
                        line
                    );
                }


                return null;
            }




            if(data == null) {

                return null;
            }





            data =
                getValueIn(
                    data,
                    components[i],
                    line
                );
        }




        return checkForMissing(
            name,
            line,
            missingIsNull,
            data
        );
    }









    Object getValueIn(
        Object data,
        String name,
        int line
    ) {



        if(Template.isThisName(name)) {

            return data;
        }





        if(data == null) {


            throw new NullPointerException(

                "Null context for variable '"
                + name
                + "' on line "
                + line
            );
        }





        Key key =
            new Key(
                data.getClass(),
                name
            );





        Mustache.VariableFetcher fetcher =
            fetcherCache.get(key);






        if(fetcher != null) {


            try {


                return fetcher.get(
                    data,
                    name
                );


            } catch(Exception e) {


                fetcher =
                    compiler.collector.createFetcher(
                        data,
                        key.name
                    );
            }


        } else {


            fetcher =
                compiler.collector.createFetcher(
                    data,
                    key.name
                );
        }






        if(fetcher == null) {


            fetcher =
                Template.NOT_FOUND_FETCHER;
        }






        try {


            Object value =
                fetcher.get(
                    data,
                    name
                );



            fetcherCache.put(
                key,
                fetcher
            );



            return value;



        } catch(Exception e) {


            throw new MustacheException.Context(

                "Failure fetching variable '"
                + name
                + "' on line "
                + line,

                name,
                line,
                e
            );
        }
    }








    private Object checkForMissing(
        String name,
        int line,
        boolean missingIsNull,
        Object value
    ) {



        if(value == Template.NO_FETCHER_FOUND) {


            if(missingIsNull) {

                return null;
            }




            throw new MustacheException.Context(

                "No method or field with name '"
                + name
                + "' on line "
                + line,

                name,
                line
            );
        }




        return value;
    }
}