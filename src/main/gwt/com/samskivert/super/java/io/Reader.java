//
// $Id$

package java.io;

/**
 * A minimal version of {@code Reader} to satisfy GWT.
 */
public abstract class Reader
{
    public abstract int read () throws IOException;
    public abstract void close () throws IOException;
}
