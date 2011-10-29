//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Provides <a href="http://mustache.github.com/">Mustache</a> templating services.
 *
 * <p> Basic usage:
 * <pre>{@code
 * String source = "Hello {{arg}}!";
 * Template tmpl = Mustache.compiler().compile(source);
 * Map<String, Object> context = new HashMap<String, Object>();
 * context.put("arg", "world");
 * tmpl.execute(context); // returns "Hello world!"
 * }</pre>
 */
public class Mustache
{
    /** An interface to the Mustache compilation process. See {@link Mustache}. */
    public static class Compiler
    {
        /** Whether or not HTML entities are escaped by default. */
        public final boolean escapeHTML;

        /** Whether or not standards mode is enabled. */
        public final boolean standardsMode;

        /** A value to use when a variable resolves to null. If this value is null (which is the
         * default null value), an exception will be thrown. If {@link #missingIsNull} is also
         * true, this value will be used when a variable cannot be resolved. */
        public final String nullValue;

        /** If this value is true, missing variables will be treated like variables that return
         * null. {@link #nullValue} will be used in their place, assuming {@link #nullValue} is
         * configured to a non-null value. */
        public final boolean missingIsNull;

        /** The template loader in use during this compilation. */
        public final TemplateLoader loader;

        /** The collector used by templates compiled with this compiler. */
        public final Collector collector;

        /** Compiles the supplied template into a repeatedly executable intermediate form. */
        public Template compile (String template) {
            return compile(new StringReader(template));
        }

        /** Compiles the supplied template into a repeatedly executable intermediate form. */
        public Template compile (Reader source) {
            return Mustache.compile(source, this);
        }

        /** Returns a compiler that either does or does not escape HTML by default. */
        public Compiler escapeHTML (boolean escapeHTML) {
            return new Compiler(escapeHTML, this.standardsMode, this.nullValue, this.missingIsNull,
                                this.loader, this.collector);
        }

        /** Returns a compiler that either does or does not use standards mode. Standards mode
         * disables the non-standard JMustache extensions like looking up missing names in a parent
         * context. */
        public Compiler standardsMode (boolean standardsMode) {
            return new Compiler(this.escapeHTML, standardsMode, this.nullValue, this.missingIsNull,
                                this.loader, this.collector);
        }

        /** Returns a compiler that will use the given value for any variable that is missing, or
         * otherwise resolves to null. This is like {@link #nullValue} except that it returns the
         * supplied default for missing keys and existing keys that return null values. */
        public Compiler defaultValue (String defaultValue) {
            return new Compiler(this.escapeHTML, this.standardsMode, defaultValue, true,
                                this.loader, this.collector);
        }

        /** Returns a compiler that will use the given value for any variable that resolves to
         * null, but will still raise an exception for variables for which an accessor cannot be
         * found. This is like {@link #defaultValue} except that it differentiates between missing
         * accessors, and accessors that exist but return null.
         * <ul>
         * <li>In the case of a Java object being used as a context, if no field or method can be
         * found for a variable, an exception will be raised.</li>
         * <li>In the case of a {@link Map} being used as a context, all possible accessors are
         * assumed to exist (but potentially return null), and no exception will ever be
         * raised.</li>
         * </ul> */
        public Compiler nullValue (String nullValue) {
            return new Compiler(this.escapeHTML, this.standardsMode, nullValue, false,
                                this.loader, this.collector);
        }

        /** Returns a compiler configured to use the supplied template loader to handle partials. */
        public Compiler withLoader (TemplateLoader loader) {
            return new Compiler(this.escapeHTML, this.standardsMode, this.nullValue,
                                this.missingIsNull, loader, this.collector);
        }

        /** Returns a compiler configured to use the supplied collector. */
        public Compiler withCollector (Collector collector) {
            return new Compiler(this.escapeHTML, this.standardsMode, this.nullValue,
                                this.missingIsNull, this.loader, collector);
        }

        protected Compiler (boolean escapeHTML, boolean standardsMode, String nullValue,
                            boolean missingIsNull, TemplateLoader loader, Collector collector) {
            this.escapeHTML = escapeHTML;
            this.standardsMode = standardsMode;
            this.nullValue = nullValue;
            this.missingIsNull = missingIsNull;
            this.loader = loader;
            this.collector = collector;
        }
    }

    /** Used to handle partials. */
    public interface TemplateLoader
    {
        /** Returns a reader for the template with the supplied name.
         * @throws Exception if the template could not be loaded for any reason. */
        public Reader getTemplate (String name) throws Exception;
    }

    /** Used to read variables from values. */
    public interface VariableFetcher
    {
        /** Reads the so-named variable from the supplied context object. */
        Object get (Object ctx, String name) throws Exception;
    }

    /** Handles interpreting objects as collections. */
    public interface Collector
    {
        /** Returns an iterator that can iterate over the supplied value, or null if the value is
         * not a collection. */
        Iterator<?> toIterator (final Object value);

        /** Creates a fetcher for a so-named variable in the supplied context object, which will
         * never be null. The fetcher will be cached and reused for future contexts for which
         * {@code octx.getClass().equals(nctx.getClass()}. */
        VariableFetcher createFetcher (Object ctx, String name);
    }

    /**
     * Returns a compiler that escapes HTML by default and does not use standards mode.
     */
    public static Compiler compiler ()
    {
        return new Compiler(true, false, null, true, FAILING_LOADER, new DefaultCollector());
    }

    /**
     * Compiles the supplied template into a repeatedly executable intermediate form.
     */
    protected static Template compile (Reader source, Compiler compiler)
    {
        Accumulator accum = new Parser(compiler).parse(source);
        return new Template(accum.finish(), compiler);
    }

    private Mustache () {} // no instantiateski

    protected static void restoreStartTag (StringBuilder text, Delims starts)
    {
        text.insert(0, starts.start1);
        if (starts.start2 != NO_CHAR) {
            text.insert(1, starts.start2);
        }
    }

    protected static String escapeHTML (String text)
    {
        for (String[] escape : ATTR_ESCAPES) {
            text = text.replace(escape[0], escape[1]);
        }
        return text;
    }

    protected static boolean allowsWhitespace (char typeChar)
    {
        return (typeChar == '=') || // change delimiters
            (typeChar == '!');      // comment
    }

    protected static final int TEXT = 0;
    protected static final int MATCHING_START = 1;
    protected static final int MATCHING_END = 2;
    protected static final int TAG = 3;

    // a hand-rolled parser; whee!
    protected static class Parser {
        final Delims delims = new Delims();
        final StringBuilder text = new StringBuilder();

        Reader source;
        Accumulator accum;

        int state = TEXT;
        int line = 1, column = 0;
        int tagStartColumn = -1;
        boolean skipNewline = false;

        public Parser (Compiler compiler) {
            this.accum = new Accumulator(compiler);
        }

        public Accumulator parse (Reader source) {
            this.source = source;

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
                    column = 0;
                    line++;
                    // skip this newline character if we're configured to do so; TODO: handle CR
                    if (skipNewline) {
                        skipNewline = false;
                        continue;
                    }
                } else {
                    column++;
                    skipNewline = false;
                }

                parseChar(c);
            }

            // accumulate any trailing text
            switch (state) {
            case TAG:
                restoreStartTag(text, delims);
                break;
            case MATCHING_END:
                restoreStartTag(text, delims);
                text.append(delims.end1);
                break;
            case MATCHING_START:
                text.append(delims.start1);
                break;
                // case TEXT: // do nothing
            }
            accum.addTextSegment(text);

            return accum;
        }

        protected void parseChar (char c) {
            switch (state) {
            case TEXT:
                if (c == delims.start1) {
                    state = MATCHING_START;
                    tagStartColumn = column;
                    if (delims.start2 == NO_CHAR) {
                        parseChar(NO_CHAR);
                    }
                } else {
                    text.append(c);
                }
                break;

            case MATCHING_START:
                if (c == delims.start2) {
                    accum.addTextSegment(text);
                    state = TAG;
                } else {
                    text.append(delims.start1);
                    state = TEXT;
                    parseChar(c);
                }
                break;

            case TAG:
                if (c == delims.end1) {
                    state = MATCHING_END;
                    if (delims.end2 == NO_CHAR) {
                        parseChar(NO_CHAR);
                    }

                } else if (c == delims.start1 && text.length() > 0) {
                    // if we've already matched some tag characters and we see a new start tag
                    // character (e.g. "{{foo {" but not "{{{"), treat the already matched text as
                    // plain text and start matching a new tag from this point
                    restoreStartTag(text, delims);
                    accum.addTextSegment(text);
                    tagStartColumn = column;
                    if (delims.start2 == NO_CHAR) {
                        accum.addTextSegment(text);
                        state = TAG;
                    } else {
                        state = MATCHING_START;
                    }

                } else {
                    text.append(c);
                }
                break;

            case MATCHING_END:
                if (c == delims.end2) {
                    if (text.charAt(0) == '=') {
                        delims.updateDelims(text.substring(1, text.length()-1));
                        text.setLength(0);
                    } else {
                        // if we haven't remapped the delimiters, and the tag starts with {{{ then
                        // require that it end with }}} and disable HTML escaping
                        if (delims.isDefault() && text.charAt(0) == delims.start1) {
                            try {
                                // we've only parsed }} at this point, so we have to slurp in
                                // another character from the input stream and check it
                                int end3 = (char)source.read();
                                if (end3 != '}') {
                                    throw new MustacheParseException(
                                        "Invalid triple-mustache tag: {{{" + text + "}}", line);
                                }
                            } catch (IOException e) {
                                throw new MustacheException(e);
                            }
                            // convert it into (equivalent) {{&text}} which addTagSegment handles
                            text.replace(0, 1, "&");
                        }
                        // process the tag between the mustaches
                        accum = accum.addTagSegment(text, line);
                        skipNewline = (tagStartColumn == 1) && accum.justOpenedOrClosedBlock();
                    }
                    state = TEXT;

                } else {
                    text.append(delims.end1);
                    state = TAG;
                    parseChar(c);
                }
                break;
            }
        }
    }

    protected static class Delims {
        public char start1 = '{';
        public char start2 = '{';
        public char end1 = '}';
        public char end2 = '}';

        public boolean isDefault () {
            return start1 == '{' && start2 == '{' && end1 == '}' && end2 == '}';
        }

        public void updateDelims (String dtext) {
            String errmsg = "Invalid delimiter configuration '" + dtext + "'. Must be of the " +
                "form {{=1 2=}} or {{=12 34=}} where 1, 2, 3 and 4 are delimiter chars.";

            String[] delims = dtext.split(" ");
            if (delims.length != 2) throw new MustacheException(errmsg);

            switch (delims[0].length()) {
            case 1:
                start1 = delims[0].charAt(0);
                start2 = NO_CHAR;
                break;
            case 2:
                start1 = delims[0].charAt(0);
                start2 = delims[0].charAt(1);
                break;
            default:
                throw new MustacheException(errmsg);
            }

            switch (delims[1].length()) {
            case 1:
                end1 = delims[1].charAt(0);
                end2 = NO_CHAR;
                break;
            case 2:
                end1 = delims[1].charAt(0);
                end2 = delims[1].charAt(1);
                break;
            default:
                throw new MustacheException(errmsg);
            }
        }
    }

    protected static class Accumulator {
        public Accumulator (Compiler compiler) {
            _compiler = compiler;
        }

        public boolean justOpenedOrClosedBlock () {
            // return true if we just closed a block segment; we'll handle just opened elsewhere
            return (!_segs.isEmpty() && _segs.get(_segs.size()-1) instanceof BlockSegment);
        }

        public void addTextSegment (StringBuilder text) {
            if (text.length() > 0) {
                _segs.add(new StringSegment(text.toString()));
                text.setLength(0);
            }
        }

        public Accumulator addTagSegment (final StringBuilder accum, final int tagLine) {
            final Accumulator outer = this;
            String tag = accum.toString().trim();
            final String tag1 = tag.substring(1).trim();
            accum.setLength(0);

            switch (tag.charAt(0)) {
            case '#':
                requireNoNewlines(tag, tagLine);
                return new Accumulator(_compiler) {
                    @Override public boolean justOpenedOrClosedBlock () {
                        // if we just opened this section, we'll have no segments
                        return (_segs.isEmpty()) || super.justOpenedOrClosedBlock();
                    }
                    @Override public Template.Segment[] finish () {
                        throw new MustacheParseException(
                            "Section missing close tag '" + tag1 + "'", tagLine);
                    }
                    @Override protected Accumulator addCloseSectionSegment (String itag, int line) {
                        requireSameName(tag1, itag, line);
                        outer._segs.add(new SectionSegment(itag, super.finish(), tagLine));
                        return outer;
                    }
                };

            case '>':
                _segs.add(new IncludedTemplateSegment(tag1, _compiler));
                return this;

            case '^':
                requireNoNewlines(tag, tagLine);
                return new Accumulator(_compiler) {
                    @Override public boolean justOpenedOrClosedBlock () {
                        // if we just opened this section, we'll have no segments
                        return (_segs.isEmpty()) || super.justOpenedOrClosedBlock();
                    }
                    @Override public Template.Segment[] finish () {
                        throw new MustacheParseException(
                            "Inverted section missing close tag '" + tag1 + "'", tagLine);
                    }
                    @Override protected Accumulator addCloseSectionSegment (String itag, int line) {
                        requireSameName(tag1, itag, line);
                        outer._segs.add(new InvertedSectionSegment(itag, super.finish(), tagLine));
                        return outer;
                    }
                };

            case '/':
                requireNoNewlines(tag, tagLine);
                return addCloseSectionSegment(tag1, tagLine);

            case '!':
                // comment!, ignore
                return this;

            case '&':
                requireNoNewlines(tag, tagLine);
                _segs.add(new VariableSegment(tag1, false, tagLine));
                return this;

            default:
                requireNoNewlines(tag, tagLine);
                _segs.add(new VariableSegment(tag, _compiler.escapeHTML, tagLine));
                return this;
            }
        }

        public Template.Segment[] finish () {
            return _segs.toArray(new Template.Segment[_segs.size()]);
        }

        protected Accumulator addCloseSectionSegment (String tag, int line) {
            throw new MustacheParseException(
                "Section close tag with no open tag '" + tag + "'", line);
        }

        protected static void requireNoNewlines (String tag, int line) {
            if (tag.indexOf("\n") != -1 || tag.indexOf("\r") != -1) {
                throw new MustacheParseException(
                    "Invalid tag name: contains newline '" + tag + "'", line);
            }
        }

        protected static void requireSameName (String name1, String name2, int line)
        {
            if (!name1.equals(name2)) {
                throw new MustacheParseException("Section close tag with mismatched open tag '" +
                                                 name2 + "' != '" + name1 + "'", line);
            }
        }

        protected Compiler _compiler;
        protected final List<Template.Segment> _segs = new ArrayList<Template.Segment>();
    }

    /** A simple segment that reproduces a string. */
    protected static class StringSegment extends Template.Segment {
        public StringSegment (String text) {
            _text = text;
        }
        @Override public void execute (Template tmpl, Template.Context ctx, Writer out) {
            write(out, _text);
        }
        protected final String _text;
    }

    protected static class IncludedTemplateSegment extends Template.Segment {
        public IncludedTemplateSegment (final String templateName, final Compiler compiler) {
            Reader r;
            try {
                r = compiler.loader.getTemplate(templateName);
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException)e;
                } else {
                    throw new MustacheException("Unable to load template: " + templateName, e);
                }
            }
            _template = compiler.compile(r);
        }
        @Override public void execute (Template tmpl, Template.Context ctx, Writer out) {
            // we must take care to preserve our context rather than creating a new one, which
            // would happen if we just called execute() with ctx.data
            _template.executeSegs(ctx, out);
        }
        protected final Template _template;
    }

    /** A helper class for named segments. */
    protected static abstract class NamedSegment extends Template.Segment {
        protected NamedSegment (String name, int line) {
            _name = name.intern();
            _line = line;
        }
        protected final String _name;
        protected final int _line;
    }

    /** A segment that substitutes the contents of a variable. */
    protected static class VariableSegment extends NamedSegment {
        public VariableSegment (String name, boolean escapeHTML, int line) {
            super(name, line);
            _escapeHTML = escapeHTML;
        }
        @Override public void execute (Template tmpl, Template.Context ctx, Writer out)  {
            Object value = tmpl.getValueOrDefault(ctx, _name, _line);
            if (value == null) {
                throw new MustacheException(
                    "No key, method or field with name '" + _name + "' on line " + _line);
            }
            String text = String.valueOf(value);
            write(out, _escapeHTML ? escapeHTML(text) : text);
        }
        protected boolean _escapeHTML;
    }

    /** A helper class for block segments. */
    protected static abstract class BlockSegment extends NamedSegment {
        protected BlockSegment (String name, Template.Segment[] segs, int line) {
            super(name, line);
            _segs = segs;
        }
        protected void executeSegs (Template tmpl, Template.Context ctx, Writer out)  {
            for (Template.Segment seg : _segs) {
                seg.execute(tmpl, ctx, out);
            }
        }
        protected final Template.Segment[] _segs;
    }

    /** A segment that represents a section. */
    protected static class SectionSegment extends BlockSegment {
        public SectionSegment (String name, Template.Segment[] segs, int line) {
            super(name, segs, line);
        }
        @Override public void execute (Template tmpl, Template.Context ctx, Writer out)  {
            Object value = tmpl.getSectionValue(ctx, _name, _line); // won't return null
            Iterator<?> iter = tmpl._compiler.collector.toIterator(value);
            if (iter != null) {
                int index = 0;
                while (iter.hasNext()) {
                    Object elem = iter.next();
                    boolean onFirst = (index == 0), onLast = !iter.hasNext();
                    executeSegs(tmpl, ctx.nest(elem, ++index, onFirst, onLast), out);
                }
            } else if (value instanceof Boolean) {
                if ((Boolean)value) {
                    executeSegs(tmpl, ctx, out);
                }
            } else {
                executeSegs(tmpl, ctx.nest(value, 0, false, false), out);
            }
        }
    }

    /** A segment that represents an inverted section. */
    protected static class InvertedSectionSegment extends BlockSegment {
        public InvertedSectionSegment (String name, Template.Segment[] segs, int line) {
            super(name, segs, line);
        }
        @Override public void execute (Template tmpl, Template.Context ctx, Writer out)  {
            Object value = tmpl.getSectionValue(ctx, _name, _line); // won't return null
            Iterator<?> iter = tmpl._compiler.collector.toIterator(value);
            if (iter != null) {
                if (!iter.hasNext()) {
                    executeSegs(tmpl, ctx, out);
                }
            } else if (value instanceof Boolean) {
                if (!(Boolean)value) {
                    executeSegs(tmpl, ctx, out);
                }
            } // TODO: fail?
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

    /** Used when we have only a single character delimiter. */
    protected static final char NO_CHAR = Character.MIN_VALUE;

    protected static final TemplateLoader FAILING_LOADER = new TemplateLoader() {
        public Reader getTemplate (String name) {
            throw new UnsupportedOperationException("Template loading not configured");
        }
    };
}
