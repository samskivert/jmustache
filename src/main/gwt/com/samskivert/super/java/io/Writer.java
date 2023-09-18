//
// $Id$

package java.io;

/**
 * A minimal version of {@code Writer} to satisfy GWT.
 */
public abstract class Writer
{
    public abstract void write (String text) throws IOException;

    public abstract Writer append (CharSequence text) throws IOException;
}
