//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache.ant;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.tools.ant.Project;
import org.junit.Test;

/**
 * Various unit tests.
 */
public class MustacheFilterTest {

	@Test
	public void testSimpleVariable() {
		test(new MustacheFilter(), "{{foo}}", "bar", context("foo", "bar"));
	}

	@Test
	public void testPrefixRemoved() {
		test(get("myprefix.", true), "{{foo}}", "bar",
				context("myprefix.foo", "bar", "foo", "IGNORED"));
	}

	@Test
	public void testBooleanTrue() {
		test(new MustacheFilter(),
				"{{#foo?}}foo? is True{{/foo?}}{{^foo?}}foo? is False{{/foo?}}",
				"foo? is True", context("foo?", "true"));
	}

	@Test
	public void testBooleanFalse() {
		test(new MustacheFilter(),
				"{{#foo?}}foo? is True{{/foo?}}{{^foo?}}foo? is False{{/foo?}}",
				"foo? is False", context("foo?", "false"));
	}

	@Test
	public void testBooleanAny() {
		test(new MustacheFilter(),
				"{{#foo?}}foo? is {{foo?}}{{/foo?}}{{^foo?}}foo? is False{{/foo?}}",
				"foo? is MyValue", context("foo?", "MyValue"));
	}

	@Test
	public void testList() {
		test(new MustacheFilter(),
				"{{#mylist}}{{__id__}}: {{p1}}-{{p2}}\n{{/mylist}}",
				"1: 1.1-1.2\n2: 2.1-2.2\n",
				context("mylist.1.p1", "1.1", "mylist.1.p2", "1.2",
						"mylist.2.p1", "2.1", "mylist.2.p2", "2.2"));
	}

	@Test
	public void testImbricatedList() {
		test(new MustacheFilter(),
				"{{#mylist1}}{{#mylist2}}{{p1}}-{{p2}}\n{{/mylist2}}{{/mylist1}}",
				"1.1.1-1.1.2\n1.2.1-1.2.2\n2.1.1-2.1.2\n2.2.1-2.2.2\n",
				context("mylist1.1.mylist2.1.p1", "1.1.1", "mylist1.1.mylist2.1.p2", "1.1.2",
						"mylist1.1.mylist2.2.p1", "1.2.1", "mylist1.1.mylist2.2.p2", "1.2.2",
						"mylist1.2.mylist2.1.p1", "2.1.1", "mylist1.2.mylist2.1.p2", "2.1.2",
						"mylist1.2.mylist2.2.p1", "2.2.1", "mylist1.2.mylist2.2.p2", "2.2.2"));
	}

	protected MustacheFilter get(String prefix, Boolean removePrefix) {
		MustacheFilter m = new MustacheFilter();
		if (prefix != null) {
			m.setPrefix(prefix);
		}
		if (removePrefix != null) {
			m.setRemovePrefix(removePrefix);
		}
		return m;
	}

	protected void test(MustacheFilter mustache, String template,
			String expected, Map<String, String> context) {
		Project project = new Project();
		project.setProperty("ant.regexp.regexpimpl",
				"org.apache.tools.ant.util.regexp.Jdk14RegexpRegexp");
		if (context != null) {
			for (Entry<String, String> entry : context.entrySet()) {
				project.setProperty(entry.getKey(), entry.getValue());
			}
		}
		mustache.setProject(project);
		String output = mustache.filter(template);
		check(expected, output);
	}

	protected void check(String expected, String output) {
		assertEquals(uncrlf(expected), uncrlf(output));
	}

	protected static String uncrlf(String text) {
		return text == null ? null : text.replace("\r", "\\r").replace("\n", "\\n");
	}

	protected Map<String, String> context(String... data) {
		Map<String, String> ctx = new HashMap<String, String>();
		for (int ii = 0; ii < data.length; ii += 2) {
			ctx.put(data[ii], data[ii + 1]);
		}
		return ctx;
	}
}
