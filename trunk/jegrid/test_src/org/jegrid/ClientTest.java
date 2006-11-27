package org.jegrid;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.jegrid.impl.NodeStatusImpl;
import org.jegrid.impl.ServerComparator;
import org.jegrid.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Some basic client side tests.
 * <br>User: Joshua Davis
 * Date: Oct 8, 2006
 * Time: 10:04:52 AM
 */
public class ClientTest extends TestCase
{
    private static Logger log = Logger.getLogger(ClientTest.class);

    public void testServerComparator() throws Exception
    {
        long startTime = 0;
        // Nodes with zero free threads go to the bottom.
        // Nodes with a larger percentage of free threads are preferred.
        // Nodes with a larger percentage of free memory per free thread are preferred.
        long lastTaskAccepted = 0;
        NodeStatusImpl[] nodes = new NodeStatusImpl[]{
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 100, 100, 0, 20, startTime, 0, lastTaskAccepted, null),
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 100, 100, 0, 10, startTime, 0, lastTaskAccepted, null),
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 100, 100, 19, 25, startTime, 0, lastTaskAccepted, null),
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 100, 100, 9, 10, startTime, 0, lastTaskAccepted, null),
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 200, 200, 10, 10, startTime, 0, lastTaskAccepted, null),
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 100, 100, 10, 10, startTime, 0, lastTaskAccepted, null),
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 100, 100, 8, 10, startTime, 0, lastTaskAccepted, null),
        };
        Arrays.sort(nodes, new ServerComparator());
        assertEquals(10, nodes[0].getAvailableWorkers());
        assertEquals(200, nodes[0].getFreeMemory());
        assertEquals(10, nodes[1].getAvailableWorkers());
        assertEquals(100, nodes[1].getFreeMemory());
        assertEquals(9, nodes[2].getAvailableWorkers());
        assertEquals(8, nodes[3].getAvailableWorkers());
        assertEquals(19, nodes[4].getAvailableWorkers());
        assertEquals(0, nodes[nodes.length - 1].getAvailableWorkers());
    }

    public void testStartStopServers() throws Exception
    {
        GridConfiguration config = new GridConfiguration();
        config.setGridName("test");
        config.setType(Grid.TYPE_CLIENT);
        Grid grid = config.configure();
        grid.connect();

        GridStatus status = grid.getGridStatus(false);
        assertEquals(0, status.getNumberOfServers());

        // Create server JVMs.
        ServerJvms jvms = new ServerJvms(grid, 3, 2);
        jvms.start();

        try
        {
            Util.sleep(1000);

            grid.shutdownServers();

            jvms.waitFor();
        }
        finally
        {
            jvms.stop();
        }
        status = grid.getGridStatus(false);
        assertEquals(0, status.getNumberOfServers());
        grid.disconnect();
    }

    public void testSimpleClient() throws Exception
    {
        GridConfiguration config = new GridConfiguration();
        config.setGridName("test");
        config.setType(Grid.TYPE_CLIENT);
        Grid grid = config.configure();
        grid.connect();

        // Create two server JVMs.
        ServerJvms jvms = new ServerJvms(grid, 2, 2);
        jvms.start();

        String taskKey = "test task";
        final MonteCarloPi.Output output;
        try
        {
            Client client = grid.getClient();
            Task task = client.createTask(taskKey);
            for (int i = 0; i < 10; i++)
                task.addInput(new MonteCarloPi.Input(17 * i + 1, 1000));
            output = new MonteCarloPi.Output();
            MonteCarloPi.MCPiAggregator aggregator = new MonteCarloPi.MCPiAggregator();
            aggregator.setAggregate(output);
            NodeAddress[] servers = client.waitForServers(1, 1, Client.WAIT_FOREVER);
            for (int i = 0; i < servers.length; i++)
            {
                log.info(servers[i]);
            }
            task.run(MonteCarloPi.class.getName(), aggregator, 10, false);
            grid.shutdownServers();
            jvms.waitFor();
        }
        finally
        {
            jvms.stop();
        }
        grid.disconnect();
        log.info("output : " + output.showResult());
    }

    public void testSimpleClientWithLocal() throws Exception
    {
        GridConfiguration config = new GridConfiguration();
        config.setGridName("test");
        config.setType(Grid.TYPE_CLIENT);
        Grid grid = config.configure();
        grid.connect();
        Client client = grid.getClient();
        String taskKey = "test task";
        Task task = client.createTask(taskKey);
        for (int i = 0; i < 10; i++)
            task.addInput(new MonteCarloPi.Input(17 * i + 1, 100000));
        final MonteCarloPi.Output output = new MonteCarloPi.Output();
        MonteCarloPi.MCPiAggregator aggregator = new MonteCarloPi.MCPiAggregator();
        aggregator.setAggregate(output);

        // Create two server JVMs.
        ServerJvms jvms = new ServerJvms(grid, 2, 2);
        jvms.start();

        try
        {
            task.run(MonteCarloPi.class.getName(), aggregator, 10, true);
            grid.shutdownServers();
            jvms.waitFor();
        }
        finally
        {
            jvms.stop();
        }
        grid.disconnect();
        log.info("output : " + output.showResult());
    }

    public void testLocalException() throws Exception
    {
        GridConfiguration config = new GridConfiguration();
        config.setGridName("test");
        config.setType(Grid.TYPE_CLIENT);
        Grid grid = config.configure();
        grid.connect();
        Client client = grid.getClient();
        String taskKey = "test task";
        Task task = client.createTask(taskKey);
        for (int i = 0; i < 10; i++)
            task.addInput(new Integer(i));
        task.addInput(new NullPointerException("Wheee!"));
        task.addInput("Huh?");
        // Create two server JVMs.
        ServerJvms jvms = new ServerJvms(grid, 2, 2);
        jvms.start();

        Exception ex = null;
        GridStatus status = null;
        try
        {
            task.run(ExceptionThrower.Processor.class.getName(), null, 10, true);
            status = grid.getGridStatus(true);
            grid.shutdownServers();
            jvms.waitFor();
        }
        catch (Exception e)
        {
            status = grid.getGridStatus(true);
            ex = e;
        }
        finally
        {
            jvms.stop();
        }
        grid.disconnect();
        showStatus(status);
        assertNotNull(ex);
        log.info("Done.");
    }

    private void showStatus(GridStatus status)
    {
        log.info("--- Status: " + status.getNumberOfNodes() + " nodes ---");
        Iterator iter = status.iterator();
        while (iter.hasNext())
        {
            NodeStatus nodeStatus = (NodeStatus) iter.next();
            log.info(nodeStatus.toString());
        }
    }

    public void testBackgroundTask() throws Exception
    {
        GridConfiguration config = new GridConfiguration();
        config.setGridName("test");
        config.setType(Grid.TYPE_CLIENT);
        Grid grid = config.configure();
        grid.connect();
        Client client = grid.getClient();
        List input = new ArrayList();
        for (int i = 0; i < 10; i++)
            input.add(new MonteCarloPi.Input(17 * i + 1, 10000));

        // Create two server JVMs.
        ServerJvms jvms = new ServerJvms(grid, 2, 2);
        jvms.start();

        try
        {
            backgroundThree(input, client);
            grid.shutdownServers();
            jvms.waitFor();
        }
        finally
        {
            jvms.stop();
        }
        grid.disconnect();
    }

    private void backgroundThree(List input, Client client)
    {
        log.info("Background task 1...");
        TaskRequest request = new TaskRequest(
                MonteCarloPi.class.getName(),
                MonteCarloPi.MCPiAggregator.class.getName(),
                10, input, "task1");
        client.background(request, Client.WAIT_FOREVER);
        log.info("Background task 2...");
        request = new TaskRequest(
                MonteCarloPi.class.getName(),
                MonteCarloPi.MCPiAggregator.class.getName(),
                10, input, "task2");
        client.background(request, Client.WAIT_FOREVER);
        log.info("Background task 3...");
        request = new TaskRequest(
                MonteCarloPi.class.getName(),
                MonteCarloPi.MCPiAggregator.class.getName(),
                10, input, "task3");
        client.background(request, Client.WAIT_FOREVER);
    }

    public void testBackgroundTaskSingleThreaded() throws Exception
    {
        GridConfiguration config = new GridConfiguration();
        config.setGridName("test");
        config.setType(Grid.TYPE_CLIENT);
        Grid grid = config.configure();
        grid.connect();
        Client client = grid.getClient();
        List input = new ArrayList();
        for (int i = 0; i < 10; i++)
            input.add(new MonteCarloPi.Input(17 * i + 1, 10000));

        // Create two server JVMs.
        ServerJvms jvms = new ServerJvms(grid, 2, 1);
        jvms.start();

        try
        {
            backgroundThree(input, client);
            grid.shutdownServers();
            jvms.waitFor();
        }
        finally
        {
            jvms.stop();
        }
        grid.disconnect();
    }
}
