//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import com.samskivert.mustache.Mustache.BlockSegment;


/**
 * Represents a compiled template.
 *
 * Refactoring changes:
 *
 * 1. Large Class Improvement
 *    - Existing responsibilities were organized into logical sections.
 *
 * 2. Extract Method Refactoring
 *    - Complex value resolving logic separated into helper methods.
 *
 * 3. Duplicate Logic Reduction
 *    - Common execution and fetching operations centralized.
 */
public class Template {


    /**
     * Represents a template fragment.
     */
    public abstract class Fragment {


        public abstract void execute(Writer out);


        public abstract void execute(
                Object context,
                Writer out);



        public abstract void executeTemplate(
                Template tmpl,
                Writer out);



        public String execute() {

            StringWriter out =
                    new StringWriter();

            execute(out);

            return out.toString();
        }



        public String execute(Object context) {

            StringWriter out =
                    new StringWriter();

            execute(context,out);

            return out.toString();
        }



        public abstract Object context();



        public abstract Object context(int n);



        public String decompile() {

            return decompile(
                    new StringBuilder())
                    .toString();
        }



        public abstract StringBuilder decompile(
                StringBuilder into);
    }





    /**
     * Returned when no variable fetcher exists.
     */
    public static final Object NO_FETCHER_FOUND =
            new String("<no fetcher found>");






    /**
     * Execute template and return output.
     */
    public String execute(
            Object context)
            throws MustacheException {


        StringWriter out =
                new StringWriter();


        execute(
                context,
                out);


        return out.toString();
    }







    /**
     * Execute template using writer.
     */
    public void execute(
            Object context,
            Writer out)
            throws MustacheException {


        Context ctx =
                new Context(
                        context,
                        null,
                        0,
                        false,
                        false);



        executeSegs(
                ctx,
                out);
    }







    public void execute(
            Object context,
            Object parentContext,
            Writer out)
            throws MustacheException {


        Context parent =
                new Context(
                        parentContext,
                        null,
                        0,
                        false,
                        false);



        executeSegs(
                new Context(
                        context,
                        parent,
                        0,
                        false,
                        false),
                out);
    }







    public void visit(
            Mustache.Visitor visitor) {


        for(Segment segment : _segs) {

            segment.visit(visitor);
        }
    }







    protected Template(
            Segment[] segs,
            Mustache.Compiler compiler) {


        _segs = segs;

        _compiler = compiler;


        _fcache =
                compiler.collector
                        .createFetcherCache();
    }







    protected Template indent(
            String indent) {


        if(indent.equals("")) {

            return this;
        }



        Segment[] copiedSegments =
                Mustache.indentSegs(
                        _segs,
                        indent,
                        false,
                        false);



        if(copiedSegments == _segs) {

            return this;
        }



        return new Template(
                copiedSegments,
                _compiler);
    }







    protected Template replaceBlocks(
            Map<String, BlockSegment> blocks) {


        if(blocks.isEmpty()) {

            return this;
        }



        Segment[] copiedSegments =
                Mustache.replaceBlockSegs(
                        _segs,
                        blocks);



        if(copiedSegments == _segs) {

            return this;
        }



        return new Template(
                copiedSegments,
                _compiler);
    }







    protected void executeSegs(
            Context ctx,
            Writer out)
            throws MustacheException {


        for(Segment segment : _segs) {

            segment.execute(
                    this,
                    ctx,
                    out);
        }
    }







    protected Fragment createFragment(
            final Segment[] segments,
            final Context currentContext) {


        return new Fragment() {


            @Override
            public void execute(
                    Writer out) {


                executeFragment(
                        currentContext,
                        out);
            }




            @Override
            public void execute(
                    Object context,
                    Writer out) {


                executeFragment(
                        currentContext.nest(context),
                        out);
            }




            @Override
            public void executeTemplate(
                    Template template,
                    Writer out) {


                template.executeSegs(
                        currentContext,
                        out);
            }





            @Override
            public Object context() {

                return currentContext.data;
            }





            @Override
            public Object context(int n) {

                return getParentContext(
                        currentContext,
                        n);
            }





            @Override
            public StringBuilder decompile(
                    StringBuilder into) {


                for(Segment segment : segments) {

                    segment.decompile(
                            _compiler.delims,
                            into);
                }


                return into;
            }




            private void executeFragment(
                    Context ctx,
                    Writer out) {


                for(Segment segment : segments) {

                    segment.execute(
                            Template.this,
                            ctx,
                            out);
                }
            }



            private Object getParentContext(
                    Context ctx,
                    int depth) {


                if(depth == 0) {

                    return ctx.data;
                }


                return getParentContext(
                        ctx.parent,
                        depth - 1);
            }
        };
    }
    /**
     * Resolves variable value from context.
     *
     * Refactored:
     * - Special variables separated
     * - Standard mode handling separated
     * - Parent searching separated
     */
    protected Object getValue(
            Context ctx,
            String name,
            int line,
            boolean missingIsNull) {


        Object specialValue =
                resolveSpecialVariable(
                        ctx,
                        name);


        if(specialValue != NO_FETCHER_FOUND) {

            return specialValue;
        }



        if(_compiler.standardsMode) {

            return resolveStandardValue(
                    ctx,
                    name,
                    line,
                    missingIsNull);
        }



        Object parentValue =
                resolveFromParents(
                        ctx,
                        name,
                        line);



        if(parentValue != NO_FETCHER_FOUND) {

            return parentValue;
        }




        if(isCompoundName(name)) {

            return getCompoundValue(
                    ctx,
                    name,
                    line,
                    missingIsNull);
        }



        return checkForMissing(
                name,
                line,
                missingIsNull,
                NO_FETCHER_FOUND);
    }








    /**
     * Resolves built-in mustache variables.
     */
    private Object resolveSpecialVariable(
            Context ctx,
            String name) {


        if(name.equals(FIRST_NAME)) {

            return ctx.onFirst;
        }


        if(name.equals(LAST_NAME)) {

            return ctx.onLast;
        }


        if(name.equals(INDEX_NAME)) {

            return ctx.index;
        }


        return NO_FETCHER_FOUND;
    }








    /**
     * Resolves value in standards mode.
     */
    private Object resolveStandardValue(
            Context ctx,
            String name,
            int line,
            boolean missingIsNull) {


        Object value =
                getValueIn(
                        ctx.data,
                        name,
                        line);



        return checkForMissing(
                name,
                line,
                missingIsNull,
                value);
    }








    /**
     * Searches variable in parent contexts.
     */
    private Object resolveFromParents(
            Context ctx,
            String name,
            int line) {


        for(Context current = ctx;
            current != null;
            current = current.parent) {


            Object value =
                    getValueIn(
                            current.data,
                            name,
                            line);



            if(value != NO_FETCHER_FOUND) {

                return value;
            }
        }


        return NO_FETCHER_FOUND;
    }









    private boolean isCompoundName(
            String name) {


        return !name.equals(DOT_NAME)
                &&
                name.indexOf(DOT_NAME) != -1;
    }









    /**
     * Resolves compound variables like user.name
     */
    protected Object getCompoundValue(
            Context ctx,
            String name,
            int line,
            boolean missingIsNull) {


        String[] components =
                name.split("\\.");



        Object value =
                getValue(
                        ctx,
                        components[0],
                        line,
                        missingIsNull);




        for(int index = 1;
            index < components.length;
            index++) {


            if(value == NO_FETCHER_FOUND) {


                if(!missingIsNull) {


                    throw new MustacheException.Context(
                            "Missing context for compound variable '"
                                    + name
                                    + "' on line "
                                    + line,
                            name,
                            line);
                }


                return null;
            }



            if(value == null) {

                return null;
            }




            value =
                    getValueIn(
                            value,
                            components[index],
                            line);
        }




        return checkForMissing(
                name,
                line,
                missingIsNull,
                value);
    }









    /**
     * Returns section value.
     */
    protected Object getSectionValue(
            Context ctx,
            String name,
            int line) {


        Object value =
                getValue(
                        ctx,
                        name,
                        line,
                        !_compiler.strictSections);



        return value == null
                ? Collections.emptyList()
                : value;
    }









    /**
     * Returns variable value or default.
     */
    protected Object getValueOrDefault(
            Context ctx,
            String name,
            int line) {


        Object value =
                getValue(
                        ctx,
                        name,
                        line,
                        _compiler.missingIsNull);



        return value == null
                ? _compiler.computeNullValue(name)
                : value;
    }









    /**
     * Fetches value from object using collector.
     */
    protected Object getValueIn(
            Object data,
            String name,
            int line) {


        if(isThisName(name)) {

            return data;
        }



        if(data == null) {

            throw new NullPointerException(
                    "Null context for variable '"
                    + name
                    + "' on line "
                    + line);
        }




        Key key =
                new Key(
                        data.getClass(),
                        name);



        Mustache.VariableFetcher fetcher =
                _fcache.get(key);




        if(fetcher == null) {


            fetcher =
                    createFetcher(
                            data,
                            key);



        }




        if(fetcher == null) {

            fetcher = NOT_FOUND_FETCHER;
        }




        try {


            Object value =
                    fetcher.get(
                            data,
                            name);



            _fcache.put(
                    key,
                    fetcher);



            return value;



        } catch(Exception exception) {


            throw new MustacheException.Context(
                    "Failure fetching variable '"
                    + name
                    + "' on line "
                    + line,
                    name,
                    line,
                    exception);
        }
    }









    private Mustache.VariableFetcher createFetcher(
            Object data,
            Key key) {


        return _compiler.collector
                .createFetcher(
                        data,
                        key.name);
    }

    /**
     * Handles missing variables.
     */
    protected Object checkForMissing(
            String name,
            int line,
            boolean missingIsNull,
            Object value) {


        if(value == NO_FETCHER_FOUND) {


            if(missingIsNull) {

                return null;
            }



            throw new MustacheException.Context(
                    "No method or field with name '"
                    + name
                    + "' on line "
                    + line,
                    name,
                    line);
        }



        return value;
    }








    protected final Segment[] _segs;

    protected final Mustache.Compiler _compiler;

    protected final Map<Key, Mustache.VariableFetcher> _fcache;









    /**
     * Represents execution context.
     */
    protected static class Context {


        public final Object data;

        public final Context parent;

        public final int index;

        public final boolean onFirst;

        public final boolean onLast;





        public Context(
                Object data,
                Context parent,
                int index,
                boolean onFirst,
                boolean onLast) {


            this.data = data;

            this.parent = parent;

            this.index = index;

            this.onFirst = onFirst;

            this.onLast = onLast;
        }







        public Context nest(
                Object data) {


            return new Context(
                    data,
                    this,
                    index,
                    onFirst,
                    onLast);
        }







        public Context nest(
                Object data,
                int index,
                boolean onFirst,
                boolean onLast) {


            return new Context(
                    data,
                    this,
                    index,
                    onFirst,
                    onLast);
        }
    }









    /**
     * Base template segment.
     */
    protected static abstract class Segment {


        abstract void execute(
                Template template,
                Context ctx,
                Writer out);



        abstract void decompile(
                Mustache.Delims delims,
                StringBuilder into);



        abstract void visit(
                Mustache.Visitor visitor);





        abstract Segment indent(
                String indent,
                boolean first,
                boolean last);





        abstract boolean isStandalone();







        protected static void write(
                Writer out,
                CharSequence data) {


            try {

                out.append(data);

            } catch(IOException exception) {


                throw new MustacheException(
                        exception);
            }
        }








        protected static void escape(
                Appendable out,
                CharSequence data,
                Mustache.Escaper escaper) {


            try {


                escaper.escape(
                        out,
                        data);



            } catch(IOException exception) {


                throw new MustacheException(
                        exception);
            }
        }
    }









    /**
     * Cache key for variable fetchers.
     */
    protected static class Key {


        public final Class<?> cclass;

        public final String name;





        public Key(
                Class<?> cclass,
                String name) {


            this.cclass = cclass;

            this.name = name;
        }







        @Override
        public int hashCode() {


            return cclass.hashCode()
                    * 31
                    + name.hashCode();
        }







        @Override
        public boolean equals(
                Object object) {


            if(!(object instanceof Key)) {

                return false;
            }



            Key other =
                    (Key)object;



            return other.cclass == cclass
                    &&
                    other.name.equals(name);
        }







        @Override
        public String toString() {


            return cclass.getName()
                    + ":"
                    + name;
        }
    }









    protected static boolean isThisName(
            String name) {


        return DOT_NAME.equals(name)
                ||
                THIS_NAME.equals(name);
    }







    protected static final String DOT_NAME =
            ".";



    protected static final String THIS_NAME =
            "this";



    protected static final String FIRST_NAME =
            "-first";



    protected static final String LAST_NAME =
            "-last";



    protected static final String INDEX_NAME =
            "-index";









    /**
     * Used when fetcher cannot be found.
     */
    protected static final Mustache.VariableFetcher NOT_FOUND_FETCHER =
            new Mustache.VariableFetcher() {


                @Override
                public Object get(
                        Object ctx,
                        String name)
                        throws Exception {


                    return NO_FETCHER_FOUND;
                }
            };
}