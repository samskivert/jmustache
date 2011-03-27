package com.samskivert.mustache;

import java.io.Reader;

/**
 * loading partials/other templates is not supported. in this {@link MustacheTemplateLoader}
 * 
 * @author Sean Scanlon <sean.scanlon@gmail.com>
 * 
 */
public enum UnsupportedTemplateLoader implements MustacheTemplateLoader {

    INSTANCE;

    public Reader getTemplate(String filename) {
        throw new UnsupportedOperationException("template loading is not supported");
    }

}
