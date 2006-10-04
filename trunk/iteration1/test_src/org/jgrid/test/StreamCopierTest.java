
package org.jgrid.test;

import junit.framework.TestCase;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.jgrid.util.StreamCopier;

/**
 * Tests classes:
 * <ul>
 * <li>StreamCopier</li>
 * </ul>
 * User: josh
 * Date: Sep 22, 2002
 * Time: 12:38:27 AM
 */
public class StreamCopierTest extends TestCase
{
    private static final byte[] BYTES = "12345678901234567890".getBytes();

    public StreamCopierTest(String name)
    {
        super(name);
    }

    public void testStreamCopy()
    {
        byte[] a = BYTES;
        InputStream in = new ByteArrayInputStream(a);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamCopier copier = new StreamCopier(in,out);
        copier.run();
        Arrays.equals(a,out.toByteArray());
    }

}
