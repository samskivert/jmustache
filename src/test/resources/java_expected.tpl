package com.example;

import java.lang.*;
import java.util.*;

public class Configuration {

    /*
     * Copy constructor
     */
    public void Configuration(Configuration src) {
        setFoo(src.getFoo());
        setBar(src.getBar());
        setBaz(src.getBaz());
    }

}