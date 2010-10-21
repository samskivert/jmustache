//
// $Id$

package com.samskivert.mustache;

import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;

/**
 * Provides <a href="http://mustache.github.com/">Mustache</a> templating services.
 */
public class Mustache
{
    /**
     * Compiles the supplied template into a repeatedly executable intermediate form.
     */
    public static Template compile (String template)
    {
        return compile(new StringReader(template));
    }

    /**
     * Compiles the supplied template into a repeatedly executable intermediate form.
     */
    public static Template compile (Reader template)
    {
        return null; // TODO
    }

    private Mustache () {} // no instantiateski

    /** A simple segment that reproduces a string. */
    protected static class StringSegment extends Template.Segment {
        public StringSegment (String text) {
            _text = text;
        }

        @Override public void execute (Object ctx, Writer out) {
            write(out, _text);
        }

        protected final String _text;
    }

    /** A segment that substitutes the contents of a variable. */
    protected static class VariableSegment extends Template.Segment {
        public VariableSegment (String name) {
            _name = name;
        }

        @Override public void execute (Object ctx, Writer out)  {
            Object value = getValue(ctx, _name);
            // TODO: configurable behavior on missing values
            if (value != null) {
                write(out, String.valueOf(value));
            }
        }

        protected final String _name;
    }

    /** A segment that represents a section. */
    protected static class SectionSegment extends Template.Segment {
        public SectionSegment (String name, Template.Segment[] segs) {
            _name = name;
            _segs = segs;
        }

        @Override public void execute (Object ctx, Writer out)  {
            Object value = getValue(ctx, _name);
            if (value == null) {
                return; // TODO: configurable behavior on missing values
            }
            if (value instanceof Iterable<?>) {
                Iterable<?> iable = (Iterable<?>)value;
                for (Object elem : iable) {
                    executeSegs(elem, out);
                }
            } else if (value instanceof Boolean) {
                if ((Boolean)value) {
                    executeSegs(ctx, out);
                }
            } else if (value.getClass().isArray()) {
                for (int ii = 0, ll = Array.getLength(value); ii < ll; ii++) {
                    executeSegs(Array.get(value, ii), out);
                }
            } else if (value instanceof Iterator<?>) {
                Iterator<?> iter = (Iterator<?>)value;
                while (iter.hasNext()) {
                    executeSegs(iter.next(), out);
                }
            } else {
                executeSegs(value, out);
            }
        }

        protected void executeSegs (Object ctx, Writer out)  {
            for (Template.Segment seg : _segs) {
                seg.execute(ctx, out);
            }
        }

        protected String _name;
        protected Template.Segment[] _segs;
    }
}
