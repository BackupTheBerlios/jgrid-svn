package org.jegrid;

import org.apache.log4j.Logger;

/**
 * TODO: Add class level javadoc.
 * <br>User: Joshua Davis
 * Date: Nov 6, 2006
 * Time: 9:59:27 PM
 */
public class GridSingletonTest
{
    private static Logger log = Logger.getLogger(GridSingletonTest.class);

    public static class Thingie implements LifecycleAware
    {
        public void initialize()
        {
            log.info("initialize()");
        }

        public void terminate()
        {
            log.info("terminate()");
        }

        public Thingie()
        {
            log.info("<ctor>()");
        }
    }

    public static void main(String[] args)
    {
        try
        {
            GridConfiguration config = new GridConfiguration();
            config.setGridName(args[0]);
            config.setType(Grid.TYPE_SERVER);
            config.addGridSingletonDescriptor(
                    new GridSingletonDescriptor(
                            Thingie.class,
                            Thingie.class)
            );
            Grid grid = config.configure();
            grid.runServer();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
