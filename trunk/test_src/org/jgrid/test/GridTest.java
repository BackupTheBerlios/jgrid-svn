// $Id:                                                                    $

package org.jgrid.test;

import junit.framework.TestCase;
import org.jgrid.*;
import org.jgrid.util.JavaProcess;
import org.jgroups.util.Util;

import java.io.IOException;
import java.io.Serializable;

/**
 * Tests federation, load balancing and simple parallel jobs.
 */
public class GridTest extends TestCase
{
    private GridConfiguration config;
    /**
     * Standard JUnit test case constructor.
     *
     * @param name The name of the test case.
     */
    public GridTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        config = new GridConfiguration();

    }

    public void testFederation() throws Exception
    {
        GridBus gridBus = connect();

        Peers peers = gridBus.getPeers();

        JavaProcess[] p = startServers();

        try
        {
            // Wait for the number of peers to be the number of nodes.
            peers.waitForPeers(p.length);
        }
        finally
        {
            stopServers(gridBus, p);
        }
    }

    private void stopServers(GridBus gridBus, JavaProcess[] p)
    {
        System.out.println("##### STOP #####");
        gridBus.broadcastStop();
        System.out.println("##### Waiting for processes #####");
        // Start a thread that will interrupt in a few seconds.
        final Thread interrupted = Thread.currentThread();
        Thread interrupter = new Thread(new Runnable() {
            public void run()
            {
                Util.sleep(10000);
                System.out.println("Interrupting...");
                interrupted.interrupt();
            }
        });
        interrupter.setDaemon(true);
        interrupter.start();
        boolean[] stopped = new boolean[p.length];
        for (int i = 0; i < stopped.length; i++)
            stopped[i] = false;

        for (int i = 0; i < p.length; i++)
        {
            JavaProcess javaProcess = p[i];
            try
            {
                javaProcess.waitFor();
            }
            catch (InterruptedException e)
            {
                System.out.println("Interrupted while waiting for processes.");
                break;
            }
            stopped[i] = true;
        }

        for (int i = 0; i < p.length; i++)
        {
            if (stopped[i])
                continue;
            JavaProcess javaProcess = p[i];
            System.out.println("Killing " + javaProcess + ", it didn't stop by itself.");
            javaProcess.kill();
        }

        System.out.println("##### GRID STOPPED #####");
    }

    public void testLoadBalancing() throws Exception
    {
        GridBus gridBus = connect();

        Peers peers = gridBus.getPeers();

        JavaProcess[] p = startServers();

        try
        {
            // Wait for the number of peers to be the number of nodes.
            peers.waitForPeers(p.length);

            ClientSession session = config.getClientSession();
            Job job = session.createJob(MyService.class);
            // Make sure we can execute the job once.
            job.execute("test");
            Object result = job.takeResult(5000);
            assertEquals("executed test",result);
        }
        finally
        {
            stopServers(gridBus, p);
        }
    }

    private GridBus connect()
    {
        GridBus gridBus = config.getGridBus();
        gridBus.connect();
        return gridBus;
    }

    private JavaProcess[] startServers()
            throws IOException
    {
        // Start a few JVMS.
        JavaProcess[] p = new JavaProcess[] {
            new JavaProcess("org.jgrid.ServerMain"),
            new JavaProcess("org.jgrid.ServerMain"),
            new JavaProcess("org.jgrid.ServerMain"),
            new JavaProcess("org.jgrid.ServerMain"),
        };
        for (int i = 0; i < p.length; i++)
        {
            JavaProcess javaProcess = p[i];
            javaProcess.start();
        }
        return p;
    }

    public static class MyService implements Service
    {
        public Serializable execute(Serializable input)
        {
            return "executed " + input.toString();
        }
    }
}
