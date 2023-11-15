//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache.specs;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * @author Yoryos Valotasios
 */
public class SpecRunner extends BlockJUnit4ClassRunner
{
    private final Yaml yaml = new Yaml();
    private final List<FrameworkMethod> tests;

    public SpecRunner (Class<?> klass) throws InitializationError {
        super(klass);
        if (klass != SpecTest.class) throw new IllegalArgumentException(
            SpecRunner.class.getSimpleName() + " should only be used with classes of type " +
            SpecTest.class.getName());
        this.tests = computeTests();
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods () {
        return tests;
    }

    @Override
    protected void validateInstanceMethods (List<Throwable> errors) {
        validatePublicVoidNoArgMethods(After.class, false, errors);
        validatePublicVoidNoArgMethods(Before.class, false, errors);
        validateTestMethods(errors);
    }

    private List<FrameworkMethod> computeTests () throws InitializationError {
        String[] groups =  new String[] {
            "comments",
            "delimiters",
            "interpolation",
            "inverted",
            "sections",
            "partials"
        };
        Method m = getTestClassMethod("test", Spec.class);
        List<FrameworkMethod> tests = new ArrayList<FrameworkMethod>();
        for (String name : groups) {
            for (Map<String, Object> test: getTestsForGroup(name)) {
                tests.add(new SpecFrameworkMethod(m, name, new Spec(test)));
            }
        }
        return tests;
    }

    private Method getTestClassMethod (String name, Class<?>... paramTypes)
        throws InitializationError {
        try {
            return getTestClass().getJavaClass().getDeclaredMethod(name, paramTypes);
        } catch (NoSuchMethodException ex) {
            throw new InitializationError("Could not find " + name + " " +
                                          getTestClass().getJavaClass());
        } catch (SecurityException ex) {
            throw new InitializationError("Could not find " + name + " " +
                                          getTestClass().getJavaClass());
        }
    }

    private Iterable<Map<String, Object>> getTestsForGroup (String name) {
        String ymlPath = "/specs/specs/" + name + ".yml";
        try {
            @SuppressWarnings("unchecked") Map<String, Object> map =
                (Map<String, Object>)yaml.load(getClass().getResourceAsStream(ymlPath));
            @SuppressWarnings("unchecked") List<Map<String, Object>> tests =
                (List<Map<String, Object>>)map.get("tests");
            return tests;
        } catch (YAMLException err) {
            System.err.println("*** Error loading: " + ymlPath);
            System.err.println("*** You probably need to 'git submodule update'.");
            throw err;
        }
    }
}
