package org.jegrid;

import junit.framework.TestCase;
import org.jegrid.impl.GridImplementor;

/**
 * Tests basic create/connect methods.
 * <br>User: jdavis
 * Date: Sep 30, 2006
 * Time: 7:25:07 AM
 */
public class ConfigurationTest extends TestCase
{
    public void testConfigure() throws Exception
    {
        GridConfiguration config = new GridConfiguration();
        Grid grid = config.configure();
        assertNoClient(grid);

        config.setType(Grid.TYPE_CLIENT);
        grid = config.configure();
        assertNotNull(grid.getClient());

        config.setType(Grid.TYPE_SERVER);
        config.setGridName("test");
        grid = config.configure();
        assertNotNull(grid.getClient());
        try
        {
            grid.connect();
            assertNotNull(grid.getLocalAddress());
        }
        finally
        {
            disconnect(grid);
        }
    }

    public void test2Servers() throws Exception
    {
        GridConfiguration config = new GridConfiguration();
        config.setType(Grid.TYPE_SERVER);
        config.setGridName("test");
        // We use the internal GridImplementor interface because we need access
        // to some methods for testing.
        GridImplementor grid1 = (GridImplementor) config.configure();
        GridImplementor grid2 = (GridImplementor) config.configure();
        try
        {
            grid1.connect();
            assertNotNull(grid1.getLocalAddress());
            // Assert that there is exactly one server node.
            assertEquals(1,grid1.getNodeStatus().size());
            // Mark the grid membership as of this time, because we want to wait
            // for a membership change beyond this one.
            int mark = grid1.nextMembershipChange();
            grid2.connect();
            assertNotNull(grid2.getLocalAddress());
            // Assert that there are exactly two server nodes, according to both grid interfaces.
            assertEquals(2,grid2.getNodeStatus().size());
            // However, it might take grid1 a while to figure out that grid2 has joined (membership
            // updates are async!).
            grid1.waitForMembershipChange(mark,5000);
            assertEquals(2,grid1.getNodeStatus().size());
        }
        finally
        {
            disconnect(grid1);
            disconnect(grid2);
        }
    }

    private void disconnect(Grid grid1)
    {
        try
        {
            grid1.disconnect();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void assertNoClient(Grid grid)
    {
        GridException ge = null;
        try
        {
            grid.getClient();
        }
        catch (GridException e)
        {
            ge = e;
        }
        assertNotNull(ge);
    }
}
