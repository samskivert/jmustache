//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
public class Mustache {

    /** Compiles templates into executable form. See {@link Mustache}. */
    public static class Compiler {

        /** Whether or not standards mode is enabled. */
        public final boolean standardsMode;

        /** Whether or not to throw an exception when a section resolves to a missing value. If
          * false, the section is simply omitted (or included in the case of inverse sections). If
          * true, a {@code MustacheException} is thrown. */
        public final boolean strictSections;

        /** A value to use when a variable resolves to null. If this value is null (which is the
          * default null value), an exception will be thrown. If {@link #missingIsNull} is also
          * true, this value will be used when a variable cannot be resolved.
          *
          * <p>If the nullValue contains a substring {@code {{name}}}, then this substring will be
          * replaced by name of the variable. For example, if nullValue is {@code ?{{name}}?} and
          * the missing variable is {@code foo}, then string {@code ?foo?} will be used.</p> */
        public final String nullValue;

        /** If this value is true, missing variables will be treated like variables that return
          * null. {@link #nullValue} will be used in their place, assuming {@link #nullValue} is
          * configured to a non-null value. */
        public final boolean missingIsNull;

        /** If this value is true, empty string will be treated as a false value, as in JavaScript
          * mustache implementation. Default is false. */
        public final boolean emptyStringIsFalse;

        /** If this value is true, zero will be treated as a false value, as in JavaScript
          * mustache implementation. Default is false. */
        public final boolean zeroIsFalse;

        /** Handles converting objects to strings when rendering a template. The default formatter
          * uses {@link String#valueOf}. */
        public final Formatter formatter;

        /** Handles escaping characters in substituted text. */
        public final Escaper escaper;

        /** The template loader in use during this compilation. */
        public final TemplateLoader loader;

        /** The collector used by templates compiled with this compiler. */
        public final Collector collector;

        /** The delimiters used by default in templates compiled with this compiler. */
        public final Delims delims;

        /** Compiles the supplied template into a repeatedly executable intermediate form. */
        public Template compile (String template) {
            return compile(new StringReader(template));
        }

        /** Compiles the supplied template into a repeatedly executable intermediate form. */
        public Template compile (Reader source) {
            return Mustache.compile(source, this);
        }

        /** Returns a compiler that either does or does not escape HTML by default. Note: this
          * overrides any escaper set via {@link #withEscaper}. */
        public Compiler escapeHTML (boolean escapeHTML) {
            return withEscaper(escapeHTML ? Escapers.HTML : Escapers.NONE);
        }

        /** Returns a compiler that either does or does not use standards mode. Standards mode
          * disables the non-standard JMustache extensions like looking up missing names in a parent
          * context. */
        public Compiler standardsMode (boolean standardsMode) {
            return new Compiler(standardsMode, this.strictSections, this.nullValue,
                                this.missingIsNull, this.emptyStringIsFalse, this.zeroIsFalse,
                                this.formatter, this.escaper, this.loader, this.collector,
                                this.delims);
        }

        /** Returns a compiler that throws an exception when a section references a missing value
          * ({@code true}) or treats a missing value as {@code false} ({@code false}, the default).
          */
        public Compiler strictSections (boolean strictSections) {
            return new Compiler(this.standardsMode, strictSections, this.nullValue,
                                this.missingIsNull, this.emptyStringIsFalse, this.zeroIsFalse,
                                this.formatter, this.escaper, this.loader, this.collector,
                                this.delims);
        }

        /** Returns a compiler that will use the given value for any variable that is missing, or
          * otherwise resolves to null. This is like {@link #nullValue} except that it returns the
          * supplied default for missing keys and existing keys that return null values. */
        public Compiler defaultValue (String defaultValue) {
            return new Compiler(this.standardsMode, this.strictSections, defaultValue, true,
                                this.emptyStringIsFalse, this.zeroIsFalse, this.formatter,
                                this.escaper, this.loader, this.collector, this.delims);
        }

        /** Returns a compiler that will use the given value for any variable that resolves to
          * null, but will still raise an exception for variables for which an accessor cannot be
          * found. This is like {@link #defaultValue} except that it differentiates between missing
          * accessors, and accessors that exist but return null.
          * <ul>
          * <li>In the case of a Java object being used as a context, if no field or method can be
          * found for a variable, an exception will be raised.</li>
          * <li>In the case of a {@link Map} being used as a context, if the map does not contain
          * a mapping for a variable, an exception will be raised. If the map contains a mapping
          * which maps to {@code null}, then {@code nullValue} is used.</li>
          * </ul> */
        public Compiler nullValue (String nullValue) {
            return new Compiler(this.standardsMode, this.strictSections, nullValue, false,
                                this.emptyStringIsFalse, this.zeroIsFalse, this.formatter,
                                this.escaper, this.loader, this.collector, this.delims);
        }

        /** Returns a compiler that will treat empty string as a false value if parameter is true. */
        public Compiler emptyStringIsFalse (boolean emptyStringIsFalse) {
            return new Compiler(this.standardsMode, this.strictSections, this.nullValue,
                                this.missingIsNull, emptyStringIsFalse, this.zeroIsFalse,
                                this.formatter, this.escaper, this.loader, this.collector,
                                this.delims);
        }

        /** Returns a compiler that will treat zero as a false value if parameter is true. */
        public Compiler zeroIsFalse (boolean zeroIsFalse) {
            return new Compiler(this.standardsMode, this.strictSections, this.nullValue,
                                this.missingIsNull, this.emptyStringIsFalse, zeroIsFalse,
                                this.formatter, this.escaper, this.loader, this.collector,
                                this.delims);
        }

        /** Configures the {@link Formatter} used to turn objects into strings. */
        public Compiler withFormatter (Formatter formatter) {
            return new Compiler(this.standardsMode, this.strictSections, this.nullValue,
                                this.missingIsNull, this.emptyStringIsFalse, this.zeroIsFalse,
                                formatter, this.escaper, this.loader, this.collector, this.delims);
        }

        /** Configures the {@link Escaper} used to escape substituted text. */
        public Compiler withEscaper (Escaper escaper) {
            return new Compiler(this.standardsMode, this.strictSections, this.nullValue,
                                this.missingIsNull, this.emptyStringIsFalse, this.zeroIsFalse,
                                this.formatter, escaper, this.loader, this.collector, this.delims);
        }

        /** Returns a compiler configured to use the supplied template loader to handle partials. */
        public Compiler withLoader (TemplateLoader loader) {
            return new Compiler(this.standardsMode, this.strictSections, this.nullValue,
                                this.missingIsNull, this.emptyStringIsFalse, this.zeroIsFalse,
                                this.formatter, this.escaper, loader, this.collector, this.delims);
        }

        /** Returns a compiler configured to use the supplied collector. */
        public Compiler withCollector (Collector collector) {
            return new Compiler(this.standardsMode, this.strictSections, this.nullValue,
                                this.missingIsNull, this.emptyStringIsFalse, this.zeroIsFalse,
                                this.formatter, this.escaper, this.loader, collector, this.delims);
        }

        /** Returns a compiler configured to use the supplied delims as default delimiters.
          * @param delims a string of the form {@code AB CD} or {@code A D} where A and B are
          * opening delims and C and D are closing delims. */
        public Compiler withDelims (String delims) {
            return new Compiler(this.standardsMode, this.strictSections, this.nullValue,
                                this.missingIsNull, this.emptyStringIsFalse, this.zeroIsFalse,
                                this.formatter, this.escaper, this.loader, this.collector,
                                new Delims().updateDelims(delims));
        }

        /** Returns the value to use in the template for the null-valued property {@code name}. See
          * {@link #nullValue} for more details. */
        public String computeNullValue (String name) {
            return (nullValue == null) ? null : nullValue.replace("{{name}}", name);
        }

        /** Returns true if the supplied value is "falsey". If {@link #emptyStringIsFalse} is true,
          * then empty strings are considered falsey. If {@link #zeroIsFalse} is true, then zero
          * values are considered falsey. */
        public boolean isFalsey (Object value) {
            return ((emptyStringIsFalse && isEmptyCharSequence(formatter.format(value))) ||
                    (zeroIsFalse && (value instanceof Number) && ((Number)value).longValue() == 0));
        }

        /**
         * Replaces "".equals(value). E.g. only not null values with length 0.
         */
        private boolean isEmptyCharSequence (Object value) {
            if (value == null) {
                return false;
            }
            if (value instanceof CharSequence) {
                return ((CharSequence) value).length() == 0;
            }
            return false;
        }

        /** Loads and compiles the template {@code name} using this compiler's configured template
          * loader. Note that this does no caching: the caller should cache the loaded template if
          * they expect to use it multiple times.
          * @return the compiled template.
          * @throws MustacheException if the template could not be loaded (due to I/O exception) or
          * compiled (due to syntax error, etc.).
          */
        public Template loadTemplate (String name) throws MustacheException {
            Reader tin = null;
            try {
                tin = loader.getTemplate(name);
                return compile(tin);
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException)e;
                } else {
                    throw new MustacheException("Unable to load template: " + name, e);
                }
            } finally {
                if (tin != null) try {
                    tin.close();
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        }

        protected Compiler (boolean standardsMode, boolean strictSections, String nullValue,
                            boolean missingIsNull, boolean emptyStringIsFalse, boolean zeroIsFalse,
                            Formatter formatter, Escaper escaper, TemplateLoader loader,
                            Collector collector, Delims delims) {
            this.standardsMode = standardsMode;
            this.strictSections = strictSections;
            this.nullValue = nullValue;
            this.missingIsNull = missingIsNull;
            this.emptyStringIsFalse = emptyStringIsFalse;
            this.zeroIsFalse = zeroIsFalse;
            this.formatter = formatter;
            this.escaper = escaper;
            this.loader = loader;
            this.collector = collector;
            this.delims = delims;
        }
    }

    /** Handles converting objects to strings when rendering templates. */
    public interface Formatter {

        /** Converts {@code value} to a CharSequence for inclusion in a template. */
        CharSequence format (Object value);
    }

    /** Handles lambdas. */
    public interface Lambda {

        /** Executes this lambda on the supplied template fragment. The lambda should write its
          * results to {@code out}.
          *
          * @param frag the fragment of the template that was passed to the lambda.
          * @param out the writer to which the lambda should write its output.
          */
        void execute (Template.Fragment frag, Writer out) throws IOException;
    }

    /** Handles lambdas that are also invoked for inverse sections.. */
    public interface InvertibleLambda extends Lambda {

        /** Executes this lambda on the supplied template fragment, when the lambda is used in an
          * inverse section. The lambda should write its results to {@code out}.
          *
          * @param frag the fragment of the template that was passed to the lambda.
          * @param out the writer to which the lambda should write its output.
          */
        void executeInverse (Template.Fragment frag, Writer out) throws IOException;
    }

    /** Reads variables from context objects. */
    public interface VariableFetcher {

        /** Reads the so-named variable from the supplied context object. */
        Object get (Object ctx, String name) throws Exception;
    }

    /** Handles escaping characters in substituted text. */
    public interface Escaper {

        /** Returns {@code raw} with the appropriate characters replaced with escape sequences. */
        String escape (String raw);

        /** Returns {@code raw} with the appropriate characters replaced with escape sequences. **/
        default CharSequence escape (CharSequence raw) {
            return escape(raw.toString());
        }

        /**
          * Escapes the raw characters with escape sequeneces if needed and appends to the appendable.
          * The default implementation calls {@link #escape(CharSequence)}.
          * @param a the stream like to append to.
          * @param raw input string.
          * @throws IOException if an error happens while writing to the appendable.
          */
        default void escape (Appendable a, CharSequence raw) throws IOException {
            a.append(escape(raw));
        }
    }

    /** Handles loading partial templates. */
    public interface TemplateLoader {

        /** Returns a reader for the template with the supplied name.
          * Reader will be closed by callee.
          * @throws Exception if the template could not be loaded for any reason. */
        Reader getTemplate (String name) throws Exception;
    }

    /** Handles interpreting objects as collections. */
    public interface Collector {

        /** Returns an iterator that can iterate over the supplied value, or null if the value is
          * not a collection. */
        Iterator<?> toIterator (final Object value);

        /** Creates a fetcher for a so-named variable in the supplied context object, which will
          * never be null. The fetcher will be cached and reused for future contexts for which
          * {@code octx.getClass().equals(nctx.getClass()}. */
        VariableFetcher createFetcher (Object ctx, String name);

        /** Creates a map to be used to cache {@link VariableFetcher} instances. The GWT-compatible
          * collector returns a HashMap here, but the reflection based fetcher (which only works on
          * the JVM and Android, returns a concurrent hashmap. */
        <K,V> Map<K,V> createFetcherCache ();
    }

    /**
     * Provides a means to implement custom logic for variable lookup. If a context object
     * implements this interface, its {@code get} method will be used to look up variables instead
     * of the usual methods.
     *
     * This is simpler than having a context implement {@link Map} which would require that it also
     * support the {@link Map#entrySet} method for iteration. A {@code CustomContext} object cannot
     * be used for a list section.
     */
    public interface CustomContext {

        /** Fetches the value of a variable named {@code name}. */
        Object get (String name) throws Exception;
    }

    /** Used to visit the tags in a template without executing it. */
    public interface Visitor {

        /** Visits a text segment. These are blocks of text that are normally just reproduced as
          * is when executing a template.
          * @param text the block of text. May contain newlines.
          */
        void visitText (String text);

        /** Visits a variable tag.
          * @param name the name of the variable.
          */
        void visitVariable (String name);

        /** Visits an include (partial) tag.
          * @param name the name of the partial template specified by the tag.
          * @return true if the template should be resolved and visited, false to skip it.
          */
        boolean visitInclude (String name);

       /** Visits a parent partial tag. For backward compatibility by default
         * <code>false</code> is returned.
         * @param name the name of the parent partial template specified by the tag.
         * @return true if the template should be resolved and visited, false to skip it.
         */
        default boolean visitParent (String name) {
            return false;
        }

       /** Visits a block tag. For backward compatibility by default is skipped.
         * @param name the name of the block.
         * @return true if the contents of the block should be visited, false to skip.
         */
        default boolean visitBlock (String name) {
            return false;
        }

        /** Visits a section tag.
          * @param name the name of the section.
          * @return true if the contents of the section should be visited, false to skip.
          */
        boolean visitSection (String name);

        /** Visits an inverted section tag.
          * @param name the name of the inverted section.
          * @return true if the contents of the section should be visited, false to skip.
          */
        boolean visitInvertedSection (String name);
    }

    /**
     * Returns a compiler that escapes HTML by default and does not use standards mode.
     */
    public static Compiler compiler () {
        return new Compiler(/*standardsMode=*/false, /*strictSections=*/false, /*nullValue=*/null,
                            /*missingIsNull=*/false, /*emptyStringIsFalse=*/false,
                            /*zeroIsFalse=*/false, DEFAULT_FORMATTER, Escapers.HTML, FAILING_LOADER,
                            new DefaultCollector(), new Delims());
    }

    /**
     * Compiles the supplied template into a repeatedly executable intermediate form.
     */
    protected static Template compile (Reader source, Compiler compiler) {
        Accumulator accum = new Parser(compiler).parse(source);
        return new Template(trim(accum.finish(), true), compiler);
    }

    private Mustache () {} // no instantiateski

    protected static Template.Segment[] trim (Template.Segment[] segs, boolean top) {
        // trim() modifies segs! Its return is not a copy!
        // now that we have all of our segments, we make a pass through them to trim whitespace
        // from section tags which stand alone on their lines
        for (int ii = 0, ll = segs.length; ii < ll; ii++) {
            Template.Segment seg = segs[ii];
            Template.Segment pseg = (ii > 0   ) ? segs[ii-1] : null;
            Template.Segment nseg = (ii < ll-1) ? segs[ii+1] : null;
            StringSegment prev = (pseg instanceof StringSegment) ? (StringSegment)pseg : null;
            StringSegment next = (nseg instanceof StringSegment) ? (StringSegment)nseg : null;
            // if we're at the top-level there are virtual "blank lines" before & after segs
            boolean prevBlank = ((pseg == null && top) || (prev != null && prev.trailsBlank()));
            boolean nextBlank = ((nseg == null && top) || (next != null && next.leadsBlank()));
            // potentially trim around the open and close tags of a block segment
            if (seg instanceof StandaloneSection) {
                StandaloneSection sect = (StandaloneSection)seg;
                String indent = "";
                if (prevBlank && sect.firstLeadsBlank()) {
                    if (prev != null)  {
                        // capture the indent before we trim
                        indent = prev.indent();
                        segs[ii-1] = prev.trimTrailBlank();
                    }
                    sect.trimFirstBlank();
                    sect.standaloneStart(true);
                }
                if (nextBlank && sect.lastTrailsBlank()) {
                    sect.trimLastBlank();
                    if (next != null) segs[ii+1] = next.trimLeadBlank();
                    sect.standaloneEnd(true);
                }
                if (sect instanceof ParentTemplateSegment && ! indent.equals("")) {
                    ParentTemplateSegment pt = (ParentTemplateSegment) sect;
                    segs[ii] = pt.indent(indent, pseg == null, nseg == null);
                }
            }

            // we have to indent partials if there is space before and they are also standalone.
            else if (seg instanceof IncludedTemplateSegment) {
                IncludedTemplateSegment include = (IncludedTemplateSegment) seg;
                if (prev != null && prevBlank && nextBlank) {
                    String indent = prev.indent();
                    include._standalone = true;
                    if (!indent.equals("")) {
                        include = include.indent(indent, pseg == null,nseg == null);
                        segs[ii] = include;
                    }
                    /*
                     * We trim the end because partials follow standalone just like blocks.
                     * HOWEVER we do NOT trim the previous StringSegment as it provides the partial indentation.
                     * See indentSegs.
                     */
                    if (next != null) {
                        segs[ii+1] = next.trimLeadBlank();
                    }
                }
            }
            // potentially trim around non-printing (comments/delims) segments
            else if (seg instanceof FauxSegment) {
                if (prevBlank && nextBlank) {
                    if (pseg != null) segs[ii-1] = prev.trimTrailBlank();
                    if (nseg != null) segs[ii+1] = next.trimLeadBlank();
                }
            }
        }
        return segs;
    }

    /**
     * Indents segments by indent.
     * @param _segs segments to be cloned if indentation is needed
     * @param indent the space to use for indent.
     * @param _first whether to append an indent on the first segment
     * @param _last whether to append an indent on the last segment last empty newline (no character after \n).
     * @return cloned segments if changed
     */
    static Template.Segment[] indentSegs (Template.Segment[] _segs, String indent, boolean _first, boolean _last) {
        // unlike trim this method clones the segments if they have changed so the return value must
        // be handled; a simple identity check on the return can be used to determine if there is
        // change
        if (indent.equals("")) {
            return _segs;
        }
        int length = _segs.length;
        Template.Segment[] copySegs = new Template.Segment[length];
        boolean changed = false;
        for (int i = 0; i < _segs.length; i++) {
            Template.Segment seg = _segs[i];
            Template.Segment pseg = (i > 0) ? _segs[i-1] : null;
            Template.Segment nseg = (i < length - 1) ? _segs[i+1] : null;
            Template.Segment copy;
            if (seg instanceof AbstractSectionSegment) {
                AbstractSectionSegment bs = (AbstractSectionSegment) seg;
                boolean first;
                boolean last;
                if (pseg == null) {
                    // We are the first segment so we inherit
                    // outer first
                    first = _first;
                }
                else if (bs.isStandaloneStart()) {
                    first = true;
                }
                else {
                    first = false;
                }
                if (bs.isStandalone()) {
                    // the closing tag owns the last new line
                    // in the section so we do not indent
                    last = false;
                }
                else if (nseg == null) {
                    // We are the last segment so we inherit the
                    // outer last
                    last = _last;
                }
                else {
                    last = true;
                }
                copy = bs.indent(indent, first, last);
            }
            else if (seg instanceof StringSegment) {
                boolean first;
                boolean last;
                if (pseg == null) {
                    first = _first;
                }
                else if (pseg.isStandalone()) {
                    first = true;
                }
                else {
                    first = false;
                }
                if (nseg == null) {
                    last = _last;
                }
                else if (nseg instanceof AbstractSectionSegment) {
                    AbstractSectionSegment bs = (AbstractSectionSegment) nseg;
                    last = ! bs.isStandaloneStart();
                }
                else if (nseg.isStandalone()) {
                    last = false;
                }
                else {
                    last = true;
                }
                copy = seg.indent(indent, first, last);
            }
            else if (seg instanceof IncludedTemplateSegment) {
                /*
                 * If we are standalone then we rely on the indentation already present before the
                 * partial tag.
                 * [  WS ]{{> partial }}[\n]
                 *
                 * That is partial tags do not have the trailing blank removed during the trim process.
                 * This avoids needlessley creating StringSegment tags.
                 */
                if (seg.isStandalone()) {
                    boolean last;
                    if (nseg == null) {
                        last = _last;
                    }
                    else if (nseg.isStandalone()) {
                        last = false;
                    }
                    else {
                        last = true;
                    }
                    // Again first = false here because we
                    //already have the indentation set on a previous segment
                    copy = seg.indent(indent, false, last);
                }
                else {
                    copy = seg;
                }
            }
            else {
                copy = seg.indent(indent, _first, _last);
            }
            if (copy != seg) {
                changed = true;
            }
            copySegs[i] = copy;
        }
        if (changed) {
            return copySegs;
        }
        return _segs;
    }

    static Template.Segment[] replaceBlockSegs(Template.Segment[] _segs, Map<String, BlockSegment> blocks) {
        if (blocks.isEmpty()) {
            return _segs;
        }
        int length = _segs.length;
        Template.Segment[] copySegs = new Template.Segment[length];
        boolean changed = false;
        for (int i = 0; i < _segs.length; i++) {
            Template.Segment seg = _segs[i];
            Template.Segment copy;
            if (seg instanceof BlockReplaceable) {
                BlockReplaceable br = (BlockReplaceable) seg;
                copy = br.replaceBlocks(blocks);
            }
            else {
                copy = seg;
            }
            if (copy != seg) {
                changed = true;
            }
            copySegs[i] = copy;
        }
        if (changed) {
            return copySegs;
        }
        return _segs;
    }

    protected static void restoreStartTag (StringBuilder text, Delims starts) {
        text.insert(0, starts.start1);
        if (starts.start2 != NO_CHAR) {
            text.insert(1, starts.start2);
        }
    }

    // TODO: this method was never called, what was my intention here?
    protected static boolean allowsWhitespace (char typeChar) {
        return (typeChar == '=' /* change delimiters */) || (typeChar == '!' /* comment */);
    }

    protected static final int TEXT = 0;
    protected static final int MATCHING_START = 1;
    protected static final int MATCHING_END = 2;
    protected static final int TAG = 3;

    // a hand-rolled parser; whee!
    protected static class Parser {
        final Delims delims;
        final StringBuilder text = new StringBuilder();

        Reader source;
        Accumulator accum;

        int state = TEXT;
        int line = 1, column = 0;
        int tagStartColumn = -1;

        public Parser (Compiler compiler) {
            this.accum = new Accumulator(compiler, true);
            this.delims = compiler.delims.copy();
        }

        public Accumulator parse (Reader source) {
            this.source = source;

            int v;
            while ((v = nextChar()) != -1) {
                char c = (char)v;
                ++column; // our columns start at one, so increment before parse
                parseChar(c);
                // if we just parsed a newline, reset the column to zero and advance line
                if (c == '\n') {
                    column = 0;
                    ++line;
                }
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
            case TEXT: // do nothing
                break;
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

                } else if (c == delims.start1 && text.length() > 0 && text.charAt(0) != '!') {
                    // if we've already matched some tag characters and we see a new start tag
                    // character (e.g. "{{foo {" but not "{{{"), treat the already matched text as
                    // plain text and start matching a new tag from this point, unless we're in
                    // a comment tag.
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
                        accum.addFauxSegment(); // for newline trimming
                    } else {
                        // if the delimiters are {{ and }}, and the tag starts with {{{ then
                        // require that it end with }}} and disable escaping
                        if (delims.isStaches() && text.charAt(0) == delims.start1) {
                            // we've only parsed }} at this point, so we have to slurp in another
                            // character from the input stream and check it
                            int end3 = nextChar();
                            if (end3 != '}') {
                                String got = (end3 == -1) ? "" : String.valueOf((char)end3);
                                throw new MustacheParseException(
                                    "Invalid triple-mustache tag: {{" + text + "}}" + got, line);
                            }
                            // convert it into (equivalent) {{&text}} which addTagSegment handles
                            text.replace(0, 1, "&");
                        }
                        // process the tag between the mustaches
                        accum = accum.addTagSegment(text, line);
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

        protected int nextChar () {
            try {
                return source.read();
            } catch (IOException ioe) {
                throw new MustacheException(ioe);
            }
        }
    }

    protected static class Delims {
        public char start1 = '{', end1 = '}';
        public char start2 = '{', end2 = '}';

        public boolean isStaches () {
            return start1 == '{' && start2 == '{' && end1 == '}' && end2 == '}';
        }

        public Delims updateDelims (String dtext) {
            String[] delims = dtext.split(" ");
            if (delims.length != 2) throw new MustacheException(errmsg(dtext));

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
                throw new MustacheException(errmsg(dtext));
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
                throw new MustacheException(errmsg(dtext));
            }
            return this;
        }

        public void addTag (char prefix, String name, StringBuilder into) {
            into.append(start1);
            into.append(start2);
            if (prefix != ' ') into.append(prefix);
            into.append(name);
            into.append(end1);
            into.append(end2);
        }

        Delims copy () {
            Delims d = new Delims();
            d.start1 = start1;
            d.start2 = start2;
            d.end1 = end1;
            d.end2 = end2;
            return d;
        }

        private static String errmsg (String dtext) {
            return "Invalid delimiter configuration '" + dtext + "'. Must be of the " +
                "form {{=1 2=}} or {{=12 34=}} where 1, 2, 3 and 4 are delimiter chars.";
        }
    }

    protected static class Accumulator {
        public Accumulator (Compiler compiler, boolean topLevel) {
            _comp = compiler;
            _topLevel = topLevel;
        }

        public void addTextSegment (StringBuilder text) {
            if (text.length() > 0) {
                _segs.add(new StringSegment(text.toString(), _segs.isEmpty() && _topLevel));
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
                return new Accumulator(_comp, false) {
                    @Override public Template.Segment[] finish () {
                        throw new MustacheParseException(
                            "Section missing close tag '" + tag1 + "'", tagLine);
                    }
                    @Override protected Accumulator addCloseSectionSegment (String itag, int line) {
                        requireSameName(tag1, itag, line);
                        outer._segs.add(new SectionSegment(_comp, itag, super.finish(), tagLine));
                        return outer;
                    }
                };

            case '>':
                _segs.add(new IncludedTemplateSegment(_comp, tag1, tagLine));
                return this;
            case '<':
                requireNoNewlines(tag, tagLine);
                return new Accumulator(_comp, false) {
                    @Override public Template.Segment[] finish () {
                        throw new MustacheParseException(
                            "Parent missing close tag '" + tag1 + "'", tagLine);
                    }
                    @Override protected Accumulator addCloseSectionSegment (String itag, int line) {
                        requireSameName(tag1, itag, line);
                        outer._segs.add(new ParentTemplateSegment(_comp, itag, super.finish(), tagLine));
                        return outer;
                    }
                };
            case '$':
                requireNoNewlines(tag, tagLine);
                return new Accumulator(_comp, false) {
                    @Override public Template.Segment[] finish () {
                        throw new MustacheParseException(
                            "Block missing close tag '" + tag1 + "'", tagLine);
                    }
                    @Override protected Accumulator addCloseSectionSegment (String itag, int line) {
                        requireSameName(tag1, itag, line);
                        outer._segs.add(new BlockSegment(_comp, itag, super.finish(), tagLine));
                        return outer;
                    }
                };
            case '^':
                requireNoNewlines(tag, tagLine);
                return new Accumulator(_comp, false) {
                    @Override public Template.Segment[] finish () {
                        throw new MustacheParseException(
                            "Inverted section missing close tag '" + tag1 + "'", tagLine);
                    }
                    @Override protected Accumulator addCloseSectionSegment (String itag, int line) {
                        requireSameName(tag1, itag, line);
                        outer._segs.add(new InvertedSegment(_comp, itag, super.finish(), tagLine));
                        return outer;
                    }
                };

            case '/':
                requireNoNewlines(tag, tagLine);
                return addCloseSectionSegment(tag1, tagLine);

            case '!':
                // comment!, ignore
                _segs.add(new FauxSegment()); // for whitespace trimming
                return this;

            case '&':
                requireNoNewlines(tag, tagLine);
                _segs.add(new VariableSegment(tag1, tagLine, _comp.formatter, Escapers.NONE));
                return this;

            default:
                requireNoNewlines(tag, tagLine);
                _segs.add(new VariableSegment(tag, tagLine, _comp.formatter, _comp.escaper));
                return this;
            }
        }

        public void addFauxSegment () {
            _segs.add(new FauxSegment());
        }

        public Template.Segment[] finish () {
            return _segs.toArray(new Template.Segment[_segs.size()]);
        }

        protected Accumulator addCloseSectionSegment (String tag, int line) {
            throw new MustacheParseException(
                "Section close tag with no open tag '" + tag + "'", line);
        }

        protected static void requireNoNewlines (String tag, int line) {
            if (tag.indexOf('\n') != -1 || tag.indexOf('\r') != -1) {
                throw new MustacheParseException(
                    "Invalid tag name: contains newline '" + tag + "'", line);
            }
        }

        protected static void requireSameName (String name1, String name2, int line) {
            if (!name1.equals(name2)) {
                throw new MustacheParseException("Section close tag with mismatched open tag '" +
                                                 name2 + "' != '" + name1 + "'", line);
            }
        }

        protected final Compiler _comp;
        protected final boolean _topLevel;
        protected final List<Template.Segment> _segs = new ArrayList<Template.Segment>();
    }

    /** A simple segment that reproduces a string. */
    protected static class StringSegment extends Template.Segment {
        public StringSegment (String text, boolean first) {
            this(text, blankPos(text, true, first), blankPos(text, false, first), first);
        }

        public StringSegment (String text, int leadBlank, int trailBlank, boolean first) {
            assert leadBlank >= -1;
            assert trailBlank >= -1;
            _text = text;
            _leadBlank = leadBlank;
            _trailBlank = trailBlank;
            _first = first;
        }

        public boolean leadsBlank () { return _leadBlank != -1; }
        public boolean trailsBlank () { return _trailBlank != -1; }

        public StringSegment trimLeadBlank () {
            if (_leadBlank == -1) return this;
            int lpos = _leadBlank+1, newTrail = _trailBlank == -1 ? -1 : _trailBlank-lpos;
            return new StringSegment(_text.substring(lpos), -1, newTrail, _first);
        }
        public StringSegment trimTrailBlank  () {
            return _trailBlank == -1 ? this : new StringSegment(
                _text.substring(0, _trailBlank), _leadBlank, -1, _first);
        }

        /**
         * Calculate indent for partial idententation
         * @return indent space or empty string
         */
        String indent () {
            if (_trailBlank == -1 || _trailBlank >= _text.length()) {
                return "";
            }
           return  _text.substring(_trailBlank);
        }

        StringSegment indent (String indent, boolean first, boolean last) {
            if (indent.equals("")) {
                return this;
            }
            String reindent = reindent(_text, indent, first, last);
            return new StringSegment(reindent, _first);
        }

        @Override boolean isStandalone () {
            return false;
        }

        @Override public void execute (Template tmpl, Template.Context ctx, Writer out) {
            write(out, _text);
        }
        @Override public void decompile (Delims delims, StringBuilder into) {
            into.append(_text);
        }
        @Override public void visit (Visitor visitor) {
            visitor.visitText(_text);
        }
        @Override public String toString () {
            return "Text(" + _text.replace("\r", "\\r").replace("\n", "\\n") + ")" +
                _leadBlank + "/" + _trailBlank;
        }

        // we indent after every new line for partial indententation
        private static String reindent (String input, String indent, boolean first, boolean last) {
            int length = input.length();
            StringBuilder sb = new StringBuilder(indent.length() + length);
            if (first) {
                sb.append(indent);
            }
            for (int ii = 0; ii < length; ii++) {
                char c = input.charAt(ii);
                sb.append(c);
                if (c == '\n' && (last || ii != length - 1)) {
                    sb.append(indent);
                }
            }
            return sb.toString();
        }

        private static int blankPos (String text, boolean leading, boolean first) {
            int len = text.length();
            for (int ii = leading ? 0 : len-1, ll = leading ? len : -1, dd = leading ? 1 : -1;
                 ii != ll; ii += dd) {
                char c = text.charAt(ii);
                if (c == '\n') return leading ? ii : ii+1;
                if (!Character.isWhitespace(c)) return -1;
            }
            // if this is the first string segment and we're looking for trailing whitespace, a
            // totally blank segment (but which lacks a newline) is all trailing whitespace
            return (leading || !first) ? -1 : 0;
        }

        protected final String _text;
        protected final int _leadBlank, _trailBlank;
        protected final boolean _first;
    }

    /** An abstract segment that is a template include. */
    protected static abstract class AbstractPartialSegment extends NamedSegment {
        protected AbstractPartialSegment (Compiler compiler, String name, int line, String indent) {
            super(name, line);
            _comp = compiler;
            _indent = indent;
        }
        @Override public final void execute (Template tmpl, Template.Context ctx, Writer out) {
            // we must take care to preserve our context rather than creating a new one, which
            // would happen if we just called execute() with ctx.data
            getTemplate().executeSegs(ctx, out);
        }
        protected final Template getTemplate () {
            // we compile our template lazily to avoid infinie recursion if a template includes
            // itself (see issue #13)
            Template t = _template;
            if (t == null) {
                // We cannot use synchronized or a CAS operation here since loadTemplate might be an IO call
                // and virtual threads prefer regular locks.
                lock.lock();
                try {
                    if ((t = _template) == null) {
                        _template = t = _loadTemplate();
                    }
                } finally {
                    lock.unlock();
                }
            }
            return t;
        }

        protected Template _loadTemplate() {
            return _comp.loadTemplate(_name).indent(_indent);
        }

        @Override public abstract boolean isStandalone();

        protected final Compiler _comp;
        protected final String _indent;
        private final Lock lock = new ReentrantLock();
        private volatile Template _template;
    }

    /** A segment that loads and executes a sub-template by spec called a partial. */
    protected static class IncludedTemplateSegment extends AbstractPartialSegment {
        public IncludedTemplateSegment (Compiler compiler, String name, int line) {
            this(compiler, name, line,"");
        }
        private IncludedTemplateSegment (Compiler compiler, String name, int line, String indent) {
            super(compiler, name, line, indent);
        }
        @Override public void decompile (Delims delims, StringBuilder into) {
            delims.addTag('<', _name, into);
        }
        @Override public void visit (Visitor visitor) {
            if (visitor.visitInclude(_name)) {
                getTemplate().visit(visitor);
            }
        }
        @Override protected IncludedTemplateSegment indent(String indent, boolean first, boolean last) {
            // Indent this partial based on the spacing provided.
            // per the spec however much the partial reference is indendented (leading whitespace)
            // is how much the partial content should be indented.
            if (indent.equals("") || ! _standalone) {
                return this;
            }
            IncludedTemplateSegment is = new IncludedTemplateSegment(_comp, _name, _line, indent + this._indent );
            is._standalone = _standalone;
            return is;
        }

        @Override public String toString () {
            return "Include(name=" + _name + ", indent=" + _indent + ", standalone=" + _standalone
                    + ")";
        }
        @Override public boolean isStandalone() { return _standalone; }
        protected boolean _standalone;
    }

    /** A segment that loads and executes a parent template by spec called inheritance. */
    protected static class ParentTemplateSegment extends AbstractPartialSegment implements StandaloneSection {
        public ParentTemplateSegment (Compiler compiler, String name, Template.Segment[] segs, int line) {
            this(compiler, name, segs, line, "");
        }
        private ParentTemplateSegment (Compiler compiler, String name, Template.Segment[] segs, int line, String indent) {
            super(compiler, name, line, indent);
            // Notice we consider the contents inside a parent to be at "top" = true.
            // Furthermore to correctly trim we remove non blocks.
            this._segs = trim(removeNonBlocks(segs), true);
            this._blocks = new LinkedHashMap<>();
        }
        private ParentTemplateSegment (ParentTemplateSegment original, Template.Segment[] segs, String indent, Map<String, BlockSegment> blocks) {
            super(original._comp, original._name, original._line, indent + original._indent);
            this._segs = segs;
            this._standaloneStart = original._standaloneStart;
            this._standaloneEnd = original._standaloneEnd;
            Map<String, BlockSegment> newBlocks = new LinkedHashMap<>();
            newBlocks.putAll(original._blocks);
            newBlocks.putAll(blocks);
            this._blocks = newBlocks;
        }
        private static Template.Segment[] removeNonBlocks(Template.Segment[] segs) {
            // the content inside a parent call
            // e.g. {{<parent}}...{{/parent}}
            // is ignored other than block segments: {{$block}}{{/block}}
            List<Template.Segment> copy = new ArrayList<>();
            for (Template.Segment seg : segs) {
                if (seg instanceof BlockSegment) {
                    copy.add(seg);
                }
            }
            return copy.toArray(new Template.Segment[] {});
        }
        @Override public void decompile (Delims delims, StringBuilder into) {
            delims.addTag('<', _name, into);
        }
        @Override public void visit (Visitor visitor) {
            if (visitor.visitParent(_name)) {
                getTemplate().visit(visitor);
            }
        }
        @Override protected ParentTemplateSegment indent(String indent, boolean first, boolean last) {
            // Indent this partial based on the spacing provided.
            // per the spec however much the partial reference is indendented (leading whitespace minus \n)
            // is how much the partial content should be indented.
            if (indent.equals("") || ! _standaloneStart) {
                return this;
            }
            return new ParentTemplateSegment(this, this._segs, indent, Collections.emptyMap());
        }
        @Override protected Template _loadTemplate () {
            Map<String, BlockSegment> blocks = new LinkedHashMap<>();
            // While we might capture other segments we only care about blocks.
            // The reason we are doing this now instead of at constructor time is
            // that indentation and trim might have changed the segments (not sure on this).
            for (Template.Segment seg : _segs) {
                if (seg instanceof BlockSegment) {
                    BlockSegment bs = (BlockSegment) seg;
                    blocks.put(bs._name, bs);
                }
            }
            blocks.putAll(_blocks);
            return super._loadTemplate().replaceBlocks(blocks);
        }
        // Parents have an unusual condition
        // where if they are empty the end tag still
        // owns the following newline.
        // Thus lastTrails and trimLast need custom behavior.
        @Override public boolean lastTrailsBlank () {
            Template.Segment[] _segs = _segs();
            int lastIdx = _segs.length-1;

            if (lastIdx < 0) {
                return true;
            }
            if (!(_segs[lastIdx] instanceof StringSegment)) return false;
            return ((StringSegment)_segs[lastIdx]).trailsBlank();
        }
        @Override public void trimLastBlank () {
            Template.Segment[] _segs = _segs();
            int idx = _segs.length-1;
            if (idx < 0) return;
            _segs[idx] = ((StringSegment)_segs[idx]).trimTrailBlank();
        }

        @Override public ParentTemplateSegment replaceBlocks(Map<String, BlockSegment> blocks) {
            return new ParentTemplateSegment(this, _segs, "", blocks);
        }
        @Override public Template.Segment[] _segs() { return _segs; }
        @Override public boolean isStandalone() { return _standaloneEnd; }
        @Override public boolean isStandaloneStart() { return _standaloneStart; }
        @Override public boolean isStandaloneEnd() { return _standaloneEnd; }
        @Override public void standaloneStart(boolean standaloneStart) { this._standaloneStart = standaloneStart; }
        @Override public void standaloneEnd(boolean standaloneEnd) { this._standaloneEnd = standaloneEnd; }
        @Override public String toString() {
            return "Parent(name=" + _name + ", indent=" + _indent + ", standaloneStart=" + _standaloneStart
                    + ")";
        }
        protected final Template.Segment[] _segs;
        protected boolean _standaloneStart = false;
        protected boolean _standaloneEnd = false;
        protected final Map<String, BlockSegment> _blocks;
    }

    /** A helper class for named segments. */
    protected static abstract class NamedSegment extends Template.Segment {
        protected NamedSegment (String name, int line) {
            _name = name;
            _line = line;
        }
        protected final String _name;
        protected final int _line;
    }

    /** A segment that substitutes the contents of a variable. */
    protected static class VariableSegment extends NamedSegment {
        public VariableSegment (String name, int line, Formatter formatter, Escaper escaper) {
            super(name, line);
            _formatter = formatter;
            _escaper = escaper;
        }
        @Override public void execute (Template tmpl, Template.Context ctx, Writer out) {
            Object value = tmpl.getValueOrDefault(ctx, _name, _line);
            if (value == null) {
                String msg = Template.isThisName(_name) ?
                    "Resolved '.' to null (which is disallowed), on line " + _line :
                    "No key, method or field with name '" + _name + "' on line " + _line;
                throw new MustacheException.Context(msg, _name, _line);
            }
            escape(out, _formatter.format(value), _escaper);
        }
        @Override public void decompile (Delims delims, StringBuilder into) {
            delims.addTag(' ', _name, into);
        }
        @Override public void visit (Visitor visitor) {
            visitor.visitVariable(_name);
        }
        @Override
        VariableSegment indent (String indent, boolean first, boolean last) {
            return this;
        }
        @Override
        boolean isStandalone () {
            return false;
        }
        @Override public String toString () {
            return "Var(" + _name + ":" + _line + ")";
        }
        protected final Formatter _formatter;
        protected final Escaper _escaper;
    }

    protected interface StandaloneSection extends BlockReplaceable {
        default boolean firstLeadsBlank () {
            Template.Segment[] _segs = _segs();
            if (_segs.length == 0 || !(_segs[0] instanceof StringSegment)) return false;
            return ((StringSegment)_segs[0]).leadsBlank();
        }
        default void trimFirstBlank () {
            Template.Segment[] _segs = _segs();
            _segs[0] = ((StringSegment)_segs[0]).trimLeadBlank();
        }
        default boolean lastTrailsBlank () {
            Template.Segment[] _segs = _segs();
            int lastIdx = _segs.length-1;
            if (_segs.length == 0 || !(_segs[lastIdx] instanceof StringSegment)) return false;
            return ((StringSegment)_segs[lastIdx]).trailsBlank();
        }
        default void trimLastBlank () {
            Template.Segment[] _segs = _segs();
            int idx = _segs.length-1;
            _segs[idx] = ((StringSegment)_segs[idx]).trimTrailBlank();
        }
        boolean isStandaloneEnd ();
        boolean isStandaloneStart ();
        void standaloneStart (boolean standaloneStart);
        void standaloneEnd (boolean standaloneEnd);

        Template.Segment[] _segs();
    }

    protected interface BlockReplaceable {
        public Template.Segment replaceBlocks(Map<String, BlockSegment> blocks);
    }

    /** A helper class for section-like segments. */
    protected static abstract class AbstractSectionSegment extends NamedSegment implements StandaloneSection {

        protected AbstractSectionSegment (Compiler compiler, String name, Template.Segment[] segs, int line) {
            super(name, line);
            _comp = compiler;
            _segs = trim(segs, false);
        }
        protected AbstractSectionSegment (AbstractSectionSegment original, Template.Segment[] segs) {
            super(original._name, original._line);
            _comp = original._comp;
            // this call assumes the segments are already trimmed
            _segs = segs;
        }

        protected void executeSegs (Template tmpl, Template.Context ctx, Writer out) {
            for (Template.Segment seg : _segs) {
                seg.execute(tmpl, ctx, out);
            }
        }

        protected abstract AbstractSectionSegment indent (String indent, boolean first, boolean last);

        @Override public boolean isStandalone() { return _standaloneEnd; }
        @Override public boolean isStandaloneStart() { return _standaloneStart; }
        @Override public boolean isStandaloneEnd() { return _standaloneEnd; }
        @Override public void standaloneStart(boolean standaloneStart) { this._standaloneStart = standaloneStart; }
        @Override public void standaloneEnd(boolean standaloneEnd) { this._standaloneEnd = standaloneEnd; }

        @Override public Template.Segment[] _segs() { return _segs; }

        protected final Compiler _comp;
        protected final Template.Segment[] _segs;
        protected boolean _standaloneStart = false;
        protected boolean _standaloneEnd = false;
    }

    /** A segment that represents a section. */
    protected static class SectionSegment extends AbstractSectionSegment {
        public SectionSegment (Compiler compiler, String name, Template.Segment[] segs, int line) {
            super(compiler, name, segs, line);
        }
        protected SectionSegment (SectionSegment original, Template.Segment[] segs) {
            super(original, segs);
        }
        @Override public void execute (Template tmpl, Template.Context ctx, Writer out) {
            Object value = tmpl.getSectionValue(ctx, _name, _line); // won't return null
            Iterator<?> iter = _comp.collector.toIterator(value);
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
            } else if (value instanceof Lambda) {
                try {
                    ((Lambda)value).execute(tmpl.createFragment(_segs, ctx), out);
                } catch (IOException ioe) {
                    throw new MustacheException(ioe);
                }
            } else if (_comp.isFalsey(value)) {
                // omit the section
            } else {
                executeSegs(tmpl, ctx.nest(value), out);
            }
        }
        @Override public void decompile (Delims delims, StringBuilder into) {
            delims.addTag('#', _name, into);
            for (Template.Segment seg : _segs) seg.decompile(delims, into);
            delims.addTag('/', _name, into);
        }
        @Override public void visit (Visitor visitor) {
            if (visitor.visitSection(_name)) {
                for (Template.Segment seg : _segs) {
                    seg.visit(visitor);
                }
            }
        }
        @Override protected SectionSegment indent (String indent, boolean first, boolean last) {
            Template.Segment[] segs = indentSegs(_segs, indent, first, last);
            if (segs == _segs) {
                return this;
            }
            return new SectionSegment(this, segs);
        }
        @Override public SectionSegment replaceBlocks(Map<String, BlockSegment> blocks) {
            Template.Segment[] segs = replaceBlockSegs(_segs, blocks);
            if (segs == _segs) {
                return this;
            }
            return new SectionSegment(this, segs);
        }
        @Override public String toString () {
            return "Section(" + _name + ":" + _line + "): " + Arrays.toString(_segs);
        }
    }

    /** A parent partial parameter using $ as the sigil. */
    protected static class BlockSegment extends AbstractSectionSegment {
        public BlockSegment (Compiler compiler, String name, Template.Segment[] segs, int line) {
            super(compiler, name, segs, line);
        }
        protected BlockSegment (BlockSegment original, Template.Segment[] segs) {
            super(original, segs);
        }
        @Override public void execute (Template tmpl, Template.Context ctx, Writer out) {
            executeSegs(tmpl, ctx, out);
        }
        @Override public void decompile (Delims delims, StringBuilder into) {
            delims.addTag('$', _name, into);
            for (Template.Segment seg : _segs) seg.decompile(delims, into);
            delims.addTag('/', _name, into);
        }
        @Override public void visit (Visitor visitor) {
            if (visitor.visitBlock(_name)) {
                for (Template.Segment seg : _segs) {
                    seg.visit(visitor);
                }
            }
        }
        @Override protected BlockSegment indent (String indent, boolean first, boolean last) {
            // Current indenting block segments is not defined by spec but might eventually
            Template.Segment[] segs = indentSegs(_segs, indent, first, last);
            if (segs == _segs) {
                return this;
            }
            return new BlockSegment(this, segs);
        }
        @Override public BlockSegment replaceBlocks (Map<String, BlockSegment> blocks) {
            BlockSegment bs = blocks.get(_name);
            if (bs == null) {
                Template.Segment[] segs = replaceBlockSegs(_segs, blocks);
                if (segs == _segs) {
                    return this;
                }
                return new BlockSegment(this, segs);
            }
            return new BlockSegment(this, bs._segs);
        }
        @Override public String toString () {
            return "Block(" + _name + ":" + _line + "): " + Arrays.toString(_segs);
        }
    }

    /** A segment that represents an inverted section. */
    protected static class InvertedSegment extends AbstractSectionSegment {
        public InvertedSegment (Compiler compiler, String name, Template.Segment[] segs, int line) {
            super(compiler, name, segs, line);
            _comp = compiler;
        }
        protected InvertedSegment (InvertedSegment original, Template.Segment[] segs) {
            super(original, segs);
            _comp = original._comp;
        }
        @Override public void execute (Template tmpl, Template.Context ctx, Writer out) {
            Object value = tmpl.getSectionValue(ctx, _name, _line); // won't return null
            Iterator<?> iter = _comp.collector.toIterator(value);
            if (iter != null) {
                if (!iter.hasNext()) {
                    executeSegs(tmpl, ctx, out);
                }
            } else if (value instanceof Boolean) {
                if (!(Boolean)value) {
                    executeSegs(tmpl, ctx, out);
                }
            } else if (value instanceof InvertibleLambda) {
                try {
                    ((InvertibleLambda)value).executeInverse(tmpl.createFragment(_segs, ctx), out);
                } catch (IOException ioe) {
                    throw new MustacheException(ioe);
                }
            } else if (_comp.isFalsey(value)) {
                executeSegs(tmpl, ctx, out);
            } // TODO: fail?
        }
        @Override public void decompile (Delims delims, StringBuilder into) {
            delims.addTag('^', _name, into);
            for (Template.Segment seg : _segs) seg.decompile(delims, into);
            delims.addTag('/', _name, into);
        }
        @Override public void visit (Visitor visitor) {
            if (visitor.visitInvertedSection(_name)) {
                for (Template.Segment seg : _segs) {
                    seg.visit(visitor);
                }
            }
        }
        @Override protected InvertedSegment indent (String indent, boolean first, boolean last) {
            Template.Segment[] segs = indentSegs(_segs, indent, first, last);
            if (segs == _segs) {
                return this;
            }
            return new InvertedSegment(this, segs);
        }
        @Override public InvertedSegment replaceBlocks (Map<String, BlockSegment> blocks) {
            Template.Segment[] segs = replaceBlockSegs(_segs, blocks);
            if (segs == _segs) {
                return this;
            }
            return new InvertedSegment(this, segs);
        }
        @Override public String toString () {
            return "Inverted(" + _name + ":" + _line + "): " + Arrays.toString(_segs);
        }
        protected final Compiler _comp;
    }

    protected static class FauxSegment extends Template.Segment {
        @Override public void execute (Template tmpl, Template.Context ctx, Writer out) {} // nada
        @Override public void decompile (Delims delims, StringBuilder into) {} // nada
        @Override public void visit (Visitor visit) {}
        @Override FauxSegment indent (String indent, boolean first, boolean last) { return this; }
        @Override boolean isStandalone () { return true; }
        @Override public String toString () { return "Faux"; }
    }

    /** Used when we have only a single character delimiter. */
    protected static final char NO_CHAR = Character.MIN_VALUE;

    protected static final TemplateLoader FAILING_LOADER = new TemplateLoader() {
        public Reader getTemplate (String name) {
            throw new UnsupportedOperationException("Template loading not configured");
        }
    };

    protected static final Formatter DEFAULT_FORMATTER = new Formatter() {
        public String format (Object value) {
            return String.valueOf(value);
        }
    };
}
