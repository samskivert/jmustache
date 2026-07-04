package com.samskivert.mustache;


public class HtmlEscaper implements Mustache.Escaper {


   private static final String[][] HTML_ENTITIES =
{
    { "&",  "&amp;" },
    { "'",  "&#39;" },
    { "\"", "&quot;" },
    { "<",  "&lt;" },
    { ">",  "&gt;" },
    { "`",  "&#x60;" },
    { "=",  "&#x3D;" }
};


    @Override
    public String escape(String text)
    {

        for(String[] entity : HTML_ENTITIES)
        {
            text =
            text.replace(
                    entity[0],
                    entity[1]);
        }


        return text;
    }
}