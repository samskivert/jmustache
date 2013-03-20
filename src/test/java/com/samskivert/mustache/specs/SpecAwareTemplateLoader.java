package com.samskivert.mustache.specs;

import java.io.Reader;
import java.io.StringReader;

import com.samskivert.mustache.Mustache.TemplateLoader;

public class SpecAwareTemplateLoader implements TemplateLoader {
	private Spec spec;

	public Reader getTemplate(String name) throws Exception {
		if (spec == null) return null;
		String partial = spec.getPartial(name);
		if (partial == null) return null;
		return new StringReader(partial);
	}

	public void setSpec(Spec spec) {
		this.spec = spec;
	}
}
