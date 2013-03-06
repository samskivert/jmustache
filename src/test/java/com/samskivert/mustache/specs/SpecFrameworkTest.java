/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samskivert.mustache.specs;

import java.lang.reflect.Method;
import org.junit.runners.model.FrameworkMethod;

/**
 *
 * @author Yoryos Valotasios
 */
public class SpecFrameworkTest extends FrameworkMethod {
    private final Spec spec;
    
    public SpecFrameworkTest(Method method, Spec spec) {
        super(method);
        this.spec = spec;
    }

    @Override
    public Object invokeExplosively(Object target, Object... params) throws Throwable {
        return super.invokeExplosively(target, spec);
    }
    
    @Override
    public String getName() {
    	return spec.getSpecName();
    }
}
