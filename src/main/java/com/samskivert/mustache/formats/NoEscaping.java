package com.samskivert.mustache.formats;

import com.samskivert.mustache.Escaping;

public class NoEscaping implements Escaping {
    @Override
    public String escape(String raw) {
        return raw;
    }
}
