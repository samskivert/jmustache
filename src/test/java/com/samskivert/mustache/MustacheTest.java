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
        test("bar", "{{foo}}", "foo", "bar");
    }

    @Test public void testOneShotSection () {
        test("baz", "{{#foo}}{{bar}}{{/foo}}", "foo", context("bar", "baz"));
    }

    @Test public void testListSection () {
        test("bazbif", "{{#foo}}{{bar}}{{/foo}}", "foo",
             Arrays.asList(context("bar", "baz"), context("bar", "bif")));
    }

    @Test public void testArraySection () {
        test("bazbif", "{{#foo}}{{bar}}{{/foo}}", "foo",
             new Object[] { context("bar", "baz"), context("bar", "bif") });
    }

    @Test public void testIteratorSection () {
        test("bazbif", "{{#foo}}{{bar}}{{/foo}}", "foo",
             Arrays.asList(context("bar", "baz"), context("bar", "bif")).iterator());
    }

    @Test public void testEmptyListSection () {
        test("", "{{#foo}}{{bar}}{{/foo}}", "foo", Collections.emptyList());
    }

    @Test public void testEmptyArraySection () {
        test("", "{{#foo}}{{bar}}{{/foo}}", "foo", new Object[0]);
    }

    @Test public void testEmptyIteratorSection () {
        test("", "{{#foo}}{{bar}}{{/foo}}", "foo", Collections.emptyList().iterator());
    }

    @Test public void testFalseSection () {
        test("", "{{#foo}}{{bar}}{{/foo}}", "foo", false);
    }

    @Test public void testNestedListSection () {
        test("1234", "{{#a}}{{#b}}{{c}}{{/b}}{{#d}}{{e}}{{/d}}{{/a}}", "a",
             new Object[] { context("b", new Object[] { context("c", "1"), context("c", "2") }),
                            context("d", new Object[] { context("e", "3"), context("e", "4") }) });
    }

    @Test public void testComment () {
        test("foobar", "foo{{! nothing to see here}}bar");
    }

    protected void test (String expected, String template, Object... data)
    {
        assertEquals(expected, Mustache.compile(template).execute(context(data)));
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
