package org.jegrid;

import org.jegrid.impl.Bus;
import org.jegrid.impl.GridImplementor;
import org.jegrid.impl.Server;
import org.jegrid.util.MicroContainer;

import java.util.Hashtable;

/**
 * Holds configuration properties for JEGrid.<br>
 * <br>User: jdavis
 * Date: Sep 21, 2006
 * Time: 5:15:46 PM
 */
public class GridConfiguration
{
    // Implementations
    private static final String GRID_IMPL = "org.jegrid.impl.GridImpl";
    private static final String BUS_IMPL = "org.jegrid.jgroups.JGroupsBus";
    private static final String CLIENT_IMPL = "org.jegrid.impl.ClientImpl";
    private static final String SERVER_IMPL = "org.jegrid.impl.ServerImpl";

    private String gridImplClass = GRID_IMPL;
    private String busImplClass = BUS_IMPL;
    private String clientImplClass = CLIENT_IMPL;
    private String serverImplClass = SERVER_IMPL;

    // Properties

    private static final int DEFAULT_THREAD_POOL_SIZE = 2;
    private static final String DEFAULT_BUS_CONFIG = "org/jegrid/jgroups/default.xml";

    private String gridName;
    private int type = Grid.TYPE_OBSERVER;
    private int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
    private String busConfiguration = DEFAULT_BUS_CONFIG;
    private boolean distributedLoggingEnabled = false;
    
    public String getBusConfiguration()
    {
        return busConfiguration;
    }

    /**
     * Returns the name of the grid.  All nodes on the same network with the same grid name will federate
     * into the same grid.
     *
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

    public int getThreadPoolSize()
    {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize)
    {
        this.threadPoolSize = threadPoolSize;
    }


    public boolean isDistributedLoggingEnabled()
    {
        return distributedLoggingEnabled;
    }

    public void setDistributedLoggingEnabled(boolean distributedLoggingEnabled)
    {
        this.distributedLoggingEnabled = distributedLoggingEnabled;
    }

    /**
     * Creates the main Grid implementation.  Use this object to observe or submit jobs to the grid.  Each
     * call will create a new instance, so be careful.  Usually an application will want to have only one instance
     * of Grid per JVM.
     *
     * @return the main Grid implementation
     */
    public Grid configure() throws ClassNotFoundException
    {
        MicroContainer mc = new MicroContainer();
        // Register the implementation class names with the container.
        mc.registerSingleton(Grid.class, gridImplClass);
        mc.registerSingleton(Bus.class, busImplClass);
        mc.registerSingleton(Server.class, serverImplClass);
        mc.registerSingleton(Client.class, clientImplClass);
        // Register this configuration with the container.
        mc.registerComponentInstance(this);
        // This will perform the constructor dependency injection.
        GridImplementor grid = (GridImplementor) mc.getComponentInstance(Grid.class);
        // Perform the rest of the initialization that cannot be done with DI.
        grid.initialize(mc);
        return grid;
    }

    public GridConfiguration setBusConfiguration(String busConfiguration)
    {
        this.busConfiguration = busConfiguration;
        return this;
    }

    public Hashtable getInitialContextEnvironment()
    {
        return null;
    }

    public String getJmsConnectionFactoryName()
    {
        return "jms/ConnectionFactory";
    }

    public String getJmsDestinationName()
    {
        return "";
    }

    public long getJmsReceiveTimeout()
    {
        return 0;
    }
}
