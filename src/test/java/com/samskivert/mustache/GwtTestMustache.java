//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.junit.client.GWTTestCase;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Tests basic Mustache operation in GWT.
 */
public class GwtTestMustache extends SharedTests
{
    public String getModuleName () {
        return "com.samskivert.Mustache";
    }

    // @Test public void testSimpleVariable () {
    //     test("bar", "{{foo}}", context("foo", "bar"));
    // }

    // @Test public void testPrimitiveArrayVariable () {
    //     test("1234", "{{#foo}}{{this}}{{/foo}}", new Object() {
    //         int[] getFoo () { return new int[] { 1, 2, 3, 4 }; }
    //     });
    // }

    // protected void test (String expected, String template, Object ctx) {
    //     test(Mustache.compiler(), expected, template, ctx);
    // }

    // protected void test (Mustache.Compiler compiler, String expected, String template, Object ctx) {
    //     assertEquals(expected, compiler.compile(template).execute(ctx));
    // }

    // protected Object context (Object... data) {
    //     Map<String, Object> ctx = new HashMap<String, Object>();
    //     for (int ii = 0; ii < data.length; ii += 2) {
    //         ctx.put(data[ii].toString(), data[ii+1]);
    //     }
    //     return ctx;
    // }
}
