// $Id:                                                                    $
package org.jgrid.util;

import java.io.IOException;

/**
 * Launches a JVM in a separate process using the specified classpath and main class.  The standard
 * output and error streams are piped into the parent's streams.
 * @author josh Jan 4, 2005 10:11:37 PM
 */
public class JavaProcess
{
    private Process process;
    private String classpath;
    private String mainClass;

    public JavaProcess(String mainClass)
    {
        this.mainClass = mainClass;
    }

    public String getClasspath()
    {
        if (classpath == null || classpath.length() == 0)
            return System.getProperty("java.class.path");
        else
            return classpath;
    }

    public void setClasspath(String classpath)
    {
        this.classpath = classpath;
    }

    public void start() throws IOException
    {
        if (process != null)
            throw new IllegalStateException("Process already running!");
        Runtime rt = Runtime.getRuntime();
        process = rt.exec(new String[] { "java", "-classpath", getClasspath(), mainClass } );

        StreamCopier stdout = new StreamCopier(process.getInputStream(),System.out);
        StreamCopier stderr = new StreamCopier(process.getErrorStream(),System.err);
        Thread errThread = new Thread(stderr);
        errThread.start();
        Thread outThread = new Thread(stdout);
        outThread.start();
    }

    public void kill()
    {
        process.destroy();
    }
    
    public int waitFor() throws InterruptedException
    {
        return process.waitFor();
    }
}
