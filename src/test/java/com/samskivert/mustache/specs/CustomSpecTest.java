package com.samskivert.mustache.specs;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CustomSpecTest extends SpecTest {

    public CustomSpecTest(Spec spec, String name) {
        super(spec, name);
    }

    @Parameters(name = "{1}")
    public static Collection<Object[]> data () {
        String[] groups = new String[] {
            "sections",
            "partials",
            "~inheritance"
        };
        return SpecTest.data("/custom/specs/", groups);
    }
}
