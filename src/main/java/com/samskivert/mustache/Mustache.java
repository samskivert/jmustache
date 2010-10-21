//
// $Id$

package com.samskivert.mustache;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Provides <a href="http://mustache.github.com/">Mustache</a> templating services.
 * <p> Limitations:
 * <ul><li> Only one or two character delimiters are supported when using {{=ab cd=}} to change
 * delimiters.
 * <li> {{< include}} is not supported. We specifically do not want the complexity of handling the
 * automatic loading of dependent templates. </ul>
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
    public static Template compile (Reader source)
    {
        // a hand-rolled parser; whee!
        Accumulator accum = new Accumulator();
        char start1 = '{', start2 = '{', end1 = '}', end2 = '}';
        int state = TEXT, startPos = 0, endPos = 0;
        StringBuilder text = new StringBuilder();
        int line = 0;

        while (true) {
            char c;
            try {
                int v = source.read();
                if (v == -1) {
                    break;
                }
                c = (char)v;
            } catch (IOException e) {
                throw new MustacheException(e);
            }

            if (c == '\n') {
                line++;
            }

            switch (state) {
            case TEXT:
                if (c == start1) {
                    if (start2 == -1) {
                        accum.addTextSegment(text);
                        state = TAG;
                    } else {
                        state = MATCHING_START;
                    }
                } else {
                    text.append(c);
                }
                break;

            case MATCHING_START:
                if (c == start2) {
                    accum.addTextSegment(text);
                    state = TAG;
                } else {
                    text.append(start1);
                    if (c != start1) {
                        state = TEXT;
                    }
                }
                break;

            case TAG:
                if (c == end1) {
                    if (end2 == -1) {
                        if (text.charAt(0) == '=') {
                            // TODO: change delimiters
                        } else {
                            accum = accum.addTagSegment(text, line);
                        }
                        state = TEXT;
                    } else {
                        state = MATCHING_END;
                    }
                } else {
                    text.append(c);
                }
                break;

            case MATCHING_END:
                if (c == end2) {
                    if (text.charAt(0) == '=') {
                        // TODO: change delimiters
                    } else {
                        accum = accum.addTagSegment(text, line);
                    }
                    state = TEXT;
                } else {
                    text.append(end1);
                    if (c != end1) {
                        state = TAG;
                    }
                }
                break;
            }
        }

        // accumulate any trailing text
        switch (state) {
        case TEXT:
            accum.addTextSegment(text);
            break;
        case MATCHING_START:
            text.append(start1);
            accum.addTextSegment(text);
            break;
        case MATCHING_END:
            text.append(end1);
            accum.addTextSegment(text);
            break;
        case TAG:
            throw new MustacheException("Template ended while parsing a tag TODO");
        }

        return new Template(accum.finish());
    }

    private Mustache () {} // no instantiateski

    protected static String escapeHTML (String text)
    {
        for (int ii = 0; ii < ATTR_ESCAPES.length; ii++) {
            text = text.replace(ATTR_ESCAPES[ii][0], ATTR_ESCAPES[ii][1]);
        }
        return text;
    }

    protected static final int TEXT = 0;
    protected static final int MATCHING_START = 1;
    protected static final int MATCHING_END = 2;
    protected static final int TAG = 3;

    protected static class Accumulator {
        public void addTextSegment (StringBuilder text) {
            if (text.length() > 0) {
                _segs.add(new StringSegment(text.toString()));
                text.setLength(0);
            }
        }

        public Accumulator addTagSegment (StringBuilder accum, final int line) {
            final Accumulator outer = this;
            String tag = accum.toString().trim();
            final String tag1 = tag.substring(1).trim();
            accum.setLength(0);

            switch (tag.charAt(0)) {
            case '#':
                requireNoNewlines(tag, line);
                return new Accumulator() {
                    public Template.Segment[] finish () {
                        throw new MustacheException("Section missing close tag " +
                                                    "[line=" + line + ", name=" + tag1 + "]");
                    }
                    protected Accumulator addCloseSectionSegment (String itag, int line) {
                        requireSameName(tag1, itag, line);
                        outer._segs.add(new SectionSegment(itag, super.finish()));
                        return outer;
                    }
                };

            case '^':
                requireNoNewlines(tag, line);
                return new Accumulator() {
                    public Template.Segment[] finish () {
                        throw new MustacheException("Inverted section missing close tag " +
                                                    "[line=" + line + ", name=" + tag1 + "]");
                    }
                    protected Accumulator addCloseSectionSegment (String itag, int line) {
                        requireSameName(tag1, itag, line);
                        outer._segs.add(new InvertedSectionSegment(itag, super.finish()));
                        return outer;
                    }
                };

            case '/':
                requireNoNewlines(tag, line);
                return addCloseSectionSegment(tag1, line);

            case '!':
                // comment!, ignore
                return this;

            case '&':
                requireNoNewlines(tag, line);
                _segs.add(new VariableSegment(tag1, false));
                return this;

            default:
                requireNoNewlines(tag, line);
                _segs.add(new VariableSegment(tag, true));
                return this;
            }
        }

        public Template.Segment[] finish () {
            return _segs.toArray(new Template.Segment[_segs.size()]);
        }

        protected Accumulator addCloseSectionSegment (String tag, int line) {
            throw new MustacheException("Section close tag with no open tag " +
                                        "[line=" + line + ", name=" + tag + "]");
        }

        protected static void requireNoNewlines (String tag, int line) {
            if (tag.indexOf("\n") != -1 || tag.indexOf("\r") != -1) {
                throw new MustacheException("Invalid tag name: contains newlne " +
                                            "[line=" + line + ", name=" + tag + "]");
            }
        }

        protected static void requireSameName (String name1, String name2, int line)
        {
            if (!name1.equals(name2)) {
                throw new MustacheException(
                    "Section close tag with mismatched open tag " +
                    "[line=" + line + ", expected=" + name1 + ", got=" + name2 + "]");
            }
        }

        protected final List<Template.Segment> _segs = new ArrayList<Template.Segment>();
    }

    /** A simple segment that reproduces a string. */
    protected static class StringSegment extends Template.Segment {
        public StringSegment (String text) {
            _text = text;
        }
        @Override public void execute (Template tmpl, Object ctx, Writer out) {
            write(out, _text);
        }
        protected final String _text;
    }

    /** A helper class for named segments. */
    protected static abstract class NamedSegment extends Template.Segment {
        protected NamedSegment (String name) {
            _name = name;
        }
        protected final String _name;
    }

    /** A segment that substitutes the contents of a variable. */
    protected static class VariableSegment extends NamedSegment {
        public VariableSegment (String name, boolean escapeHTML) {
            super(name);
            _escapeHTML = escapeHTML;
        }
        @Override public void execute (Template tmpl, Object ctx, Writer out)  {
            Object value = tmpl.getValue(ctx, _name);
            // TODO: configurable behavior on missing values
            if (value != null) {
                String text = String.valueOf(value);
                write(out, _escapeHTML ? escapeHTML(text) : text);
            }
        }
        protected boolean _escapeHTML;
    }

    /** A helper class for compound segments. */
    protected static abstract class CompoundSegment extends NamedSegment {
        protected CompoundSegment (String name, Template.Segment[] segs) {
            super(name);
            _segs = segs;
        }
        protected void executeSegs (Template tmpl, Object ctx, Writer out)  {
            for (Template.Segment seg : _segs) {
                seg.execute(tmpl, ctx, out);
            }
        }
        protected final Template.Segment[] _segs;
    }

    /** A segment that represents a section. */
    protected static class SectionSegment extends CompoundSegment {
        public SectionSegment (String name, Template.Segment[] segs) {
            super(name, segs);
        }
        @Override public void execute (Template tmpl, Object ctx, Writer out)  {
            Object value = tmpl.getValue(ctx, _name);
            if (value == null) {
                return; // TODO: configurable behavior on missing values
            }
            if (value instanceof Iterable<?>) {
                Iterable<?> iable = (Iterable<?>)value;
                for (Object elem : iable) {
                    executeSegs(tmpl, elem, out);
                }
            } else if (value instanceof Boolean) {
                if ((Boolean)value) {
                    executeSegs(tmpl, ctx, out);
                }
            } else if (value.getClass().isArray()) {
                for (int ii = 0, ll = Array.getLength(value); ii < ll; ii++) {
                    executeSegs(tmpl, Array.get(value, ii), out);
                }
            } else if (value instanceof Iterator<?>) {
                Iterator<?> iter = (Iterator<?>)value;
                while (iter.hasNext()) {
                    executeSegs(tmpl, iter.next(), out);
                }
            } else {
                executeSegs(tmpl, value, out);
            }
        }
    }

    /** A segment that represents an inverted section. */
    protected static class InvertedSectionSegment extends CompoundSegment {
        public InvertedSectionSegment (String name, Template.Segment[] segs) {
            super(name, segs);
        }
        @Override public void execute (Template tmpl, Object ctx, Writer out)  {
            Object value = tmpl.getValue(ctx, _name);
            if (value == null) {
                executeSegs(tmpl, ctx, out); // TODO: configurable behavior on missing values
            }
            if (value instanceof Iterable<?>) {
                Iterable<?> iable = (Iterable<?>)value;
                if (!iable.iterator().hasNext()) {
                    executeSegs(tmpl, ctx, out);
                }
            } else if (value instanceof Boolean) {
                if (!(Boolean)value) {
                    executeSegs(tmpl, ctx, out);
                }
            } else if (value.getClass().isArray()) {
                if (Array.getLength(value) == 0) {
                    executeSegs(tmpl, ctx, out);
                }
            } else if (value instanceof Iterator<?>) {
                Iterator<?> iter = (Iterator<?>)value;
                if (!iter.hasNext()) {
                    executeSegs(tmpl, ctx, out);
                }
            }
        }
    }

    /** Map of strings that must be replaced inside html attributes and their replacements. (They
     * need to be applied in order so amps are not double escaped.) */
    protected static final String[][] ATTR_ESCAPES = {
        { "&", "&amp;" },
        { "'", "&apos;" },
        { "\"", "&quot;" },
        { "<", "&lt;" },
        { ">", "&gt;" },
    };
}
