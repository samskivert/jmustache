//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache;

import static java.util.Map.entry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.samskivert.mustache.specs.SpecTest;

/**
 * Vestige from when JMustache supported both GWT and JVM.
 */
public abstract class SharedTests
{
    @Test public void testSimpleVariable () {
        test("bar", "{{foo}}", context("foo", "bar"));
    }

    @Test public void testPrimitiveArrayVariable () {
        test("1234", "{{#foo}}{{this}}{{/foo}}", context("foo", new int[] { 1, 2, 3, 4 }));
    }

    @Test public void testPrimitiveArrayIndexVariable () {
        test("1", "{{foo.0}}", context("foo", new int[] { 1, 2, 3, 4 }));
    }

    @Test public void testPrimitiveArrayIndexOutOfBoundsVariable () {
        Mustache.Compiler comp = Mustache.compiler().defaultValue("?");
        test(comp, "?", "{{foo.4}}", context("foo", new int[] { 1, 2, 3, 4 }));
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

    @Test public void testListIndexOutOfBoundsSection () {
        test("", "{{#foo.2}}{{bar}}{{/foo.2}}", context(
                 "foo", Arrays.asList(context("bar", "baz"), context("bar", "bif"))));
    }

    @Test public void testListItemSection () {
        test("baz", "{{foo.0.bar}}", context(
            "foo", Arrays.asList(context("bar", "baz"), context("bar", "bif"))));
    }

    @Test public void testMapEntriesSection () {
        Map<String,String> data = new HashMap<String,String>();
        data.put("k1", "v1");
        data.put("k2", "v2");
        test(Mustache.compiler().escapeHTML(false),
             "k1=v1k2=v2", "{{#map.entrySet}}{{.}}{{/map.entrySet}}",
             context("map", data));
    }

    @Test public void testArraySection () {
        test("bazbif", "{{#foo}}{{bar}}{{/foo}}",
             context("foo", new Object[] { context("bar", "baz"), context("bar", "bif") }));
    }

    @Test public void testArrayIndexSection () {
        test("baz", "{{#foo.0}}{{bar}}{{/foo.0}}",
            context("foo", new Object[] {
                context("bar", "baz"), context("bar", "bif") }));
    }

    @Test public void testArrayIndexOutOfBoundsSection () {
        test("", "{{#foo.2}}{{bar}}{{/foo.2}}",
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

    @Test public void testIteratorIndexOutOfBoundsSection () {
        test("", "{{#foo.2}}{{bar}}{{/foo.2}}",
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
        Object ctx = context("foo", null);
        test("", "{{#foo}}{{bar}}{{/foo}}", ctx);
        test(Mustache.compiler().defaultValue(""), "", "{{#foo}}{{bar}}{{/foo}}", ctx);
        test(Mustache.compiler().nullValue(""), "", "{{#foo}}{{bar}}{{/foo}}", ctx);
    }

    @Test public void testMissingNonStrictSection () {
        // no foo; section omitted due to non-strict-sections
        test("", "{{#foo}}{{bar}}{{/foo}}", EMPTY);
        // no foo; no exception because nullValue does change section strictness
        test(Mustache.compiler().nullValue(""), "", "{{#foo}}{{bar}}{{/foo}}", EMPTY);
        // no foo; no exception because defaultValue does change section strictness
        test(Mustache.compiler().defaultValue(""), "", "{{#foo}}{{bar}}{{/foo}}", EMPTY);
    }

    @Test public void testMissingStrictSection () {
        try {
            test(Mustache.compiler().strictSections(true), "", "{{#foo}}{{bar}}{{/foo}}",
                 EMPTY); // no foo; should throw exception due to strict-sections
            fail();
        } catch (MustacheException me) {} // expected
    }

    @Test public void testMissingStrictSectionNullValue () {
        try {
            // missing strict-sections always throw regardless of nullValue()
            test(Mustache.compiler().strictSections(true).nullValue(""), "",
                 "{{#foo}}{{bar}}{{/foo}}", EMPTY);
            fail();
        } catch (MustacheException me) {} // expected
    }

    @Test public void testMissingStrictSectionDefaultValue () {
        try {
            // missing strict-sections always throw regardless of defaultValue()
            test(Mustache.compiler().strictSections(true).defaultValue(""), "",
                 "{{#foo}}{{bar}}{{/foo}}", EMPTY);
            fail();
        } catch (MustacheException me) {} // expected
    }

    @Test public void testSectionWithNonFalseyEmptyString () {
        test(Mustache.compiler(), "test", "{{#foo}}test{{/foo}}", context("foo", ""));
    }

    @Test public void testSectionWithFalseyEmptyString () {
        Object ctx = context("foo", "", "bar", "nonempty");
        // test normal sections with falsey empty string
        Mustache.Compiler compiler = Mustache.compiler().emptyStringIsFalse(true);
        test(compiler, "",     "{{#foo}}test{{/foo}}", ctx);
        test(compiler, "test", "{{#bar}}test{{/bar}}", ctx);
        // test inverted sections with falsey empty string
        test(compiler, "test", "{{^foo}}test{{/foo}}", ctx);
        test(compiler, "",     "{{^bar}}test{{/bar}}", ctx);
    }

    @Test public void testComment () {
        test("foobar", "foo{{! nothing to see here}}bar", EMPTY);
    }

    @Test public void testCommentWithFunnyChars() {
        test("foobar", "foo{{! {baz\n }}bar", EMPTY);
    }

    @Test public void testPartialUseWhenUnconfigured () {
        try {
            test(null, "{{>foo}}", null);
            fail();
        } catch (UnsupportedOperationException uoe) {} // expected
    }

    Map<String,String> partials = new LinkedHashMap<>();

    @SafeVarargs
    protected final Mustache.TemplateLoader partials(Map.Entry<String, String> ... entries) {
        Map<String,String> templates = new LinkedHashMap<>();
        for (Entry<String, String> e : entries) {
            templates.put(e.getKey(), e.getValue());
        }
        partials = templates;
        return name -> new StringReader(templates.get(name));
    }

    @Test public void testPartial () {
        test(Mustache.compiler().withLoader(
                partials(entry("foo", "inside:{{bar}}"),
                        entry("baz", "nonfoo")))
                , "foo inside:foo nonfoo foo", "{{bar}} {{>foo}} {{>baz}} {{bar}}", context("bar", "foo"));
    }

    @Test public void testPartialIndent () {
        test(Mustache.compiler().withLoader(new Mustache.TemplateLoader() {
            public Reader getTemplate (String name) {
                return new StringReader("|\n{{{content}}}\n|\n");
            }
        }), "\\\n |\n <\n->\n |\n/\n", "\\\n {{>partial}}\n/\n", context("content", "<\n->"));
    }

    @Test public void testPartialBlankLines () {
        test(Mustache.compiler().withLoader(new Mustache.TemplateLoader() {
            public Reader getTemplate (String name) {
                return new StringReader("|\na\n\nb\n|\n");
            }
        }), "\\\n\t|\n\ta\n\t\n\tb\n\t|\n/\n", "\\\n\t{{>partial}}\n/\n", context());
    }

    @Test public void testNestedPartialBlankLines () {
        test(Mustache.compiler().withLoader(new Mustache.TemplateLoader() {
            public Reader getTemplate (String name) {
                if (name.equals("partial")) {
                    return new StringReader("1\n\t{{>nest}}\n1\n");
                } else {
                    return new StringReader("2\na\n\nb\n2\n");
                }
            }
        }), "\\\n\t1\n\t\t2\n\t\ta\n\t\t\n\t\tb\n\t\t2\n\t1\n/\n", "\\\n\t{{>partial}}\n/\n", context());
    }

    @Test public void testNestedPartialBlankLinesCRLF () {
        test(Mustache.compiler().withLoader(new Mustache.TemplateLoader() {
            public Reader getTemplate (String name) {
                if (name.equals("partial")) {
                    return new StringReader("1\r\n\t{{>nest}}\r\n1\r\n");
                } else {
                    return new StringReader("2\r\na\r\n\r\nb\r\n2\r\n");
                }
            }
        }), "\\\r\n\t1\r\n\t\t2\r\n\t\ta\r\n\t\t\r\n\t\tb\r\n\t\t2\r\n\t1\r\n/\r\n", "\\\r\n\t{{>partial}}\r\n/\r\n", context());
    }

    @Test public void testNestedPartialIndent () {
        Mustache.TemplateLoader loader = partials(entry("partial", "1\n {{>nest}}\n1\n"), entry("nest", "2\n{{{content}}}\n2\n"));
        test(Mustache.compiler().withLoader(loader),
                "|\n 1\n  2\n  <\n->\n  2\n 1\n|\n",
                "|\n {{>partial}}\n|\n", context("content", "<\n->"));
    }

    @Test public void testPartialIndentWithVariableAtTheStart () {
        test(Mustache.compiler().withLoader(new Mustache.TemplateLoader() {
            public Reader getTemplate (String name) {
                    return new StringReader("{{{content}}}\n|\n");
            }
        }), "\\\n <\n->\n |\n/\n", "\\\n {{>partial}}\n/\n", context("content", "<\n->"));
    }

    @Test public void testPartialIndentWithBlock () {
        test(Mustache.compiler().withLoader(new Mustache.TemplateLoader() {
            public Reader getTemplate (String name) {
                    return new StringReader("|\n{{#show}}\n{{{content}}}{{/show}}\n|\n");
            }
        }), "\\\n |\n <\n->\n |\n/\n", "\\\n {{>partial}}\n/\n", context("show", true, "content", "<\n->"));
    }
    @Test public void testPartialIndentInBlock () {
        test(Mustache.compiler().withLoader(new Mustache.TemplateLoader() {
            public Reader getTemplate (String name) {
                    return new StringReader("content");
            }
        }), " content", "{{#show}}\n {{>partial}}\n{{/show}}\n", context("show", true));
    }

    @Test public void testPartialIndentWithBlockAtStart () {
        test(Mustache.compiler().withLoader(new Mustache.TemplateLoader() {
            public Reader getTemplate (String name) {
                    return new StringReader("{{#show}}\n{{{content}}}{{/show}}\n|\n");
            }
        }), "\\\n <\n->\n |\n/\n", "\\\n {{>partial}}\n/\n", context("show", true, "content", "<\n->"));
    }

    @Test public void testPartialIndentWithInlineBlock () {
        test(Mustache.compiler().withLoader(new Mustache.TemplateLoader() {
            public Reader getTemplate (String name) {
                    return new StringReader("line {{#show}}content{{/show}}\n");
            }
        }), "\\\n line content\n/\n", "\\\n {{>partial}}\n/\n", context("show", true));
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

    @Test public void testRecursivePartial () {
        String template = "[{{name}}{{#properties}}, {{> schema.mustache}}{{/properties}}]";
        test(Mustache.compiler().withLoader(new Mustache.TemplateLoader() {
            public Reader getTemplate (String name) {
                return new StringReader(template);
            }
        }), "[level0, [level1a, [level2a]], [level1b, [level2b]]]", template,
             context("name", "level0",
                     "properties", Arrays.asList(
            context("name", "level1a",
                    // test with empty list as recursive terminator
                    "properties", context("name", "level2a", "properties", Arrays.asList())),
            context("name", "level1b",
                    // test with null as recursive terminator
                    "properties", context("name", "level2b", "properties", null))
        )));
    }

    @Test public void testDelimiterChange () {
        test("foo bar baz", "{{one}} {{=<% %>=}}<%two%><%={{ }}=%> {{three}}",
             context("one", "foo", "two", "bar", "three", "baz"));
        test("baz bar foo", "{{three}} {{=% %=}}%two%%={{ }}=% {{one}}",
             context("one", "foo", "two", "bar", "three", "baz"));
    }

    @Test public void testUnescapeHTML () {
        check("<b>", Mustache.compiler().escapeHTML(true).compile("{{&a}}").
              execute(context("a", "<b>")));
        check("<b>", Mustache.compiler().escapeHTML(true).compile("{{{a}}}").
              execute(context("a", "<b>")));
        // make sure these also work when escape HTML is off
        check("<b>", Mustache.compiler().escapeHTML(false).compile("{{&a}}").
              execute(context("a", "<b>")));
        check("<b>", Mustache.compiler().escapeHTML(false).compile("{{{a}}}").
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

    @Test public void testInvalidUnescapeHTML () {
        try {
            Mustache.compiler().escapeHTML(true).compile("{{{a}}").execute(context("a", "<b>"));
            fail();
        } catch (MustacheParseException me) {} // expected
    }

    @Test public void testEscapeHTML () {
        check("&lt;b&gt;", Mustache.compiler().compile("{{a}}").execute(context("a", "<b>")));
        check("<b>", Mustache.compiler().escapeHTML(false).compile("{{a}}").
              execute(context("a", "<b>")));
        // ensure that some potential XSS enablers are escaped
        check("&#x60;&#x3D;", Mustache.compiler().compile("{{a}}").execute(context("a", "`=")));
    }

    @Test public void testUserDefinedEscaping() {
        Mustache.Escaper escaper = Escapers.simple(new String[][] {
            { "[", ":BEGIN:" },
            { "]", ":END:" }
        });
        check(":BEGIN:b:END:", Mustache.compiler().withEscaper(escaper).
              compile("{{a}}").execute(context("a", "[b]")));
    }

    @Test public void testPartialDelimiterMatch () {
        check("{bob}", Mustache.compiler().compile("{bob}").execute(EMPTY));
        check("bar", Mustache.compiler().compile("{{bob}bob}}").execute(context("bob}bob", "bar")));
    }

    @Test public void testTopLevelThis () {
        check("bar", Mustache.compiler().compile("{{this}}").execute("bar"));
        check("bar", Mustache.compiler().compile("{{.}}").execute("bar"));
    }

    @Test public void testNestedThis () {
        check("barbazbif", Mustache.compiler().compile("{{#things}}{{this}}{{/things}}").
              execute(context("things", Arrays.asList("bar", "baz", "bif"))));
        check("barbazbif", Mustache.compiler().compile("{{#things}}{{.}}{{/things}}").
              execute(context("things", Arrays.asList("bar", "baz", "bif"))));
    }

    @Test public void testNestedNullThis () {
        check("bar!bif", Mustache.compiler().defaultValue("!").
              compile("{{#things}}{{.}}{{/things}}").
              execute(context("things", Arrays.asList("bar", null, "bif"))));
    }

    @Test public void testNewlineSkipping () {
        testNewlineSkipping("\n");
    }

    @Test public void testNewlineSkippingCRLF () {
        testNewlineSkipping("\r\n");
    }

    @Test public void testNewlineSkippingDelimsTag () {
        test("Begin.\nEnd.\n", "Begin.\n{{=@ @=}}\nEnd.\n", EMPTY);
    }

    @Test public void testNoTrimNewlineFromNestedTagAt0 () {
        test(" | \n  | \n", " | {{^boolean}}{{! comment }}\n {{/boolean}} | \n",
             context("boolean", false));
    }

    @Test public void testTrimBlank () {
        Mustache.StringSegment str = new Mustache.StringSegment("  \r\n  ", false);
        check("Text(  )-1/0", str.trimLeadBlank().toString());
        check("Text(  \\r\\n)3/-1", str.trimTrailBlank().toString());
    }

    static class GetKeysVisitor implements Mustache.Visitor {
        public final Set<String> keys = new HashSet<String>();
        public void visitText (String text) {}
        public void visitVariable (String name) { keys.add(name); }
        public boolean visitInclude (String name) { keys.add(name); return true; }
        public boolean visitSection (String name) { keys.add(name); return true; }
        public boolean visitInvertedSection (String name) { keys.add(name); return true; }
    }

    @Test public void testVisit() {
        String template = "{{#one}}1{{/one}} {{^two}}2{{three}}{{/two}}{{four}}";
        GetKeysVisitor viz = new GetKeysVisitor();
        Mustache.compiler().compile(template).visit(viz);
        List<String> expect = Arrays.asList("one", "two", "three", "four");
        assertEquals(new HashSet<>(expect), viz.keys);
    }

    @Test public void testVisitNested() {
        String template = "{{#one}}1{{/one}} {{^two}}" +
            "2{{two_and_half}}{{#five}}{{three}}{{/five}}" +
            "{{/two}} {{#one}}{{three}}{{/one}} {{four}}{{! ignore me }}";
        GetKeysVisitor viz = new GetKeysVisitor();
        Mustache.compiler().compile(template).visit(viz);
        List<String> expect = Arrays.asList("one", "two", "three", "four", "two_and_half", "five");
        assertEquals(new HashSet<>(expect), viz.keys);
    }

    protected void testNewlineSkipping (String sep) {
        String tmpl = "list:" + sep +
            "{{#items}}" + sep +
            "{{this}}" + sep +
            "{{/items}}" + sep +
            "{{^items}}" + sep +
            "no items" + sep +
            "{{/items}}" + sep +
            "endlist";
        test("list:" + sep +
             "one" + sep +
             "two" + sep +
             "three" + sep +
             "endlist", tmpl, context("items", Arrays.asList("one", "two", "three")));
        test("list:" + sep +
             "no items" + sep +
             "endlist", tmpl, context("items", Collections.emptyList()));

        // this tests newline trimming even if the group tags have leading/trailing whitespace
        String htmlTmpl =
            "<ul>" + sep +
            "  {{#items}}  " + sep +
            "  <li>{{this}}</li>" + sep +
            "  {{/items}}  " + sep +
            "  {{^items}}" + sep +
            "  <li>no items</li>" + sep +
            "  {{/items}}" + sep +
            "</ul>";
        test("<ul>" + sep +
             "  <li>one</li>" + sep +
             "  <li>two</li>" + sep +
             "  <li>three</li>" + sep +
             "</ul>", htmlTmpl, context("items", Arrays.asList("one", "two", "three")));
        test("<ul>" + sep +
             "  <li>no items</li>" + sep +
             "</ul>", htmlTmpl, context("items", Collections.emptyList()));
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

    @Test public void testFirstLastCombo () {
        test("FIRST_and_LAST", "{{#things}}{{#-first}}FIRST{{/-first}}{{this}}{{#-last}}LAST{{/-last}}{{/things}}",
             context("things", Arrays.asList("_and_")));
    }

    @Test public void testInverseFirstLastCombo () {
        test("_and_", "{{#things}}{{^-first}}NOT-FIRST{{/-first}}{{this}}{{^-last}}NOT-LAST{{/-last}}{{/things}}",
             context("things", Arrays.asList("_and_")));
    }

    @Test public void testNotFirst () {
        test("1,2,3", "{{#things}}{{^-first}},{{/-first}}{{this}}{{/things}}",
             context("things", Arrays.asList("1", "2", "3")));
    }

    @Test public void testNotLast () {
        test("1,2,3", "{{#things}}{{this}}{{^-last}},{{/-last}}{{/things}}",
             context("things", Arrays.asList("1", "2", "3")));
    }

    @Test public void testIndex () {
        test("123", "{{#things}}{{-index}}{{/things}}",
             context("things", Arrays.asList("foo", "bar", "baz")));
    }

    @Test public void testNestedIndex () {
        String tmpl =
            "{{#fooList}}\n" +
            "{{#quantity}}|q{{-index}}={{quantity}}{{/quantity}}|{{name}}\n" +
            "{{/fooList}}";
        test("|q1=1|a\n|b\n", tmpl,
             context("fooList", Arrays.asList(context("name", "a", "quantity", 1),
                                              context("name", "b"))));
    }

    @Test public void testLineReporting () {
        String tmpl = "first line\n{{nonexistent}}\nsecond line";
        try {
            Mustache.compiler().compile(tmpl).execute(EMPTY);
            fail("Referencing a nonexistent variable should throw MustacheException");
        } catch (MustacheException e) {
            assertTrue(e.getMessage().contains("line 2"));
        }
    }

    @Test public void testStandardsModeWithNullValuesInLoop () {
        test("first line\nsecond line",
             "first line\n{{#nullvalue}}foo\n{{/nullvalue}}\nsecond line",
             context("nullvalue", null));
    }

    @Test public void testStandardsModeWithNullValuesInInverseLoop () {
        test("first line\nfoo \nsecond line",
             "first line\n{{^nullvalue}}foo{{/nullvalue}} \nsecond line",
             context("nullvalue", null));
    }

    @Test public void testStandardsModeWithDotValue () {
        String tmpl = "{{#foo}}:{{.}}:{{/foo}}";
        String result = Mustache.compiler().standardsMode(true).compile(tmpl).
            execute(Collections.singletonMap("foo", "bar"));
        check(":bar:", result);
    }

    @Test public void testStandardsModeWithNoParentContextSearching () {
        try {
            String tmpl = "{{#parent}}foo{{parentProperty}}bar{{/parent}}";
            String result = Mustache.compiler().standardsMode(true).compile(tmpl).
                execute(context("parent", EMPTY,
                                "parentProperty", "bar"));
            fail();
        } catch (MustacheException me) {} // expected
    }

    @Test public void testMissingValue () {
        try {
            test("n/a", "{{missing}} {{notmissing}}", context("notmissing", "bar"));
            fail();
        } catch (MustacheException me) {} // expected
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
             "foobar", "{{nullvar}}{{nonnullvar}}", context("nonnullvar", "bar", "nullvar", null));
    }

    @Test public void testMissingValueWithNullDefault () {
        try {
            Object ctx = context("notmissing", "bar");
            // no field or method for 'missing'
            test(Mustache.compiler().nullValue(""), "bar", "{{missing}}{{notmissing}}", ctx);
            fail();
        } catch (MustacheException me) {} // expected
    }

    @Test public void testInvalidTripleMustache () {
        try {
            Mustache.compiler().compile("{{{foo}}");
            fail("Expected MustacheParseException");
        } catch (MustacheParseException e) {
            check("Invalid triple-mustache tag: {{{foo}} @ line 1", e.getMessage());
        }
        try {
            Mustache.compiler().compile("{{{foo}}]");
            fail("Expected MustacheParseException");
        } catch (MustacheParseException e) {
            check("Invalid triple-mustache tag: {{{foo}}] @ line 1", e.getMessage());
        }
    }

    @Test public void testNullValueGetsNullDefault () {
        test(Mustache.compiler().nullValue("foo"),
             "foobar", "{{nullvar}}{{nonnullvar}}", context("nonnullvar", "bar", "nullvar", null));
    }

    @Test public void testCompilingDoesntChangeCompilersDelimiters() {
        Mustache.Compiler compiler = Mustache.compiler();
        Object ctx = context("variable", "value");
        test(compiler, "value", "{{=<% %>=}}<% variable %>", ctx);
        test(compiler, "value", "{{=<% %>=}}<% variable %>", ctx);
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
        test("a in l1, a in l2", "{{#l1}}{{a}}{{/l1}}, {{#l2}}{{a}}{{/l2}}", context(
                 "l1", new Mustache.Lambda() {
                     public void execute (Template.Fragment frag, Writer out) throws IOException {
                         frag.execute(context("a", "a in l1"), out);
                     }
                 },
                 "l2", new Mustache.Lambda() {
                     public void execute (Template.Fragment frag, Writer out) throws IOException {
                         frag.execute(context("a", "a in l2"), out);
                     }
                 }));
    }

    @Test public void testLambdaDecompile () {
        test("Foo {{a}}, Bar {{a}}", "{{#lam}}Foo {{a}}{{/lam}}, {{#lam}}Bar {{a}}{{/lam}}",
             context("lam", new Mustache.Lambda() {
               public void execute (Template.Fragment frag, Writer out) throws IOException {
                   out.write(frag.decompile());
               }
             }));
        // whitespace inside tags is dropped!
        test("Foo {{a}}, Bar {{a}}", "{{#lam}}Foo {{a}}{{/lam}}, {{#lam}}Bar {{ a }}{{/lam}}",
             context("lam", new Mustache.Lambda() {
               public void execute (Template.Fragment frag, Writer out) throws IOException {
                   out.write(frag.decompile());
               }
             }));
        // custom delimiters are ignored!
        test("Foo {{a}}, Bar {{a}}",
             "{{#lam}}Foo {{a}}{{/lam}}, {{=(( ))=}}((#lam))Bar ((a))((/lam))",
             context("lam", new Mustache.Lambda() {
               public void execute (Template.Fragment frag, Writer out) throws IOException {
                   out.write(frag.decompile());
               }
             }));
        // coalesced whitespace around section tags is preserved
        test("{{#section}}\n" +
             "{{a}}\n" +
             "{{/section}}",
             "{{#lam}}{{#section}}\n" +
             "{{a}}\n" +
             "{{/section}}{{/lam}}",
             context("lam", new Mustache.Lambda() {
               public void execute (Template.Fragment frag, Writer out) throws IOException {
                   out.write(frag.decompile());
               }
             }));
    }

    @Test public void testLambdaFragExecTemplate () {
        Template a = Mustache.compiler().compile("{{value}} should be A");
        Template b = Mustache.compiler().compile("{{value}} should be B");
        Mustache.Lambda lambda = new Mustache.Lambda() {
            public void execute (Template.Fragment frag, Writer out) {
                String which = frag.execute();
                if (which.equals("A")) {
                    frag.executeTemplate(a, out);
                } else if (which.equals("B")) {
                    frag.executeTemplate(b, out);
                } else {
                    throw new AssertionError("Invalid fragment content: " + which);
                }
            }
        };
        String tmpl = "[{{#lam}}{{value}}{{/lam}}]";
        test("[A should be A]", tmpl, context("lam", lambda, "value", "A"));
        test("[B should be B]", tmpl, context("lam", lambda, "value", "B"));
    }

    @Test public void testNonStandardDefaultDelims () {
        test(Mustache.compiler().withDelims("<% %>"), "bar", "<%foo%>", context("foo", "bar"));
    }

    protected String name;

    @Rule public TestRule watcher = new TestWatcher() {
       protected void starting(Description description) {
           name = description.getDisplayName();
       }
    };

    protected void test(Mustache.Compiler compiler, String expected, String template, Object ctx) {
        String actual = compiler.compile(template).execute(ctx);
        if (! Objects.equals(expected, actual)) {
            System.out.println("");
            System.out.println("----------------------------------------");
            System.out.println("");
            System.out.println("Failed: " + name);
            System.out.println("Template : \"" + SpecTest.showWhitespace(template) + "\"");
            if (! partials.isEmpty()) {
                System.out.println("Partials : ");
                for (Entry<String, String> e : partials.entrySet()) {
                    System.out.println("\t" + e.getKey() + ": \"" + SpecTest.showWhitespace(e.getValue()) + "\"");
                }
            }
            System.out.println("Expected : \"" + SpecTest.showWhitespace(expected) + "\"");
            System.out.println("Result   : \"" + SpecTest.showWhitespace(actual) + "\"");
            System.out.println("----------------------------------------");
            System.out.println("");
        }
        check(expected, actual);
    }

    protected void check (String expected, String output) {
        //assertEquals(uncrlf(expected), uncrlf(output));
        assertEquals(SpecTest.showWhitespace(expected), SpecTest.showWhitespace(output));

    }

    protected void test (String expected, String template, Object ctx) {
        test(Mustache.compiler(), expected, template, ctx);
    }

    protected static String uncrlf (String text) {
        return text == null ? null : text.replace("\r", "\\r").replace("\n", "\\n");
    }

    protected static Object context (Object... data) {
        Map<String, Object> ctx = new HashMap<String, Object>();
        for (int ii = 0; ii < data.length; ii += 2) {
            ctx.put(data[ii].toString(), data[ii+1]);
        }
        return ctx;
    }

    protected static Object EMPTY = context();
}
