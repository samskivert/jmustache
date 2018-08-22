package com.samskivert.mustache;

/**
 * Provides a simple interface for callers to implement their own logic for contextual values.
 */
public interface MustacheCustomContext {
    Object get(String name) throws Exception;
}
