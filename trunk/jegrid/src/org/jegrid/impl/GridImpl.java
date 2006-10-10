package org.jegrid.impl;

import org.jegrid.*;
import org.jegrid.util.MicroContainer;
import org.apache.log4j.Logger;

import java.util.*;

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
    private NodeStatusImpl localStatus;

    public GridImpl(GridConfiguration config)
    {
        this.config = config;
        membership = new Membership(this);
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
        return new NodeStatusImpl(
                bus.getAddress(),
                config.getType(),
                membership.getCoordinator(),
                rt.freeMemory(),
                rt.totalMemory(),
                freeThreads,
                totalThreads
        );
    }

    public GridStatus getGridStatus(boolean cached)
    {
        if (!cached)
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

    public void onHello(NodeStatus from)
    {
        membership.onHello(from);
    }

    public void onNewCoordinator(NodeAddress address)
    {
        membership.onNewCoordinator(address);
    }
}
