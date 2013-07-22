package com.samskivert.mustache.formats;

import com.samskivert.mustache.Escaping;

public class SimpleEscaping implements Escaping {
    private final String[][] substringReplacements;

    public SimpleEscaping(String[][] substringReplacements) {
        this.substringReplacements = substringReplacements;
    }

    @Override
    public String escape(String text) {
        for (String[] escape : substringReplacements) {
            text = text.replace(escape[0], escape[1]);
        }
        return text;
    }
}
