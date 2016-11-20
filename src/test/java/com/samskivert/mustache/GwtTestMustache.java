//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.junit.client.GWTTestCase;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Tests basic Mustache operation in GWT.
 */
public class GwtTestMustache extends SharedTests
{
    public String getModuleName () {
        return "com.samskivert.Mustache";
    }
}
