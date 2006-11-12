package org.jegrid;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.jegrid.util.JavaProcess;
import org.jegrid.util.Util;

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

    public void testGetJavaCommand() throws Exception
    {
        int version = Util.getJavaVersion();
        if (version < 14)
            throw new Exception("JEGrid needs JDK 1.4+");
        String javaCommand = Util.getJavaCommand();
        log.info("javaCommand = " + javaCommand);
    }
    
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

    public void testInterruptSleep() throws Exception
    {
        JavaProcess proc = new JavaProcess(TestMain.class.getName());
        proc.setArgs(new String[] { "10000" } );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        proc.setOutStream(baos);
        proc.setErrorStream(baos);
        proc.start();
        Util.sleep(1000);
        proc.kill();
        int rc = proc.waitFor();
        log.info("rc = " + rc);
        String output = baos.toString();
        log.info("Output:\n" + output);
        assertTrue(output.indexOf("Hello") >= 0);
        assertTrue(output.indexOf("Goodbye") < 0);
        assertTrue(output.indexOf("sleep") >= 0);
    }
}
