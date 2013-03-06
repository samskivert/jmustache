package com.samskivert.mustache;

/**
 * An exception thrown if we encounter an unsatisfactory context error (e.g. a missing variable) while executing a template.
 */
public class MustacheContextException extends MustacheException {
    private String key;
    private int lineNo;

    public MustacheContextException (String message, String key, int lineNo) {
        super(message);
        this.key = key;
        this.lineNo = lineNo;
    }

    public MustacheContextException (String message, String key, int lineNo, Throwable cause) {
        super(message, cause);
        this.key = key;
        this.lineNo = lineNo;
    }

    /** Get key (e.g. variable or section name) that caused the problem. */
    public String getKey() {
        return key;
    }

    /** Line number where the problem occurred. */
    public int getLineNo() {
        return lineNo;
    }

}
