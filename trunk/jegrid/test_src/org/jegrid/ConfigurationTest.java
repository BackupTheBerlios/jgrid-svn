package org.jegrid;

import junit.framework.TestCase;

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
        assertNoServer(grid);

        config.setType(Grid.TYPE_SERVER);
        config.setGridName("test");
        grid = config.configure();
        assertNotNull(grid.getClient());
        assertNotNull(grid.getServer());
        grid.connect();
        assertNotNull(grid.getLocalAddress());
        grid.disconnect();
    }

    public void test2Servers() throws Exception
    {
        GridConfiguration config = new GridConfiguration();
        config.setType(Grid.TYPE_SERVER);
        config.setGridName("test");
        Grid grid1 = config.configure();
        Grid grid2 = config.configure();
        grid1.connect();
        assertNotNull(grid1.getLocalAddress());
        grid2.connect();
        assertNotNull(grid2.getLocalAddress());
        grid1.disconnect();
        grid2.disconnect();
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

    private void assertNoServer(Grid grid)
    {
        GridException ge = null;
        try
        {
            grid.getServer();
        }
        catch (GridException e)
        {
            ge = e;
        }
        assertNotNull(ge);
    }
}
