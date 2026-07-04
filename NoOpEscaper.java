package com.samskivert.mustache;


public class NoOpEscaper implements Mustache.Escaper {


    @Override
    public String escape(String text)
    {
        return text;
    }
}