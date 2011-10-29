//
// $Id$

package java.io;

/**
 * A minimal version of {@code Writer} to satisfy GWT.
 */
public interface Writer
{
    void write (String text) throws IOException;
}
