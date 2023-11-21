//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache.specs;

import java.io.Reader;
import java.io.StringReader;

import com.samskivert.mustache.Mustache;

public class SpecAwareTemplateLoader implements Mustache.TemplateLoader
{
    private static final String EMPTY_STRING = "";
    private final Spec spec;

    public SpecAwareTemplateLoader(Spec spec) {
        super();
        this.spec = spec;
    }

    @Override public Reader getTemplate (String name) throws Exception {
        if (spec == null) return new StringReader(EMPTY_STRING);
        String partial = spec.getPartial(name);
        return new StringReader(partial == null ? EMPTY_STRING : partial);
    }
}
