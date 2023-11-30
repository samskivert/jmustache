package com.samskivert.mustache.specs;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class OfficialSpecTest extends SpecTest {

    public OfficialSpecTest(Spec spec, String name) {
        super(spec, name);
    }

    @Parameters(name = "{1}")
    public static Collection<Object[]> data () {
        String[] groups = new String[] {
            "comments",
            "delimiters",
            "interpolation",
            "inverted",
            "sections",
            "partials",
            "~inheritance"
        };
        return SpecTest.data("/specs/specs/", groups);
    }
}
