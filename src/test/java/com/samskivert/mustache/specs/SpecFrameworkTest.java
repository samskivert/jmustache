//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache.specs;

import java.lang.reflect.Method;
import org.junit.runners.model.FrameworkMethod;

/**
 * @author Yoryos Valotasios
 */
public class SpecFrameworkTest extends FrameworkMethod
{
    private final String group;
    private final Spec spec;

    public SpecFrameworkTest (Method method, String group, Spec spec) {
        super(method);
        this.group = group;
        this.spec = spec;
    }

    @Override
    public Object invokeExplosively (Object target, Object... params) throws Throwable {
        return super.invokeExplosively(target, spec);
    }

    @Override
    public String getName () {
        return group + ": " + spec.getName();
    }
}
