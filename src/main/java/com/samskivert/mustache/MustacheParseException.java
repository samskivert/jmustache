//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache;

/**
 * An exception thrown if we encounter an error while parsing a template.
 */
public class MustacheParseException extends MustacheException
{
    public MustacheParseException (String message) {
        super(message);
    }

    public MustacheParseException (String message, int lineNo) {
        super(message + " @ line " + lineNo);
    }
}
