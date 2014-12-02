//
// $Id$

package java.io;

/**
 * A basic implementation of {@code StringReader} for use in GWT.
 */
public class StringReader extends Reader
{
    public StringReader (String data) {
        _data = data;
    }

    public int read () throws IOException {
        return (_pos >= _data.length()) ? -1 : _data.charAt(_pos++);
    }

    protected final String _data;
    protected int _pos;
}
