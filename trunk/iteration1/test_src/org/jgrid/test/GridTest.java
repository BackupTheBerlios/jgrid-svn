// $Id:                                                                    $

package org.jgrid.test;

import junit.framework.TestCase;
import org.jgrid.*;
import org.jgrid.util.JavaProcess;

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
        GridBus gridBus = GridSetupHelper.connect(config);

        Peers peers = gridBus.getPeers();

        JavaProcess[] p = GridSetupHelper.startServers(4);

        try
        {
            // Wait for the number of peers to be the number of nodes.
            peers.waitForPeers(p.length);
        }
        finally
        {
            GridSetupHelper.stopServers(gridBus, p);
        }
    }

    public void testLoadBalancing() throws Exception
    {
        GridBus gridBus = GridSetupHelper.connect(config);

        Peers peers = gridBus.getPeers();

        JavaProcess[] p = GridSetupHelper.startServers(4);

        try
        {
            // Wait for the number of peers to be the number of nodes.
            peers.waitForPeers(p.length);

            ClientSession session = config.getClientSession();
            Job job = session.createJob(MyService.class);
            // Make sure we can start the job once.
            job.start("test");
            Object result = job.join(5000);
            assertEquals("executed test",result);
        }
        finally
        {
            GridSetupHelper.stopServers(gridBus, p);
        }
    }

    public static class MyService implements Service
    {
        public Serializable execute(Serializable input)
        {
            return "executed " + input.toString();
        }
    }
}
