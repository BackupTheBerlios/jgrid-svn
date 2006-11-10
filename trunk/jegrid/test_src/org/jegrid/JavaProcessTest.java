package org.jegrid;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.jegrid.util.JavaProcess;

import java.io.ByteArrayOutputStream;

/**
 * Test the process spawning/interrupting code.
 * <br>User: jdavis
 * Date: Nov 10, 2006
 * Time: 9:29:08 AM
 */
public class JavaProcessTest extends TestCase
{
    private static Logger log = Logger.getLogger(JavaProcessTest.class);

    public void testRun() throws Exception
    {
        JavaProcess proc = new JavaProcess(TestMain.class.getName());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        proc.setOutStream(baos);
        proc.setErrorStream(baos);
        proc.start();
        int rc = proc.waitFor();
        assertEquals(0,rc);
        String output = baos.toString();
        log.info("Output:\n" + output);
        assertTrue(output.indexOf("Hello") >= 0);
        assertTrue(output.indexOf("Goodbye") >= 0);
    }

    public void testSleep() throws Exception
    {
        JavaProcess proc = new JavaProcess(TestMain.class.getName());
        proc.setArgs(new String[] { "2000" } );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        proc.setOutStream(baos);
        proc.setErrorStream(baos);
        proc.start();
        int rc = proc.waitFor();
        assertEquals(0,rc);
        String output = baos.toString();
        log.info("Output:\n" + output);
        assertTrue(output.indexOf("Hello") >= 0);
        assertTrue(output.indexOf("Goodbye") >= 0);
        assertTrue(output.indexOf("sleep") >= 0);
    }
}
