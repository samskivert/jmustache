//
// $Id$

package com.samskivert.mustache;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.*;
import static org.junit.Assert.*;

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

    @Test public void testArraySection () {
        test("bazbif", "{{#foo}}{{bar}}{{/foo}}",
             context("foo", new Object[] {
                     context("bar", "baz"), context("bar", "bif") }));
    }

    @Test public void testIteratorSection () {
        test("bazbif", "{{#foo}}{{bar}}{{/foo}}",
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

    @Test public void testComment () {
        test("foobar", "foo{{! nothing to see here}}bar", new Object());
    }

    @Test public void testEscapeHTML () {
        assertEquals("&lt;b&gt;", Mustache.compiler().compile("{{a}}").
                     execute(context("a", "<b>")));
        assertEquals("<b>", Mustache.compiler().escapeHTML(false).compile("{{a}}").
                     execute(context("a", "<b>")));
    }

    @Test public void testPartialDelimiterMatch () {
        assertEquals("{bob}", Mustache.compiler().compile("{bob}").execute(context()));
        assertEquals("bar", Mustache.compiler().compile("{{bob}bob}}").execute(
                         context("bob}bob", "bar")));
    }

    @Test public void testTopLevelThis () {
        assertEquals("bar", Mustache.compiler().compile("{{this}}").execute("bar"));
    }

    @Test public void testNestedThis () {
        assertEquals("barbazbif", Mustache.compiler().compile("{{#things}}{{this}}{{/things}}").
                     execute(context("things", Arrays.asList("bar", "baz", "bif"))));
    }

    @Test public void testStructuredVariable () {
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
        String tmpl = "first line\n{{#nonexistent}}foo{{/nonexistent}}\nsecond line";
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

    @Test(expected = MustacheException.class)
    public void testStandardsModeWithNoParentContextSearching () {
        String tmpl = "{{#parent}}foo{{parentProperty}}bar{{/parent}}";
        String result = Mustache.compiler().standardsMode(true).compile(tmpl).
            execute(context("parent", new Object(),
                            "parentProperty", "bar"));
    }

    protected void test (String expected, String template, Object ctx)
    {
        assertEquals(expected, Mustache.compiler().compile(template).execute(ctx));
    }

    protected static Object context (Object... data)
    {
        Map<String, Object> ctx = new HashMap<String, Object>();
        for (int ii = 0; ii < data.length; ii += 2) {
            ctx.put(data[ii].toString(), data[ii+1]);
        }
        return ctx;
    }
}
