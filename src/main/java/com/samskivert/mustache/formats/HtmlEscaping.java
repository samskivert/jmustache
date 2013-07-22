package com.samskivert.mustache.formats;

public class HtmlEscaping extends SimpleEscaping {
    public HtmlEscaping() {
        super(new String[][]{
                {"&", "&amp;"},
                {"'", "&#39;"},
                {"\"", "&quot;"},
                {"<", "&lt;"},
                {">", "&gt;"},
        });
    }
}
