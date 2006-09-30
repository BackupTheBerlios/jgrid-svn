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
        grid.disconnect();
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
