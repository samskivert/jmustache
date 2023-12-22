//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Defines some standard {@link Mustache.Escaper}s.
 */
public class Escapers
{
    // TODO for 2.0 this class should be final and have a private constructor but that
    // would break semver on the off chance someone did extend it.

    /** Escapes HTML entities. */
    public static final Mustache.Escaper HTML = simple(new String[][] {
        { "&",  "&amp;" },
        { "'",  "&#39;" },
        { "\"", "&quot;" },
        { "<",  "&lt;" },
        { ">",  "&gt;" },
        { "`",  "&#x60;" },
        { "=",  "&#x3D;" }
    });

    /** An escaper that does no escaping. */
    public static final Mustache.Escaper NONE = new Mustache.Escaper() {
        @Override public String escape (String text) {
            return text;
        }
    };

    /** Returns an escaper that replaces a list of text sequences with canned replacements.
     * @param repls a list of {@code (text, replacement)} pairs. */
    public static Mustache.Escaper simple (final String[]... repls) {
        String[] lookupTable = Lookup7bitEscaper.createTable(repls);
        if (lookupTable != null) {
            return new Lookup7bitEscaper(lookupTable);
        }
        // our lookup replacements are not 7 bit ascii.
        return new Mustache.Escaper() {
            @Override public String escape (String text) {
                for (String[] escape : repls) {
                    text = text.replace(escape[0], escape[1]);
                }
                return text;
            }
        };
    }
    // This is based on benchmarking: https://github.com/jstachio/escape-benchmark
    private static class Lookup7bitEscaper implements Mustache.Escaper {
        /*
         * This only works for replacing the lower 7 bit ascii
         * characters
         */
        private final String[] lookupTable;

        private Lookup7bitEscaper(
                String[] lookupTable) {
            super();
            this.lookupTable = lookupTable;
        }

        static /* @Nullable */ String[] createTable (String[][] mappings) {
            String[] table = new String[128];
            for (String[] entry : mappings) {
                String key = entry[0];
                String value = entry[1];
                if (key.length() != 1) {
                    return null;
                }
                char k = key.charAt(0);
                if (k > 127) {
                    return null;
                }
                table[k] = value;
            }
            return table;
        }

        @Override
        public void escape (Appendable a, CharSequence raw) throws IOException {
            int end = raw.length();
            for (int i = 0, start = 0; i < end; i++) {
                char c = raw.charAt(i);
                String found = escapeChar(lookupTable, c);
                /*
                 * While this could be done with one loop it appears through
                 * benchmarking that by having the first loop assume the string
                 * to be not changed creates a fast path for strings with no escaping needed.
                 */
                if (found != null) {
                    a.append(raw, 0, i);
                    a.append(found);
                    start = i = i + 1;
                    for (; i < end; i++) {
                        c = raw.charAt(i);
                        found = escapeChar(lookupTable, c);
                        if (found != null) {
                            a.append(raw, start, i);
                            a.append(found);
                            start = i + 1;
                        }
                    }
                    a.append(raw, start, end);
                    return;
                }
            }
            a.append(raw);
        }

        private static /* @Nullable */ String escapeChar (String[] lookupTable, char c) {
            if (c > 127) {
                return null;
            }
            return lookupTable[c];
        }

        @Override
        public String escape (String raw) {
            StringBuilder sb = new StringBuilder(raw.length());
            try {
                escape(sb, raw);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return sb.toString();
        }

        @Override
        public String toString () {
            StringBuilder sb = new StringBuilder();
            sb.append("Escaper[");
            for(char i = 0; i < lookupTable.length; i++) {
                String value = lookupTable[i];
                if (value != null) {
                    sb.append("{'").append(i).append("', '").append(value).append("'},");
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
