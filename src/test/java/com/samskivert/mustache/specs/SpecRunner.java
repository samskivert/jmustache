package com.samskivert.mustache.specs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author Yoryos Valotasios
 */
public class SpecRunner extends BlockJUnit4ClassRunner { 
    private final List<FrameworkMethod> tests;
    
    public SpecRunner(Class<?> klass) throws InitializationError {
        super(klass);
        assert  klass == SpecTest.class;
        this.tests = computeTests();
    }
    
    private List<FrameworkMethod> computeTests() throws InitializationError {
        try {
            Method getSpecsGroupsToRun = getTestClassMethod("getSpecsGroupsToRun");
            if (!Modifier.isStatic(getSpecsGroupsToRun.getModifiers())) {
              throw new InitializationError("Could not static method getSpecsGroupsToRun");  
            }
            
            String[] groups = (String[]) getSpecsGroupsToRun.invoke(getTestClass().getJavaClass());
            Method m = getTestClassMethod("test", Spec.class);
            
            List<FrameworkMethod> tests = new ArrayList<FrameworkMethod>();
            for (Spec s: getSpecsOfGroups(groups)) {
                tests.add(new SpecFrameworkTest(m, s));
            }
            
            return tests;
        } catch (SecurityException ex) {
            throw new InitializationError("Could not find getSpecsGroupsToRun" + getTestClass().getJavaClass());
        } catch (IllegalAccessException ex) {
            throw new InitializationError("Could invoke getSpecsGroupsToRun");
        } catch (IllegalArgumentException ex) {
            throw new InitializationError("Could invoke getSpecsGroupsToRun");
        } catch (InvocationTargetException ex) {
            throw new InitializationError("Could invoke getSpecsGroupsToRun");
        }
    }
    
    private Method getTestClassMethod(String name, Class<?>... paramTypes) throws InitializationError {
        try {
            return getTestClass().getJavaClass().getDeclaredMethod(name, paramTypes);
        } catch (NoSuchMethodException ex) {
            throw new InitializationError("Could not find " + name + " " + getTestClass().getJavaClass());
        } catch (SecurityException ex) {
            throw new InitializationError("Could not find " + name + " " + getTestClass().getJavaClass());
        }
    }
    
    private Collection<Spec> getSpecsOfGroups(String[] names) {
        Collection<Spec> specs = new HashSet<Spec>();
        for (String n: names) {
            specs.addAll(getSpecsOfGroup(n));
        }
        return specs;
    }
    
    private Collection<Spec> getSpecsOfGroup(String name) {
        Yaml yalm = new Yaml();
        @SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) yalm.load(getClass().getResourceAsStream("/specs/specs/" + name + ".yml"));
        @SuppressWarnings("unchecked")
		List<Map<String, Object>> tests = (List<Map<String, Object>>) map.get("tests");
        Collection<Spec> specs = new HashSet<Spec>();
        for (Map<String, Object> test: tests) {
            specs.add(new Spec(test));
        }
        return specs;
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        return tests;
    }

    @Override
    protected void validateInstanceMethods(List<Throwable> errors) {
        validatePublicVoidNoArgMethods(After.class, false, errors);
        validatePublicVoidNoArgMethods(Before.class, false, errors);
        validateTestMethods(errors);
    }
    
    
}
