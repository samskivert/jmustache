//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache;

import java.io.IOException;
import java.io.UncheckedIOException;


/**
 * Defines some standard {@link Mustache.Escaper}s.
 *
 * REFACTORING:
 * - Removed direct HTML escaping responsibility
 * - Removed NONE escaper implementation
 * - Delegated escaping behavior to separate classes
 *
 * New classes:
 * HtmlEscaper.java
 * NoOpEscaper.java
 */
public class Escapers
{

    /*
     * ================================
     * CHANGE 1:
     *
     * BEFORE:
     *
     * HTML escaping logic was written
     * directly inside this class.
     *
     * AFTER:
     *
     * HTML escaping responsibility moved
     * to HtmlEscaper class.
     *
     * Reason:
     * Reduce responsibility of Escapers.java
     * and improve maintainability.
     * ================================
     */

    /** Escapes HTML entities. */
    public static final Mustache.Escaper HTML =
            new HtmlEscaper();



    /*
     * ================================
     * CHANGE 2:
     *
     * BEFORE:
     *
     * NONE was anonymous inner class:
     *
     * new Mustache.Escaper(){
     *      return text;
     * }
     *
     * AFTER:
     *
     * Separate NoOpEscaper class.
     *
     * Benefit:
     * Better readability and testing.
     * ================================
     */

    /** An escaper that does no escaping. */
    public static final Mustache.Escaper NONE =
            new NoOpEscaper();



    /*
     * ================================
     * UNCHANGED PART
     *
     * This method is kept because it is
     * a reusable factory method.
     *
     * It is also used by other parts
     * of the library.
     * ================================
     */

    /**
     * Returns an escaper that replaces a list
     * of text sequences with canned replacements.
     *
     * @param repls list of (text,replacement) pairs
     */
    public static Mustache.Escaper simple (
            final String[]... repls)
    {

        String[] lookupTable =
                Lookup7bitEscaper.createTable(repls);


        /*
         * Performance optimization:
         *
         * If replacements are ASCII based,
         * use fast lookup escaper.
         */
        if (lookupTable != null)
        {
            return new Lookup7bitEscaper(lookupTable);
        }


        /*
         * Fallback implementation
         * for non ASCII replacements.
         */
        return new Mustache.Escaper()
        {
            @Override
            public String escape(String text)
            {

                for(String[] escape : repls)
                {
                    text =
                    text.replace(
                            escape[0],
                            escape[1]);
                }

                return text;
            }
        };
    }



    /*
     * ================================
     *
     * ORIGINAL INTERNAL OPTIMIZATION
     *
     * No major change applied.
     *
     * Reason:
     * This class handles performance
     * optimization and is already
     * well-designed.
     *
     * ================================
     */


    private static class Lookup7bitEscaper
            implements Mustache.Escaper
    {


        private final String[] lookupTable;



        private Lookup7bitEscaper(
                String[] lookupTable)
        {
            this.lookupTable = lookupTable;
        }



        static String[] createTable(
                String[][] mappings)
        {

            String[] table =
                    new String[128];


            for(String[] entry : mappings)
            {

                String key =
                        entry[0];


                String value =
                        entry[1];


                if(key.length()!=1)
                {
                    return null;
                }


                char k =
                        key.charAt(0);


                if(k > 127)
                {
                    return null;
                }


                table[k]=value;
            }


            return table;
        }




        @Override
        public void escape(
                Appendable a,
                CharSequence raw)
                throws IOException
        {


            int end =
                    raw.length();


            for(
                int i=0,start=0;
                i<end;
                i++)
            {

                char c =
                        raw.charAt(i);


                String found =
                        escapeChar(
                                lookupTable,
                                c);



                if(found!=null)
                {

                    a.append(
                            raw,
                            0,
                            i);


                    a.append(found);


                    start =
                    i =
                    i+1;



                    for(;i<end;i++)
                    {

                        c =
                        raw.charAt(i);


                        found =
                        escapeChar(
                                lookupTable,
                                c);



                        if(found!=null)
                        {

                            a.append(
                                    raw,
                                    start,
                                    i);


                            a.append(found);


                            start =
                            i+1;
                        }
                    }


                    a.append(
                            raw,
                            start,
                            end);


                    return;
                }
            }


            a.append(raw);
        }



        private static String escapeChar(
                String[] lookupTable,
                char c)
        {

            if(c>127)
            {
                return null;
            }


            return lookupTable[c];
        }



        @Override
        public String escape(String raw)
        {

            StringBuilder sb =
                    new StringBuilder(
                            raw.length());


            try
            {
                escape(sb,raw);
            }
            catch(IOException e)
            {
                throw new UncheckedIOException(e);
            }


            return sb.toString();
        }



        @Override
        public String toString()
        {

            StringBuilder sb =
                    new StringBuilder();


            sb.append("Escaper[");


            for(char i=0;
                i<lookupTable.length;
                i++)
            {

                String value =
                        lookupTable[i];


                if(value!=null)
                {

                    sb.append("{'")
                    .append(i)
                    .append("', '")
                    .append(value)
                    .append("'},");
                }
            }


            sb.append("]");


            return sb.toString();
        }
    }
}