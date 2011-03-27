package com.samskivert.mustache;

import java.io.Reader;

/**
 * 
 * @author Sean Scanlon <sean.scanlon@gmail.com>
 */
public interface MustacheTemplateLoader {

    /**
     * load a {@code Mustache} template from a given file.
     * 
     * @param filename
     * @return a string {@code Mustache} template
     */
    public Reader getTemplate(String filename) throws Exception;

}
