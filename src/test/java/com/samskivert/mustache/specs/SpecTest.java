//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache.specs;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

/**
 * @author Yoryos Valotasios
 */
@RunWith(SpecRunner.class)
public class SpecTest
{
    private static Mustache.Compiler compiler;
    private static SpecAwareTemplateLoader loader;

    @BeforeClass
    public static void setUp () {
        loader = new SpecAwareTemplateLoader();
        compiler = Mustache.compiler().defaultValue("").withLoader(loader);
    }

    public void test (Spec spec) {
        loader.setSpec(spec);
        String tmpl = spec.getTemplate();
        String desc = String.format("Template: '''%s'''\nData: '%s'\n",
                                    uncrlf(tmpl), uncrlf(spec.getData().toString()));
        try {
            Template t = compiler.compile(spec.getTemplate());
            String out = t.execute(spec.getData());
            Assert.assertEquals(desc, uncrlf(spec.getExpectedOutput()), uncrlf(out));
        } catch (Exception e) {
            Assert.fail(desc + ": " + e);
        }
    }

    private static String uncrlf (String text) {
        return (text == null) ? null : text.replace("\r", "\\r").replace("\n", "\\n");
    }
}
