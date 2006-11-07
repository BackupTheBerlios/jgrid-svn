package org.jegrid.impl;

import org.apache.log4j.Logger;
import org.jegrid.*;
import org.jegrid.util.MicroContainer;

import java.util.Set;
import java.util.List;
import java.util.Iterator;

/**
 * Implements a connection to the grid based on the Bus abstraction, manages
 * the other two components: client and server.
 * <br>User: jdavis
 * Date: Sep 21, 2006
 * Time: 5:34:18 PM
 */
public class GridImpl implements GridImplementor
{
    private static Logger log = Logger.getLogger(GridImpl.class);
    private GridConfiguration config;
    private Membership membership;
    private Bus bus;
    private Server server;
    private ClientImplementor client;
    private long startTime;
    private MicroContainer singletons;

    public GridImpl(GridConfiguration config)
    {
        this.config = config;
        membership = new Membership(this);
        singletons = new MicroContainer();
    }

    public void initialize(MicroContainer mc)
    {
        // These objects need the GridImplementor in the micro-container, so
        // we ask the container to construct them now to avoid circular dependency.
        bus = (Bus) mc.getComponentInstance(Bus.class);
        switch (config.getType())
        {
            case TYPE_SERVER:
                server = (Server) mc.getComponentInstance(Server.class);
            case TYPE_CLIENT:
                client = (ClientImplementor) mc.getComponentInstance(Client.class);
            default:
                break;
        }
    }

    public Client getClient()
    {
        return client;
    }

    public Server getServer()
    {
        return server;
    }

    public void connect()
    {
        startTime = System.currentTimeMillis();
        bus.connect();
    }

    public void disconnect()
    {
        bus.disconnect();
    }

    public NodeAddress getLocalAddress()
    {
        return bus.getAddress();
    }

    public NodeStatus getLocalStatus()
    {
        Runtime rt = Runtime.getRuntime();
        int freeThreads = (server == null) ? 0 : server.freeThreads();
        int totalThreads = (server == null) ? 0 : server.totalThreads();
        int tasksAccepted = (server == null) ? 0 : server.tasksAccepted();
        long lastTaskAccepted = (server == null) ? 0 : server.lastTaskAccepted();
        return new NodeStatusImpl(
                bus.getAddress(),
                config.getType(),
                membership.getCoordinator(),
                rt.freeMemory(),
                rt.totalMemory(),
                freeThreads,
                totalThreads,
                startTime,
                tasksAccepted,
                lastTaskAccepted);
    }

    public GridStatus getGridStatus(boolean cached)
    {
        if (!cached || membership.getNumberOfUnknownNodes() > 0)
        {
            NodeStatus[] ns = bus.getGridStatus();
            membership.refreshStatus(ns);
        }
        return membership;
    }

    public void runServer()
    {
        if (server == null)
            throw new GridException("This node is not configured as a server.");
        log.info("Starting server...");
        server.run();
    }

    public int nextMembershipChange()
    {
        return membership.nextMembershipChange();
    }

    public void waitForMembershipChange(int mark, long timeout)
    {
        membership.waitForMembershipChange(mark, timeout);
    }

    // Callback methods for grid membership invoked by the bus listener.

    public void onMembershipChange(Set joined, Set left)
    {
        membership.onMembershipChange(joined, left);
        if (client != null)
            client.onMembershipChange(joined, left);
    }

    public void onNodeStatus(NodeStatus from)
    {
        membership.onNodeStatus(from);
    }

    public void onNewCoordinator(NodeAddress address)
    {
        // If I am the coordinator, then I should try to instantiate all the grid singletons
        if (address.equals(getLocalAddress()))
        {
            log.info("*** I am the coordinator ***");
            List list = config.getGridSingletonDescriptors();
            for (Iterator iterator = list.iterator(); iterator.hasNext();)
            {
                GridSingletonDescriptor descriptor = (GridSingletonDescriptor) iterator.next();
                singletons.registerSingleton(descriptor.getKey(),descriptor.getImpl());
            }
            // Now create them all.
            for (Iterator iterator = list.iterator(); iterator.hasNext();)
            {
                GridSingletonDescriptor descriptor = (GridSingletonDescriptor) iterator.next();
                Object component = singletons.getComponentInstance(descriptor.getKey());
                if (component instanceof LifecycleAware)
                {
                    LifecycleAware lifecycleAware = (LifecycleAware) component;
                    lifecycleAware.initialize();
                }
            }
        }
        membership.onNewCoordinator(address);
    }

    public void waitForServers() throws InterruptedException
    {
        membership.waitForServers();
    }

    public Object instantiateObject(String clazz)
    {
        Object o = null;
        try
        {
            Class aClass = Thread.currentThread().getContextClassLoader().loadClass(clazz);
            o = aClass.newInstance();
            if (o instanceof LifecycleAware)
            {
                LifecycleAware lifecycleAware = (LifecycleAware) o;
                lifecycleAware.initialize();
            }
        }
        catch (Exception e)
        {
            throw new GridException(e);
        }
        return o;
    }
}
