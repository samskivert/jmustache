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
import java.util.Map;

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
    /** Compiles templates into executable form. See {@link Mustache}. */
    public static class Compiler
    {
        /** Whether or not standards mode is enabled. */
        public final boolean standardsMode;

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
            return new Compiler(standardsMode, this.nullValue, this.missingIsNull,
                                this.emptyStringIsFalse, this.zeroIsFalse, this.formatter,
                                this.escaper, this.loader, this.collector, this.delims);
        }

        /** Returns a compiler that will use the given value for any variable that is missing, or
         * otherwise resolves to null. This is like {@link #nullValue} except that it returns the
         * supplied default for missing keys and existing keys that return null values. */
        public Compiler defaultValue (String defaultValue) {
            return new Compiler(this.standardsMode, defaultValue, true,
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
            return new Compiler(this.standardsMode, nullValue, false,
                                this.emptyStringIsFalse, this.zeroIsFalse, this.formatter,
                                this.escaper, this.loader, this.collector, this.delims);
        }

        /** Returns a compiler that will treat empty string as a false value if parameter is true. */
        public Compiler emptyStringIsFalse (boolean emptyStringIsFalse) {
            return new Compiler(this.standardsMode, this.nullValue, this.missingIsNull,
                                emptyStringIsFalse, this.zeroIsFalse, this.formatter,
                                this.escaper, this.loader, this.collector, this.delims);
        }

        /** Returns a compiler that will treat zero as a false value if parameter is true. */
        public Compiler zeroIsFalse (boolean zeroIsFalse) {
            return new Compiler(this.standardsMode, this.nullValue, this.missingIsNull,
                                this.emptyStringIsFalse, zeroIsFalse, this.formatter,
                                this.escaper, this.loader, this.collector, this.delims);
        }

        /** Configures the {@link Formatter} used to turn objects into strings. */
        public Compiler withFormatter (Formatter formatter) {
            return new Compiler(this.standardsMode, this.nullValue, this.missingIsNull,
                                this.emptyStringIsFalse, this.zeroIsFalse, formatter,
                                this.escaper, this.loader, this.collector, this.delims);
        }

        /** Configures the {@link Escaper} used to escape substituted text. */
        public Compiler withEscaper (Escaper escaper) {
            return new Compiler(this.standardsMode, this.nullValue, this.missingIsNull,
                                this.emptyStringIsFalse, this.zeroIsFalse, this.formatter,
                                escaper, this.loader, this.collector, this.delims);
        }

        /** Returns a compiler configured to use the supplied template loader to handle partials. */
        public Compiler withLoader (TemplateLoader loader) {
            return new Compiler(this.standardsMode, this.nullValue, this.missingIsNull,
                                this.emptyStringIsFalse, this.zeroIsFalse, this.formatter,
                                this.escaper, loader, this.collector, this.delims);
        }

        /** Returns a compiler configured to use the supplied collector. */
        public Compiler withCollector (Collector collector) {
            return new Compiler(this.standardsMode, this.nullValue, this.missingIsNull,
                                this.emptyStringIsFalse, this.zeroIsFalse, this.formatter,
                                this.escaper, this.loader, collector, this.delims);
        }

        /** Returns a compiler configured to use the supplied delims as default delimiters.
         * @param delims a string of the form {@code AB CD} or {@code A D} where A and B are
         * opening delims and C and D are closing delims. */
        public Compiler withDelims (String delims) {
            return new Compiler(this.standardsMode, this.nullValue, this.missingIsNull,
                                this.emptyStringIsFalse, this.zeroIsFalse, this.formatter,
                                this.escaper, this.loader, this.collector,
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
            return (emptyStringIsFalse && "".equals(value)) ||
                (zeroIsFalse && (value instanceof Number) && ((Number)value).longValue() == 0);
        }

        protected Compiler (boolean standardsMode, String nullValue, boolean missingIsNull,
                            boolean emptyStringIsFalse, boolean zeroIsFalse, Formatter formatter,
                            Escaper escaper, TemplateLoader loader, Collector collector,
                            Delims delims) {
            this.standardsMode = standardsMode;
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
    public interface Formatter
    {
        /** Converts {@code value} to a string for inclusion in a template. */
        String format (Object value);
    }

    /** Handles lambdas. */
    public interface Lambda
    {
        /** Executes this lambda on the supplied template fragment. The lambda should write its
         * results to {@code out}.
         *
         * @param frag the fragment of the template that was passed to the lambda.
         * @param out the writer to which the lambda should write its output.
         */
        void execute (Template.Fragment frag, Writer out) throws IOException;
    }

    /** Handles lambdas that are also invoked for inverse sections.. */
    public interface InvertibleLambda extends Lambda
    {
        /** Executes this lambda on the supplied template fragment, when the lambda is used in an
         * inverse section. The lambda should write its results to {@code out}.
         *
         * @param frag the fragment of the template that was passed to the lambda.
         * @param out the writer to which the lambda should write its output.
         */
        void executeInverse (Template.Fragment frag, Writer out) throws IOException;
    }

    /** Reads variables from context objects. */
    public interface VariableFetcher
    {
        /** Reads the so-named variable from the supplied context object. */
        Object get (Object ctx, String name) throws Exception;
    }

    /** Handles escaping characters in substituted text. */
    public interface Escaper
    {
        /** Returns {@code raw} with the appropriate characters replaced with escape sequences. */
        String escape (String raw);
    }

    /** Handles loading partial templates. */
    public interface TemplateLoader
    {
        /** Returns a reader for the template with the supplied name.
         * @throws Exception if the template could not be loaded for any reason. */
        Reader getTemplate (String name) throws Exception;
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

        /** Creates a map to be used to cache {@link VariableFetcher} instances. The GWT-compatible
         * collector returns a HashMap here, but the reflection based fetcher (which only works on
         * the JVM and Android, returns a concurrent hashmap. */
        <K,V> Map<K,V> createFetcherCache ();
    }

    /**
     * Returns a compiler that escapes HTML by default and does not use standards mode.
     */
    public static Compiler compiler () {
        return new Compiler(false, null, true, false, false, DEFAULT_FORMATTER, Escapers.HTML,
                            FAILING_LOADER, new DefaultCollector(), new Delims());
    }

    /**
     * Compiles the supplied template into a repeatedly executable intermediate form.
     */
    protected static Template compile (Reader source, Compiler compiler) {
        Accumulator accum = new Parser(compiler).parse(source);
        return new Template(accum.finish(), compiler);
    }

    private Mustache () {} // no instantiateski

    protected static void restoreStartTag (StringBuilder text, Delims starts) {
        text.insert(0, starts.start1);
        if (starts.start2 != NO_CHAR) {
            text.insert(1, starts.start2);
        }
    }

    protected static class LineEndingMode {
        protected static final int LF = 1;
        protected static final int CR = 2;
        protected static final int CRLF = 3;
    }

    // a hand-rolled parser; whee!
    protected static class Parser {

        // Parser current state
        protected static final int TEXT = 0;
        protected static final int MATCHING_START = 1;
        protected static final int MATCHING_END = 2;
        protected static final int TAG = 3;

        final Delims delims;
        final StringBuilder text = new StringBuilder();

        Reader source;
        Accumulator accum;

        int state = TEXT;
        int line = 1, column = 0;
        int lineEndingMode = LineEndingMode.LF;
        int numberOfSectionTagsInCurrentLine = 0;
        StringWrapper lastTextBufferSpanningCurrentLine = null;

        public Parser (Compiler compiler) {
            this.accum = new Accumulator(compiler);
            this.delims = compiler.delims.copy();
        }

        public Accumulator parse (Reader source) {
            this.source = source;

            tryGuessingLineEndingMode();

            int encodedChar;
            char currentChar;
            while ((encodedChar = nextChar()) > -1) {

                currentChar = (char) encodedChar;
                parseChar(currentChar);

                if (isLineTerminator(currentChar)) {
                    removeLineJustTerminatedIfItContainedOnlySectionTags();
                    column = 0;
                    line++;
                    numberOfSectionTagsInCurrentLine = 0;
                    lastTextBufferSpanningCurrentLine = null;
                } else {
                    column++;
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

            flushBufferIntoTextSegment();

            return accum;
        }

        /**
         * Improves template readability when we are not generating HTML and line ending is important.
         * For example if we are using Mustache for Java code generation (line endings represented
         * with \LINE_END, in source code they are \n, \r\n or \r)
         *
         * 1. public void copyProperties({{beanClass}} src, {{beanClass}} dst) {\LINE_END
         * 2.     {{props}}\LINE_END
         * 3.     dst.{{setter}}(src.{{getter}}());\LINE_END
         * 4.     {{/props}}\LINE_END
         * 5. }
         *
         * We want line 2 and 4 to disappear from the generated output. This method is run after the line
         * terminator character has been added to the current buffer. It checks if the current line contains
         * only whitespace and exactly one section (ie a tag whose name begins with ^, # or /) and if
         * that's the case it clears the current buffer and adjust any TextSegment before the section tag
         */
        private void removeLineJustTerminatedIfItContainedOnlySectionTags() {
            if (numberOfSectionTagsInCurrentLine == 1) {

                TextSegmentSpanningCurrentLineBeforeSectionTag textBeforeSectionTag
                        = new TextSegmentSpanningCurrentLineBeforeSectionTag();

                if (textBeforeSectionTag.isLastLineAllWhitespace() && isAllWhitespace(text.toString())) {
                    textBeforeSectionTag.truncateWhitespaceAfterLastLineEnding();
                    text.setLength(0);
                }
            }
        }

        private class TextSegmentSpanningCurrentLineBeforeSectionTag {

            public boolean isLastLineAllWhitespace() {
                return lastTextBufferSpanningCurrentLine == null || isAllWhitespace(lastLine());
            }

            private String lastLine() {
                return textSpansMultipleLines()
                        ? text().substring(lastLineStartIndex())
                        : text();
            }

            private int lastLineStartIndex() {
                return text().lastIndexOf(lineEndingStringInCurrentLineEndingMode()) + lineEndingStringInCurrentLineEndingMode().length();
            }

            private String text() {
                return lastTextBufferSpanningCurrentLine.getString();
            }

            private boolean textSpansMultipleLines() {
                return text().lastIndexOf(lineEndingStringInCurrentLineEndingMode()) > -1;
            }

            public void truncateWhitespaceAfterLastLineEnding() {
                if (lastTextBufferSpanningCurrentLine != null) {
                    lastTextBufferSpanningCurrentLine.setString(textSpansMultipleLines()
                            ? text().substring(0, lastLineStartIndex())
                            : "");
                }
            }
        }

        private boolean isAllWhitespace(CharSequence cs) {

            boolean isAllWhitespace = true;

            for (int i = cs.length() - 1; i >= 0; i--) {
                char c = cs.charAt(i);
                if (!Character.isWhitespace(c)) {
                    isAllWhitespace = false;
                    break;
                }
            }

            return isAllWhitespace;
        }

        private String lineEndingStringInCurrentLineEndingMode() {
            switch (lineEndingMode) {
                case LineEndingMode.CRLF: return "\r\n";
                case LineEndingMode.CR: return "\r";
                case LineEndingMode.LF: return "\n";
                default: throw new RuntimeException("Unknown line ending mode " + lineEndingMode);
            }
        }

        private boolean isLineTerminator(char c) {
            return c == '\n' && (lineEndingMode == LineEndingMode.CRLF || lineEndingMode == LineEndingMode.LF)
                    || c == '\r' && lineEndingMode == LineEndingMode.CR;
        }

        private void tryGuessingLineEndingMode() {
            if (source.markSupported()) {
                try {
                    source.mark(0);

                    boolean foundCR = false;
                    boolean foundLF = false;
                    char c;

                    int encodedChar;
                    while ((encodedChar = source.read()) > -1) {
                        c = (char) encodedChar;
                        if (c == '\r') {
                            foundCR = true;
                        } else if (c == '\n') {
                            foundLF = true;
                        } else if (foundCR || foundLF) {
                            break;
                        }
                    }

                    if (foundCR && foundLF) {
                        lineEndingMode = LineEndingMode.CRLF;
                    } else if (foundCR) {
                        lineEndingMode = LineEndingMode.CR;
                    } else if (foundLF) {
                        lineEndingMode = LineEndingMode.LF;
                    }

                    source.reset();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private StringWrapper flushBufferIntoTextSegment() {
            StringWrapper content = new StringWrapper(text.toString());
            lastTextBufferSpanningCurrentLine = content;
            accum.addTextSegment(content);
            text.setLength(0);
            return content;
        }

        protected void parseChar (char c) {
            switch (state) {
            case TEXT:
                if (c == delims.start1) {
                    state = MATCHING_START;
                    if (delims.start2 == NO_CHAR) {
                        parseChar(NO_CHAR);
                    }
                } else {
                    text.append(c);
                }
                break;

            case MATCHING_START:
                if (c == delims.start2) {
                    flushBufferIntoTextSegment();
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
                    flushBufferIntoTextSegment();
                    if (delims.start2 == NO_CHAR) {
                        flushBufferIntoTextSegment();
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
                        String tagText = text.toString();
                        accum = accum.addTagSegment(text, line);
                        switch (tagText.charAt(0)) {
                            case '/':
                            case '#':
                            case '^':
                                numberOfSectionTagsInCurrentLine++;
                                break;
                        }
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
        public Accumulator (Compiler compiler) {
            _comp = compiler;
        }

        public boolean justOpenedOrClosedBlock () {
            // return true if we just closed a block segment; we'll handle just opened elsewhere
            return (!_segs.isEmpty() && _segs.get(_segs.size()-1) instanceof BlockSegment);
        }

        public TextSegment addTextSegment (StringWrapper text) {
            TextSegment segment = new TextSegment(text);
            _segs.add(segment);
            return segment;
        }

        public Accumulator addTagSegment (final StringBuilder accum, final int tagLine) {
            final Accumulator outer = this;
            String tag = accum.toString().trim();
            final String tag1 = tag.substring(1).trim();
            accum.setLength(0);

            switch (tag.charAt(0)) {
            case '#':
                requireNoNewlines(tag, tagLine);
                return new Accumulator(_comp) {
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
                        outer._segs.add(new SectionSegment(_comp, itag, super.finish(), tagLine));
                        return outer;
                    }
                };

            case '>':
                _segs.add(new IncludedTemplateSegment(_comp, tag1));
                return this;

            case '^':
                requireNoNewlines(tag, tagLine);
                return new Accumulator(_comp) {
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
                        outer._segs.add(new InvertedSegment(_comp, itag, super.finish(), tagLine));
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
                _segs.add(new VariableSegment(tag1, tagLine, _comp.formatter, Escapers.NONE));
                return this;

            default:
                requireNoNewlines(tag, tagLine);
                _segs.add(new VariableSegment(tag, tagLine, _comp.formatter, _comp.escaper));
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

        protected final Compiler _comp;
        protected final List<Template.Segment> _segs = new ArrayList<Template.Segment>();
    }

    protected static class StringWrapper {

        private String str;

        public StringWrapper(String str) {
            this.str = str;
        }

        public void setString(String str) {
            this.str = str;
        }

        public String getString() {
            return str;
        }

    }

    /** A simple segment that reproduces a string verbatim. The actual string is wrapped to allow whitespace adjusting */
    protected static class TextSegment extends Template.Segment {
        public TextSegment(StringWrapper text) {
            _text = text;
        }
        @Override public void execute (Template tmpl, Template.Context ctx, Writer out) {
            write(out, _text.getString());
        }
        protected final StringWrapper _text;
    }

    protected static class IncludedTemplateSegment extends Template.Segment {
        public IncludedTemplateSegment (Compiler compiler, String name) {
            _comp = compiler;
            _name = name;
        }
        @Override public void execute (Template tmpl, Template.Context ctx, Writer out) {
            // we compile our template lazily to avoid infinie recursion if a template includes
            // itself (see issue #13)
            if (_template == null) {
                try {
                    _template = _comp.compile(_comp.loader.getTemplate(_name));
                } catch (Exception e) {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException)e;
                    } else {
                        throw new MustacheException("Unable to load template: " + _name, e);
                    }
                }
            }
            // we must take care to preserve our context rather than creating a new one, which
            // would happen if we just called execute() with ctx.data
            _template.executeSegs(ctx, out);
        }
        protected final Compiler _comp;
        protected final String _name;
        protected Template _template;
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
        public VariableSegment (String name, int line, Formatter formatter, Escaper escaper) {
            super(name, line);
            _formatter = formatter;
            _escaper = escaper;
        }
        @Override public void execute (Template tmpl, Template.Context ctx, Writer out)  {
            Object value = tmpl.getValueOrDefault(ctx, _name, _line);
            if (value == null) {
                throw new MustacheException.Context("No key, method or field with name '" + _name +
                                                    "' on line " + _line, _name, _line);
            }
            write(out, _escaper.escape(_formatter.format(value)));
        }
        protected final Formatter _formatter;
        protected final Escaper _escaper;
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
        public SectionSegment (Compiler compiler, String name, Template.Segment[] segs, int line) {
            super(name, segs, line);
            _comp = compiler;
        }
        @Override public void execute (Template tmpl, Template.Context ctx, Writer out)  {
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
                executeSegs(tmpl, ctx.nest(value, 0, false, false), out);
            }
        }
        protected final Compiler _comp;
    }

    /** A segment that represents an inverted section. */
    protected static class InvertedSegment extends BlockSegment {
        public InvertedSegment (Compiler compiler, String name, Template.Segment[] segs, int line) {
            super(name, segs, line);
            _comp = compiler;
        }
        @Override public void execute (Template tmpl, Template.Context ctx, Writer out)  {
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
        protected final Compiler _comp;
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
