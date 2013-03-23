package com.samskivert.mustache.specs;

import java.io.Reader;
import java.io.StringReader;

import com.samskivert.mustache.Mustache.TemplateLoader;

public class SpecAwareTemplateLoader implements TemplateLoader {
	private static final String EMPTY_STRING = "";
	private Spec spec;

	public Reader getTemplate(String name) throws Exception {
		if (spec == null) return new StringReader(EMPTY_STRING);
		String partial = spec.getPartial(name);
		if (partial == null) return new StringReader(EMPTY_STRING);
		return new StringReader(partial);
	}

	public void setSpec(Spec spec) {
		this.spec = spec;
	}
}
