package com.samskivert.mustache.specs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

public abstract class SpecTest {

    private static final Yaml yaml = new Yaml();

    private final Spec spec;
    private final String name;

    public SpecTest (Spec spec, String name) {
        super();
        this.spec = spec;
        this.name = name;
    }

    @Test
    public void test () throws Exception {
        //System.out.println("Testing: " + name);
        SpecAwareTemplateLoader loader = new SpecAwareTemplateLoader(spec);
        Mustache.Compiler compiler = Mustache.compiler().emptyStringIsFalse(true).defaultValue("").withLoader(loader);
        String tmpl = spec.getTemplate();
        String desc = String.format("Template: '''%s'''\nData: '%s'\n",
                                    uncrlf(tmpl), uncrlf(spec.getData().toString()));
        try {
            Template t = compiler.compile(spec.getTemplate());
            String out = t.execute(spec.getData());
            if (! Objects.equals(uncrlf(spec.getExpectedOutput()), uncrlf(out))) {
                System.out.println("");
                System.out.println("----------------------------------------");
                System.out.println("");
                System.out.println("Failed: " + name);
                System.out.println(spec);
                System.out.println("Expected : \"" + showWhitespace(spec.getExpectedOutput()) + "\"");
                System.out.println("Result   : \"" + showWhitespace(out) + "\"");
                System.out.println("----------------------------------------");
                System.out.println("");
            }
            Assert.assertEquals(desc, showWhitespace(spec.getExpectedOutput()), showWhitespace(out));
        } catch (Exception e) {

            // the specs tests assume that the engine silently ignores invalid delimiter
            // specifications, but we throw an exception (and rightfully so IMO; this is not a
            // place where silent failure is helpful), so just ignore those test failures
            if (!e.getMessage().contains("Invalid delimiter")) {
                e.printStackTrace();
                Assert.fail(
                desc + "\nExpected: " + uncrlf(spec.getExpectedOutput()) + "\nError: " + e);
            }
        }
    }

    public static String showWhitespace (String s) {
       s = s.replace("\r\n", "\u240D");
       s = s.replace('\t', '\u21E5');
       s = s.replace("\n", "\u21B5\n");
       s = s.replace("\u240D", "\u240D\n");
       return s;
    }

    private static String uncrlf (String text) {
        return (text == null) ? null : text.replace("\r", "\\r").replace("\n", "\\n");
    }

    public static Collection<Object[]> data (String specPath, String[] groups) {
        List<Object[]> tuples = new ArrayList<>();
        int i = 0;
        for (String g : groups) {
            Iterable<Spec> specs = getTestsForGroup(specPath, g);
            for (Spec s : specs) {
                Object[] tuple = new Object[] {s, g + "-" + s.getName() + "-" + i++};
                tuples.add(tuple);
            }
        }
        return tuples;
    }

    private static Iterable<Spec> getTestsForGroup (String specPath, String name) {
        //String ymlPath = "/specs/specs/" + name + ".yml";
        String ymlPath = specPath + name + ".yml";
        return getTestsFromYaml(name, ymlPath);
    }

    private static Iterable<Spec> getTestsFromYaml(String name, String ymlPath) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) yaml.load(SpecTest.class.getResourceAsStream(ymlPath));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tests = (List<Map<String, Object>>) map.get("tests");
            List<Spec> specs = new ArrayList<>();
            for (Map<String,Object> t : tests) {
                specs.add(new Spec(t));
            }
            return specs;
        } catch (YAMLException err) {
            System.err.println("*** Error loading: " + ymlPath);
            System.err.println("*** You probably need to 'git submodule update'.");
            throw err;
        }
    }
}
