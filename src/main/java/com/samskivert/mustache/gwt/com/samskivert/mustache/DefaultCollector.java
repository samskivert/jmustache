//
// $Id$

package com.samskivert.mustache;

import java.util.Arrays;
import java.util.Iterator;

/**
 * A collector used when running in GWT.
 */
public class DefaultCollector extends BasicCollector
{
    @Override
    public Iterator<?> toIterator (final Object value) {
        Iterator<?> iter = super.toIterator(value);
        if (iter != null) return iter;

        if (value.getClass().isArray()) {
            return Arrays.asList((Object[])value);
        }
        return null;
    }
}
