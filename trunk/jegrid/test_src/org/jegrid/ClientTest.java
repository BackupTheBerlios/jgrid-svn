package org.jegrid;

import junit.framework.TestCase;
import org.jegrid.impl.NodeStatusImpl;
import org.jegrid.impl.ServerComparator;

import java.util.Arrays;

/**
 * TODO: Add class level javadoc.
 * <br>User: Joshua Davis
 * Date: Oct 8, 2006
 * Time: 10:04:52 AM
 */
public class ClientTest extends TestCase
{
    public void testServerComparator() throws Exception
    {
        // Nodes with zero free threads go to the bottom.
        // Nodes with a larger percentage of free threads are preferred.
        // Nodes with a larger percentage of free memory per free thread are preferred.
        NodeStatusImpl[] nodes = new NodeStatusImpl[]{
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 100, 100, 0, 20),
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 100, 100, 0, 10),
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 100, 100, 19, 25),
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 100, 100, 9, 10),
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 200, 200, 10, 10),
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 100, 100, 10, 10),
                new NodeStatusImpl(null, Grid.TYPE_SERVER, null, 100, 100, 8, 10),
        };
        Arrays.sort(nodes, new ServerComparator());
        assertEquals(10, nodes[0].getFreeThreads());
        assertEquals(200, nodes[0].getFreeMemory());
        assertEquals(10, nodes[1].getFreeThreads());
        assertEquals(100, nodes[1].getFreeMemory());
        assertEquals(9, nodes[2].getFreeThreads());
        assertEquals(8, nodes[3].getFreeThreads());
        assertEquals(19, nodes[4].getFreeThreads());
        assertEquals(0, nodes[nodes.length - 1].getFreeThreads());
    }

    public void testSimpleClient() throws Exception
    {
        GridConfiguration config = new GridConfiguration();
        config.setGridName("test");
        config.setType(Grid.TYPE_CLIENT);
        Grid grid = config.configure();
        grid.connect();
        Client client = grid.getClient();
        Task task = client.createTask(MonteCarloPi.class);
        for (int i = 0; i < 10; i++)
            task.addInput(new MonteCarloPi.Input(17 * i + 1, 10000));
        final MonteCarloPi.Output aggregate = new MonteCarloPi.Output();
        task.run(new Aggregator()
        {

            public void aggregate(TaskData output)
            {
                MonteCarloPi.Output out = (MonteCarloPi.Output) output.getData();
                System.out.println("# " + output.getInputId() + " : " + out.showResult());
                aggregate.aggregate(out);
            }
        }, 10);
        System.out.println("aggregate : " + aggregate.showResult());
    }
}
