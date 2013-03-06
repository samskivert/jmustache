//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache;

/**
 * An exception thrown when an error occurs parsing or executing a Mustache template.
 */
public class MustacheException extends RuntimeException
{
    /** An exception thrown if we encounter a context error (e.g. a missing variable) while
     * compiling or executing a template. */
    public static class Context extends MustacheException {
        /** The key that caused the problem. */
        public final String key;

        /** The line number of the template on which the problem occurred. */
        public final int lineNo;

        public Context (String message, String key, int lineNo) {
            super(message);
            this.key = key;
            this.lineNo = lineNo;
        }

        public Context (String message, String key, int lineNo, Throwable cause) {
            super(message, cause);
            this.key = key;
            this.lineNo = lineNo;
        }
    }

    public MustacheException (String message) {
        super(message);
    }

    public MustacheException (Throwable cause) {
        super(cause);
    }

    public MustacheException (String message, Throwable cause) {
        super(message, cause);
    }
}
