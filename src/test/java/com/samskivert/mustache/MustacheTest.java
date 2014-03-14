//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Various unit tests.
 */
public class MustacheTest
{
    @Test public void testSimpleVariable () {
        test("bar", "{{foo}}", context("foo", "bar"));
    }

    @Test public void testFieldVariable () {
        test("bar", "{{foo}}", new Object() {
            String foo = "bar";
        });
    }

    @Test public void testMethodVariable () {
        test("bar", "{{foo}}", new Object() {
            String foo () { return "bar"; }
        });
    }

    @Test public void testPropertyVariable () {
        test("bar", "{{foo}}", new Object() {
            String getFoo () { return "bar"; }
        });
    }

    @Test public void testSkipVoidReturn () {
        test("bar", "{{foo}}", new Object() {
            void foo () {}
            String getFoo () { return "bar"; }
        });
    }

    @Test public void testPrimitiveArrayVariable () {
        test("1234", "{{#foo}}{{this}}{{/foo}}", new Object() {
            int[] getFoo () { return new int[] { 1, 2, 3, 4 }; }
        });
    }

    @Test public void testCallSiteReuse () {
        Template tmpl = Mustache.compiler().compile("{{foo}}");
        Object ctx = new Object() {
            String getFoo () { return "bar"; }
        };
        for (int ii = 0; ii < 50; ii++) {
            assertEquals("bar", tmpl.execute(ctx));
        }
    }

    @Test public void testCallSiteChange () {
        Template tmpl = Mustache.compiler().compile("{{foo}}");
        assertEquals("bar", tmpl.execute(new Object() {
            String getFoo () { return "bar"; }
        }));
        assertEquals("bar", tmpl.execute(new Object() {
            String foo = "bar";
        }));
    }

    @Test public void testOneShotSection () {
        test("baz", "{{#foo}}{{bar}}{{/foo}}", context("foo", context("bar", "baz")));
    }

    @Test public void testListSection () {
        test("bazbif", "{{#foo}}{{bar}}{{/foo}}", context(
                 "foo", Arrays.asList(context("bar", "baz"), context("bar", "bif"))));
    }

    @Test public void testListIndexSection() {
        test("baz", "{{#foo.0}}{{bar}}{{/foo.0}}", context(
            "foo", Arrays.asList(context("bar", "baz"), context("bar", "bif"))));
    }

    @Test public void testListItemSection() {
        test("baz", "{{foo.0.bar}}", context(
            "foo", Arrays.asList(context("bar", "baz"), context("bar", "bif"))));
    }

    @Test public void testArraySection () {
        test("bazbif", "{{#foo}}{{bar}}{{/foo}}",
             context("foo", new Object[] {
                     context("bar", "baz"), context("bar", "bif") }));
    }

    @Test public void testArrayIndexSection () {
        test("baz", "{{#foo.0}}{{bar}}{{/foo.0}}",
            context("foo", new Object[] {
                context("bar", "baz"), context("bar", "bif") }));
    }

    @Test public void testArrayItemSection () {
        test("baz", "{{foo.0.bar}}",
            context("foo", new Object[] {
                context("bar", "baz"), context("bar", "bif") }));
    }

    @Test public void testIteratorSection () {
        test("bazbif", "{{#foo}}{{bar}}{{/foo}}",
             context("foo", Arrays.asList(context("bar", "baz"),
                                          context("bar", "bif")).iterator()));
    }

    @Test public void testIteratorIndexSection () {
        test("baz", "{{#foo.0}}{{bar}}{{/foo.0}}",
            context("foo", Arrays.asList(context("bar", "baz"),
                context("bar", "bif")).iterator()));
    }

    @Test public void testIteratorItemSection () {
        test("baz", "{{foo.0.bar}}",
            context("foo", Arrays.asList(context("bar", "baz"),
                context("bar", "bif")).iterator()));
    }

    @Test public void testEmptyListSection () {
        test("", "{{#foo}}{{bar}}{{/foo}}", context("foo", Collections.emptyList()));
    }

    @Test public void testEmptyArraySection () {
        test("", "{{#foo}}{{bar}}{{/foo}}", context("foo", new Object[0]));
    }

    @Test public void testEmptyIteratorSection () {
        test("", "{{#foo}}{{bar}}{{/foo}}", context("foo", Collections.emptyList().iterator()));
    }

    @Test public void testFalseSection () {
        test("", "{{#foo}}{{bar}}{{/foo}}", context("foo", false));
    }

    @Test public void testNestedListSection () {
        test("1234", "{{#a}}{{#b}}{{c}}{{/b}}{{#d}}{{e}}{{/d}}{{/a}}",
             context("a", context("b", new Object[] { context("c", "1"), context("c", "2") },
                                  "d", new Object[] { context("e", "3"), context("e", "4") })));
    }

    @Test public void testNullSection () {
        test("", "{{#foo}}{{bar}}{{/foo}}", new Object() {
            Object foo = null;
        });
    }

    @Test public void testNullSectionWithDefaultValue () {
        test(Mustache.compiler().defaultValue(""), "", "{{#foo}}{{bar}}{{/foo}}", new Object() {
            Object foo = null;
        });
    }

    @Test public void testNullSectionWithNullValue () {
        test(Mustache.compiler().nullValue(""), "", "{{#foo}}{{bar}}{{/foo}}", new Object() {
            Object foo = null;
        });
    }

    @Test public void testSectionWithNonFalseyEmptyString () {
        test(Mustache.compiler(), "test", "{{#foo}}test{{/foo}}", new Object() {
            String foo = "";
        });
    }

    @Test public void testSectionWithFalseyEmptyString () {
        Object ctx = new Object() {
            String foo = "";
            String bar = "nonempty";
        };
        // test normal sections with falsey empty string
        Mustache.Compiler compiler = Mustache.compiler().emptyStringIsFalse(true);
        test(compiler, "",     "{{#foo}}test{{/foo}}", ctx);
        test(compiler, "test", "{{#bar}}test{{/bar}}", ctx);
        // test inverted sections with falsey empty string
        test(compiler, "test", "{{^foo}}test{{/foo}}", ctx);
        test(compiler, "",     "{{^bar}}test{{/bar}}", ctx);
    }

    @Test public void testSectionWithNonFalseyZero () {
        test(Mustache.compiler(), "test", "{{#foo}}test{{/foo}}", new Object() {
            Long foo = 0L;
        });
    }

    @Test public void testSectionWithFalseyZero () {
        test(Mustache.compiler().zeroIsFalse(true), "",
             "{{#intv}}intv{{/intv}}" +
             "{{#longv}}longv{{/longv}}" +
             "{{#floatv}}floatv{{/floatv}}" +
             "{{#doublev}}doublev{{/doublev}}" +
             "{{#longm}}longm{{/longm}}" +
             "{{#intm}}intm{{/intm}}" +
             "{{#floatm}}floatm{{/floatm}}" +
             "{{#doublem}}doublem{{/doublem}}",
             new Object() {
                 Integer intv = 0;
                 Long longv = 0L;
                 Float floatv = 0f;
                 Double doublev = 0d;
                 int intm () { return 0; }
                 long longm () { return 0l; }
                 float floatm () { return 0f; }
                 double doublem () { return 0d; }
             });
    }

    @Test public void testMissingSection () {
        test("", "{{#foo}}{{bar}}{{/foo}}", new Object() {
            // no foo
        });
    }

    @Test public void testMissingSectionWithDefaultValue () {
        test(Mustache.compiler().defaultValue(""), "", "{{#foo}}{{bar}}{{/foo}}", new Object() {
            // no foo
        });
    }

    @Test(expected=MustacheException.class)
    public void testMissingSectionWithNullValue () {
        test(Mustache.compiler().nullValue(""), "", "{{#foo}}{{bar}}{{/foo}}", new Object() {
            // no foo
        });
    }

    @Test public void testComment () {
        test("foobar", "foo{{! nothing to see here}}bar", new Object());
    }

    @Test public void testCommentWithFunnyChars() {
        test("foobar", "foo{{! {baz\n }}bar", new Object());
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPartialUseWhenUnconfigured () {
        test(null, "{{>foo}}", null);
    }

    @Test public void testPartial () {
        test(Mustache.compiler().withLoader(new Mustache.TemplateLoader() {
            public Reader getTemplate (String name) {
                if (name.equals("foo")) {
                    return new StringReader("inside:{{bar}}");
                } else {
                    return new StringReader("nonfoo");
                }
            }
        }), "foo inside:foo nonfoo foo", "{{bar}} {{>foo}} {{>baz}} {{bar}}", context("bar", "foo"));
    }

    @Test public void testPartialPlusNestedContext () {
        test(Mustache.compiler().withLoader(new Mustache.TemplateLoader() {
            public Reader getTemplate (String name) {
                if (name.equals("nested")) {
                    return new StringReader("{{name}}{{thing_name}}");
                } else {
                    return new StringReader("nonfoo");
                }
            }
        }), "foo((foobar)(foobaz))", "{{name}}({{#things}}({{>nested}}){{/things}})",
            context("name", "foo",
                    "things", Arrays.asList(context("thing_name", "bar"),
                                            context("thing_name", "baz"))));
    }

    @Test public void testDelimiterChange () {
        test("foo bar baz", "{{one}} {{=<% %>=}}<%two%><%={{ }}=%> {{three}}",
             context("one", "foo", "two", "bar", "three", "baz"));
        test("baz bar foo", "{{three}} {{=% %=}}%two%%={{ }}=% {{one}}",
             context("one", "foo", "two", "bar", "three", "baz"));
    }

    @Test public void testUnescapeHTML () {
        assertEquals("<b>", Mustache.compiler().escapeHTML(true).compile("{{&a}}").
                     execute(context("a", "<b>")));
        assertEquals("<b>", Mustache.compiler().escapeHTML(true).compile("{{{a}}}").
                     execute(context("a", "<b>")));
        // make sure these also work when escape HTML is off
        assertEquals("<b>", Mustache.compiler().escapeHTML(false).compile("{{&a}}").
                     execute(context("a", "<b>")));
        assertEquals("<b>", Mustache.compiler().escapeHTML(false).compile("{{{a}}}").
                     execute(context("a", "<b>")));
    }

    @Test public void testDanglingTag () {
        test("foo{", "foo{", context("a", "<b>"));
        test("foo{{", "foo{{", context("a", "<b>"));
        test("foo{{a", "foo{{a", context("a", "<b>"));
        test("foo{{a}", "foo{{a}", context("a", "<b>"));
    }

    @Test public void testStrayTagCharacters () {
        test("funny [b] business {{", "funny {{a}} business {{", context("a", "[b]"));
        test("funny <b> business {{", "funny {{{a}}} business {{", context("a", "<b>"));
        test("{{ funny [b] business", "{{ funny {{a}} business", context("a", "[b]"));
        test("{{ funny <b> business", "{{ funny {{{a}}} business", context("a", "<b>"));
        test("funny [b] business }}", "funny {{a}} business }}", context("a", "[b]"));
        test("funny <b> business }}", "funny {{{a}}} business }}", context("a", "<b>"));
        test("}} funny [b] business", "}} funny {{a}} business", context("a", "[b]"));
        test("}} funny <b> business", "}} funny {{{a}}} business", context("a", "<b>"));
    }

    @Test(expected=MustacheParseException.class)
    public void testInvalidUnescapeHTML () {
        Mustache.compiler().escapeHTML(true).compile("{{{a}}").execute(context("a", "<b>"));
    }

    @Test public void testCustomFormatter () {
        Mustache.Formatter fmt = new Mustache.Formatter() {
            public String format (Object value) {
                if (value instanceof Date) return _fmt.format((Date)value);
                else return String.valueOf(value);
            }
            protected SimpleDateFormat _fmt = new SimpleDateFormat("yyyy/MM/dd");
        };
        assertEquals("Date: 2014/01/08", Mustache.compiler().withFormatter(fmt).
                     compile("{{msg}}: {{today}}").execute(new Object() {
                         String msg = "Date";
                         Date today = new Date(1389208567874L);
                     }));
    }

    @Test public void testEscapeHTML () {
        assertEquals("&lt;b&gt;", Mustache.compiler().compile("{{a}}").
                     execute(context("a", "<b>")));
        assertEquals("<b>", Mustache.compiler().escapeHTML(false).compile("{{a}}").
                     execute(context("a", "<b>")));
    }

    @Test public void testUserDefinedEscaping() {
        Mustache.Escaper escaper = Escapers.simple(new String[][] {
            { "[", ":BEGIN:" },
            { "]", ":END:" }
        });
        assertEquals(":BEGIN:b:END:", Mustache.compiler().withEscaper(escaper).
                     compile("{{a}}").execute(context("a", "[b]")));
    }

    @Test public void testPartialDelimiterMatch () {
        assertEquals("{bob}", Mustache.compiler().compile("{bob}").execute(context()));
        assertEquals("bar", Mustache.compiler().compile("{{bob}bob}}").execute(
                         context("bob}bob", "bar")));
    }

    @Test public void testTopLevelThis () {
        assertEquals("bar", Mustache.compiler().compile("{{this}}").execute("bar"));
        assertEquals("bar", Mustache.compiler().compile("{{.}}").execute("bar"));
    }

    @Test public void testNestedThis () {
        assertEquals("barbazbif", Mustache.compiler().compile("{{#things}}{{this}}{{/things}}").
                     execute(context("things", Arrays.asList("bar", "baz", "bif"))));
        assertEquals("barbazbif", Mustache.compiler().compile("{{#things}}{{.}}{{/things}}").
                execute(context("things", Arrays.asList("bar", "baz", "bif"))));
    }

    @Test public void testCompoundVariable () {
        test("hello", "{{foo.bar.baz}}", new Object() {
            Object foo () {
                return new Object() {
                    Object bar = new Object() {
                        String baz = "hello";
                    };
                };
            }
        });
    }

    @Test(expected=MustacheException.class)
    public void testNullComponentInCompoundVariable () {
        test(Mustache.compiler(), "unused", "{{foo.bar.baz}}", new Object() {
            Object foo = new Object() {
                Object bar = null;
            };
        });
    }

    @Test(expected=MustacheException.class)
    public void testMissingComponentInCompoundVariable () {
        test(Mustache.compiler(), "unused", "{{foo.bar.baz}}", new Object() {
            Object foo = new Object(); // no bar
        });
    }

    @Test public void testNullComponentInCompoundVariableWithDefault () {
        test(Mustache.compiler().nullValue("null"), "null", "{{foo.bar.baz}}", new Object() {
            Object foo = null;
        });
        test(Mustache.compiler().nullValue("null"), "null", "{{foo.bar.baz}}", new Object() {
            Object foo = new Object() {
                Object bar = null;
            };
        });
    }

    @Test public void testMissingComponentInCompoundVariableWithDefault () {
        test(Mustache.compiler().defaultValue("?"), "?", "{{foo.bar.baz}}", new Object() {
            // no foo, no bar
        });
        test(Mustache.compiler().defaultValue("?"), "?", "{{foo.bar.baz}}", new Object() {
            Object foo = new Object(); // no bar
        });
    }

    @Test public void testNewlineSkipping () {
        String tmpl = "list:\n" +
            "{{#items}}\n" +
            "{{this}}\n" +
            "{{/items}}\n" +
            "{{^items}}\n" +
            "no items\n" +
            "{{/items}}\n" +
            "endlist";
        test("list:\n" +
             "one\n" +
             "two\n" +
             "three\n" +
             "endlist", tmpl, context("items", Arrays.asList("one", "two", "three")));
        test("list:\n" +
             "no items\n" +
             "endlist", tmpl, context("items", Collections.emptyList()));
    }

    @Test public void testNewlineNonSkipping () {
        // only when a section tag is by itself on a line should we absorb the newline following it
        String tmpl = "thing?: {{#thing}}yes{{/thing}}{{^thing}}no{{/thing}}\n" +
            "that's nice";
        test("thing?: yes\n" +
             "that's nice", tmpl, context("thing", true));
        test("thing?: no\n" +
             "that's nice", tmpl, context("thing", false));
    }

    @Test public void testNestedContexts () {
        test("foo((foobar)(foobaz))", "{{name}}({{#things}}({{name}}{{thing_name}}){{/things}})",
             context("name", "foo",
                     "things", Arrays.asList(context("thing_name", "bar"),
                                             context("thing_name", "baz"))));
    }

    @Test public void testShadowedContext () {
        test("foo((bar)(baz))", "{{name}}({{#things}}({{name}}){{/things}})",
             context("name", "foo",
                     "things", Arrays.asList(context("name", "bar"), context("name", "baz"))));
    }

    @Test public void testShadowedContextWithNull () {
        Mustache.Compiler comp = Mustache.compiler().nullValue("(null)");
        String tmpl = "{{foo}}{{#inner}}{{foo}}{{/inner}}", expect = "outer(null)";
        test(comp, expect, tmpl, new Object() {
            public String foo = "outer";
            public Object inner = new Object() {
                // this foo should shadow the outer foo even though it's null
                public String foo = null;
            };
        });
        // same as above, but with maps instead of objects
        test(comp, expect, tmpl, context("foo", "outer", "inner", context("foo", null)));
    }

    @Test public void testFirst () {
        test("foo|bar|baz", "{{#things}}{{^-first}}|{{/-first}}{{this}}{{/things}}",
             context("things", Arrays.asList("foo", "bar", "baz")));
    }

    @Test public void testLast () {
        test("foo|bar|baz", "{{#things}}{{this}}{{^-last}}|{{/-last}}{{/things}}",
             context("things", Arrays.asList("foo", "bar", "baz")));
    }

    @Test public void testFirstLast () {
        test("[foo]", "{{#things}}{{#-first}}[{{/-first}}{{this}}{{#-last}}]{{/-last}}{{/things}}",
             context("things", Arrays.asList("foo")));
        test("foo", "{{#things}}{{this}}{{^-last}}|{{/-last}}{{/things}}",
             context("things", Arrays.asList("foo")));
    }

    @Test public void testIndex () {
        test("123", "{{#things}}{{-index}}{{/things}}",
             context("things", Arrays.asList("foo", "bar", "baz")));
    }

    @Test public void testLineReporting () {
        String tmpl = "first line\n{{nonexistent}}\nsecond line";
        try {
            Mustache.compiler().compile(tmpl).execute(new Object());
            fail("Referencing a nonexistent variable should throw MustacheException");
        } catch (MustacheException e) {
            assertTrue(e.getMessage().contains("line 2"));
        }
    }

    @Test public void testStandardsModeWithNullValuesInLoop () {
        String tmpl = "first line\n{{#nonexistent}}foo\n{{/nonexistent}}\nsecond line";
        String result = Mustache.compiler().standardsMode(true).compile(tmpl).execute(new Object());
        assertEquals("first line\nsecond line", result);
    }

    @Test public void testStandardsModeWithNullValuesInInverseLoop () {
        String tmpl = "first line\n{{^nonexistent}}foo{{/nonexistent}} \nsecond line";
        String result = Mustache.compiler().standardsMode(true).compile(tmpl).execute(new Object());
        assertEquals("first line\nfoo \nsecond line", result);
    }

    @Test public void testStandardsModeWithDotValue () {
        String tmpl = "{{#foo}}:{{.}}:{{/foo}}";
        String result = Mustache.compiler().standardsMode(true).compile(tmpl).
            execute(Collections.singletonMap("foo", "bar"));
        assertEquals(":bar:", result);
    }

    @Test(expected=MustacheException.class)
    public void testStandardsModeWithNoParentContextSearching () {
        String tmpl = "{{#parent}}foo{{parentProperty}}bar{{/parent}}";
        String result = Mustache.compiler().standardsMode(true).compile(tmpl).
            execute(context("parent", new Object(),
                            "parentProperty", "bar"));
    }

    @Test(expected=MustacheException.class)
    public void testMissingValue () {
        test("n/a", "{{missing}} {{notmissing}}", context("notmissing", "bar"));
    }

    @Test public void testMissingValueWithDefault () {
        test(Mustache.compiler().defaultValue(""),
             "bar", "{{missing}}{{notmissing}}", context("notmissing", "bar"));
    }

    @Test public void testMissingValueWithDefaultNonEmptyString () {
        test(Mustache.compiler().defaultValue("foo"),
             "foobar", "{{missing}}{{notmissing}}", context("notmissing", "bar"));
    }

    @Test public void testMissingValueWithDefaultSubstitution () {
        test(Mustache.compiler().defaultValue("?{{name}}?"),
             "?missing?bar", "{{missing}}{{notmissing}}", context("notmissing", "bar"));
    }

    @Test public void testMissingValueWithDefaultSubstitution2 () {
        test(Mustache.compiler().defaultValue("{{{{name}}}}"),
             "{{missing}}bar", "{{missing}}{{notmissing}}", context("notmissing", "bar"));
    }

    @Test public void testMissingValueWithDefaultSubstitution3 () {
        test(Mustache.compiler().defaultValue("{{?{{name}}?}}"),
             "{{?missing?}}bar", "{{missing}}{{notmissing}}", context("notmissing", "bar"));
    }

    @Test public void testNullValueGetsDefault () {
        test(Mustache.compiler().defaultValue("foo"),
             "foobar", "{{nullvar}}{{nonnullvar}}", new Object() {
                 String nonnullvar = "bar";
                 String nullvar = null;
             });
    }

    @Test(expected=MustacheException.class)
    public void testMissingValueWithNullDefault () {
        test(Mustache.compiler().nullValue(""),
             "bar", "{{missing}}{{notmissing}}", new Object() {
                 String notmissing = "bar";
                 // no field or method for 'missing'
             });
    }

    @Test public void testInvalidTripleMustache () {
        try {
            Mustache.compiler().compile("{{{foo}}");
            fail("Expected MustacheParseException");
        } catch (MustacheParseException e) {
            assertEquals("Invalid triple-mustache tag: {{{foo}} @ line 1", e.getMessage());
        }
        try {
            Mustache.compiler().compile("{{{foo}}]");
            fail("Expected MustacheParseException");
        } catch (MustacheParseException e) {
            assertEquals("Invalid triple-mustache tag: {{{foo}}] @ line 1", e.getMessage());
        }
    }

    @Test public void testNullValueGetsNullDefault () {
        test(Mustache.compiler().nullValue("foo"),
             "foobar", "{{nullvar}}{{nonnullvar}}", new Object() {
                 String nonnullvar = "bar";
                 String nullvar = null;
             });
    }

    @Test public void testCompilingDoesntChangeCompilersDelimiters() {
        Mustache.Compiler compiler = Mustache.compiler();
        test(compiler,
             "value", "{{=<% %>=}}<% variable %>", new Object() {
                 String variable = "value";
             });
        test(compiler,
             "value", "{{=<% %>=}}<% variable %>", new Object() {
                 String variable = "value";
             });
    }

    @Test public void testLambda1 () {
        test("<b>Willy is awesome.</b>", "{{#bold}}{{name}} is awesome.{{/bold}}",
             context("name", "Willy", "bold", new Mustache.Lambda() {
                 public void execute (Template.Fragment frag, Writer out) throws IOException {
                     out.write("<b>");
                     frag.execute(out);
                     out.write("</b>");
                 }
             }));
    }

    @Test public void testLambda2 () {
        test("Slug bug potato!", "{{#l}}1{{/l}} {{#l}}2{{/l}} {{#l}}{{three}}{{/l}}",
             context("three", "3", "l", new Mustache.Lambda() {
                 public void execute (Template.Fragment frag, Writer out) throws IOException {
                     out.write(lookup(frag.execute()));
                 }
                 protected String lookup (String contents) {
                     if (contents.equals("1")) return "Slug";
                     else if (contents.equals("2")) return "bug";
                     else if (contents.equals("3")) return "potato!";
                     else return "Who was that man?";
                 }
             }));
    }

    @Test public void testInvertibleLambda () {
        test("positive = positive, negative = negative, simple lambdas do still work",
             "{{#invertible}}positive{{/invertible}}, {{^invertible}}negative{{/invertible}}, " +
             "simple lambdas do {{^simple}}NOT {{/simple}}still work",
             context("invertible", new Mustache.InvertibleLambda() {
                 public void execute (Template.Fragment frag, Writer out) throws IOException {
                     out.write("positive = ");
                     frag.execute(out);
                 }
                 public void executeInverse (Template.Fragment frag, Writer out) throws IOException {
                     out.write("negative = ");
                     frag.execute(out);
                 }
             }, "simple", new Mustache.Lambda() {
                 public void execute (Template.Fragment frag, Writer out) throws IOException {
                     frag.execute(out);
                 }
             }));
    }

    @Test public void testLambdaWithContext () {
        test("a in l1, a in l2", "{{#l1}}{{a}}{{/l1}}, {{#l2}}{{a}}{{/l2}}",
             context("l1", new Mustache.Lambda() {
                     public void execute(Template.Fragment frag, Writer out) throws IOException {
                         frag.execute(context("a", "a in l1"), out);
                     }
                 }, "l2", new Mustache.Lambda() {
                     public void execute(Template.Fragment frag, Writer out) throws IOException {
                         frag.execute(context("a", "a in l2"), out);
                     }
                 }
             ));
    }

    @Test public void testNonStandardDefaultDelims () {
        test(Mustache.compiler().withDelims("<% %>"), "bar", "<%foo%>", new Object() {
            String foo = "bar";
        });
    }

    protected void test (Mustache.Compiler compiler, String expected, String template, Object ctx) {
        assertEquals(expected, compiler.compile(template).execute(ctx));
    }

    protected void test (String expected, String template, Object ctx) {
        test(Mustache.compiler(), expected, template, ctx);
    }

    protected Object context (Object... data) {
        Map<String, Object> ctx = new HashMap<String, Object>();
        for (int ii = 0; ii < data.length; ii += 2) {
            ctx.put(data[ii].toString(), data[ii+1]);
        }
        return ctx;
    }
}
