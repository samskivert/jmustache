package com.samskivert.mustache;

import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class DelimsTest {

    @Test public void shouldConstructDefaultDelims () {
        test(new Mustache.Delims(), '{', '{', '}', '}', true);
    }

    @Test public void shouldUpdateDefaultDelims () {
        Mustache.Delims delims = new Mustache.Delims();
        delims.updateDelims("<% %>");
        test(delims, '<', '%', '%', '>', false);
    }

    @Test public void shouldConstructCustomDelims () {
        test(new Mustache.Delims("<% %>"), '<', '%', '%', '>', true);
    }

    @Test public void shouldUpdateCustomConstructedDelims () {
        Mustache.Delims delims = new Mustache.Delims("<% %>");
        delims.updateDelims("{{ }}");
        test(delims, '{', '{', '}', '}', false);
    }

    protected void test (Mustache.Delims delims, char start1, char start2, char end1, char end2, boolean isDefault)
    {
        assertThat(delims.start1, equalTo(start1));
        assertThat(delims.start2, equalTo(start2));
        assertThat(delims.end1, equalTo(end1));
        assertThat(delims.end2, equalTo(end2));
        assertThat(delims.isDefault(), equalTo(isDefault));
    }
}
