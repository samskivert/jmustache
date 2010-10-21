//
// $Id$

package com.samskivert.mustache;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * Represents a compiled template.
 */
public class Template
{
    /**
     * Executes this template with the supplied data, writing the results to the supplied writer.
     *
     * <p>The data can be any tree of objects. Given a name <code>foo</code>, the following
     * mechanisms are supported for resolving its value (and are sought in this order):
     * <ul>
     * <li>If the object is a {@link Map}, {@link Map#get} will be called with the string
     * <code>foo</code> as the key.
     * <li>A method named <code>foo</code> in the supplied object (with non-void return value).
     * <li>A method named <code>getFoo</code> in the supplied object (with non-void return value).
     * <li>A field named <code>foo</code> in the supplied object.
     * </ul>
     * </p><p> The field type, method return type, or map value type should correspond to the
     * desired behavior if the resolved name corresponds to a section. {@link Boolean} is used for
     * showing or hiding sections without binding a sub-context. Arrays, {@link Iterator} and
     * {@link Iterable} implementations are used for sections that repeat, with the context bound
     * to the elements of the array, iterator or iterable. Lambdas are current unsupported, though
     * they would be easy enough to add if desire exists. See the <a
     * href="http://mustache.github.com/mustache.5.html">Mustache documentation</a> for more
     * details on section behavior. </p>
     *
     * @throws MustacheException if an error occurs while writing the template.
     */
    public void execute (Object data, Writer out) throws MustacheException
    {
        for (Segment seg : _segs) {
            seg.execute(data, out);
        }
    }

    /**
     * Executes this template with the supplied data, returning the results as a string. See {@link
     * #execute(Object, Writer).
     *
     * @throws MustacheException if an error occurs while writing the template.
     */
    public String execute (Object data) throws MustacheException
    {
        StringWriter out = new StringWriter();
        execute(data, out);
        return out.toString();
    }

    protected Template (Segment[] segs)
    {
        _segs = segs;
    }

    /** A template is broken into segments. */
    protected static abstract class Segment
    {
        abstract void execute (Object ctx, Writer out);

        protected Object getValue (Object ctx, String name) {
            // TODO: support things other than values
            return ((Map<?,?>)ctx).get(name);
        }

        protected static void write (Writer out, String data) {
            try {
                out.write(data);
            } catch (IOException ioe) {
                throw new MustacheException(ioe);
            }
        }
    }

    protected final Segment[] _segs;
}
