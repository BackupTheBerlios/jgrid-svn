package org.jegrid;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.jegrid.impl.NodeStatusImpl;
import org.jegrid.impl.ServerComparator;

import java.util.ArrayList;
import java.util.Arrays;
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
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 100, 100, 0, 20, startTime, 0, lastTaskAccepted),
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 100, 100, 0, 10, startTime, 0, lastTaskAccepted),
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 100, 100, 19, 25, startTime, 0, lastTaskAccepted),
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 100, 100, 9, 10, startTime, 0, lastTaskAccepted),
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 200, 200, 10, 10, startTime, 0, lastTaskAccepted),
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 100, 100, 10, 10, startTime, 0, lastTaskAccepted),
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 100, 100, 8, 10, startTime, 0, lastTaskAccepted),
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

    public void testSimpleClient() throws Exception
    {
        GridConfiguration config = new GridConfiguration();
        config.setGridName("test");
        config.setType(Grid.TYPE_CLIENT);
        Grid grid = config.configure();
        grid.connect();
        Client client = grid.getClient();
        Task task = client.createTask();
        for (int i = 0; i < 10; i++)
            task.addInput(new MonteCarloPi.Input(17 * i + 1, 10000));
        final MonteCarloPi.Output output = new MonteCarloPi.Output();
        MonteCarloPi.MCPiAggregator aggregator = new MonteCarloPi.MCPiAggregator();
        aggregator.setAggregate(output);
        task.run(MonteCarloPi.class.getName(), aggregator, 10, false);
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
        Task task = client.createTask();
        for (int i = 0; i < 10; i++)
            task.addInput(new MonteCarloPi.Input(17 * i + 1, 10000));
        final MonteCarloPi.Output output = new MonteCarloPi.Output();
        MonteCarloPi.MCPiAggregator aggregator = new MonteCarloPi.MCPiAggregator();
        aggregator.setAggregate(output);
        task.run(MonteCarloPi.class.getName(), aggregator, 10, true);
        log.info("output : " + output.showResult());
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
        TaskRequest request = new TaskRequest(
                MonteCarloPi.class.getName(),
                MonteCarloPi.MCPiAggregator.class.getName(),
                10, input);
        log.info("Background task 1...");
        client.background(request);
        log.info("Background task 2...");
        client.background(request);
        log.info("Background task 3...");
        client.background(request);
    }

}
