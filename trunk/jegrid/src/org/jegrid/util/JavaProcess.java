package org.jegrid.util;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Launches a JVM in a separate process using the specified classpath and main class.  The standard
 * output and error streams are piped into the parent's streams.
 * @author josh Jan 4, 2005 10:11:37 PM
 */
public class JavaProcess
{
    private static Logger log = Logger.getLogger(JavaProcess.class);

    private Process process;
    private String classpath;
    private String mainClass;
    private String[] args;
    private OutputStream out;
    private OutputStream err;

    public JavaProcess(String mainClass)
    {
        this.mainClass = mainClass;
        out = System.out;
        err = System.err;
    }


    public void setOutStream(OutputStream out)
    {
        this.out = out;
    }

    public void setErrorStream(OutputStream err)
    {
        this.err = err;
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

    public void setArgs(String[] args)
    {
        this.args = args;
    }
    
    public void start() throws IOException
    {
        if (process != null)
            throw new IllegalStateException("Process already running!");
        Runtime rt = Runtime.getRuntime();
        List commandLine = new ArrayList();
        commandLine.add("java");
        commandLine.add("-classpath");
        commandLine.add(getClasspath());
        commandLine.add(mainClass);

        if (args != null && args.length > 0)
            commandLine.addAll(Arrays.asList(args));
        
        String[] cmdarray = (String[]) commandLine.toArray(new String[commandLine.size()]);
        process = rt.exec(cmdarray);

        StreamCopier stdout = new StreamCopier(process.getInputStream(), out);
        StreamCopier stderr = new StreamCopier(process.getErrorStream(), err);
        Thread errThread = new Thread(stderr);
        errThread.start();
        Thread outThread = new Thread(stdout);
        outThread.start();
        log.info("Process started.");
    }

    public void kill()
    {
        process.destroy();
        process = null;
    }

    /**
     * Waits for the process to complete.
     * @return the return code from the process.
     * @throws InterruptedException if the thread is interrupted.
     */
    public int waitFor() throws InterruptedException
    {
        log.info("Waiting ...");
        int rc = process.waitFor();
        log.info("Process completed.  return code = " + rc);
        return rc;
    }
}
