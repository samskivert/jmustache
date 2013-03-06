/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samskivert.mustache.specs;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

/**
 *
 * @author Yoryos Valotasios
 */
@RunWith(SpecRunner.class)
public class SpecTest {
    
    private static Mustache.Compiler compiler;
    
    @BeforeClass
    public static void setUp() {
        compiler = Mustache.compiler();
        System.out.println("Created " + compiler);
    }
    
    public static String[] getSpecsGroupsToRun() {
        return new String[] {"comments", "delimiters", "interpolation", "inverted", "sections"};
    }
    
    public void test(Spec spec) {
        Template t = compiler.compile(spec.getTemplate());
        String out = t.execute(spec.getData());
        Assert.assertEquals(spec.getExpectedOutput(), out);
    }
}
