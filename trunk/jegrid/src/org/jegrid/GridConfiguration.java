package org.jegrid;

import org.jegrid.util.MicroContainer;
import org.jegrid.impl.Bus;
import org.jegrid.impl.Server;
import org.w3c.dom.Element;

/**
 * Holds configuration properties for JEGrid.<br>
 * <br>User: jdavis
 * Date: Sep 21, 2006
 * Time: 5:15:46 PM
 */
public class GridConfiguration
{

    private static final String GRID_IMPL = "org.jegrid.impl.GridImpl";
    private static final String BUS_IMPL = "org.jegrid.jgroups.JGroupsBus";
    private static final String CLIENT_IMPL = "org.jegrid.impl.ClientImpl";
    private static final String SERVER_IMPL = "org.jegrid.impl.ServerImpl";
    private static final String THREAD_POOL_IMPL = "org.jegrid.impl.WorkerThreadPool";

    private static final int DEFAULT_THREAD_POOL_SIZE = 2;

    private String gridImplClass = GRID_IMPL;
    private String busImplClass = BUS_IMPL;
    private String clientImplClass = CLIENT_IMPL;
    private String serverImplClass = SERVER_IMPL;

    private String gridName;
    private int type = Grid.TYPE_OBSERVER;
    private int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;

    public Element getBusConfiguration()
    {

        return null;
    }

    /**
     * Returns the name of the grid.  All nodes on the same network with the same grid name will federate
     * into the same grid.
     * @return the name of the grid
     */
    public String getGridName()
    {
        return gridName;
    }

    public int getType()
    {
        return type;
    }

    public void setType(int type)
    {
        this.type = type;
    }

    public void setGridName(String s)
    {
        gridName = s;
    }

    /**
     * Creates the main Grid implementation.  Use this object to observe or submit jobs to the grid.  Each
     * call will create a new instance, so be careful.  Usually an application will want to have only one instance
     * of Grid per JVM.
     * @return the main Grid implementation
     */
    public Grid configure() throws ClassNotFoundException
    {
        MicroContainer mc = new MicroContainer();
        // Register the implementation class names with the container.
        mc.registerSingleton(Grid.class,gridImplClass);
        mc.registerSingleton(Bus.class,busImplClass);
        // Depending on the configuration type, we register the client and server implementations.
        switch (type)
        {
            case Grid.TYPE_SERVER:
                mc.registerSingleton(Server.class,serverImplClass);
            case Grid.TYPE_CLIENT:
                mc.registerSingleton(Client.class,clientImplClass);
        }
        // Register this configuration with the container.
        mc.registerComponentInstance(this);
        // Register the microcontainer in itself (I know... wierd, but it makes sense because
        // this allows the microcontainer to resolve the constructors.
        mc.registerComponentInstance(mc);
        // This will perform the constructor dependency injection.
        return (Grid) mc.getComponentInstance(Grid.class);
    }

    public int getThreadPoolSize()
    {
        return threadPoolSize;
    }
}
