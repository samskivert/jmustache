//
// $Id$

package java.io;

/**
 * A basic implementation of {@code StringReader} for use in GWT.
 */
public class StringWriter extends Writer
{
    public void write (String text) throws IOException {
        _buf.append(text);
    }

    @Override public String toString () {
        return _buf.toString();
    }

    protected final StringBuilder _buf = new StringBuilder();
}
