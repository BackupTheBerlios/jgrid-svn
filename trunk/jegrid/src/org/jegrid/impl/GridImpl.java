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
    private MicroContainer mc;
    private Membership membership;

    public GridImpl(GridConfiguration config, MicroContainer mc)
    {
        this.config = config;
        this.mc = mc;   // Lazily access the other components to avoid circular dependency.
        membership = new Membership(this);
    }

    public Client getClient()
    {
        Client client = (Client) mc.getComponentInstance(Client.class);
        if (client == null)
            throw new GridException("This configuration is not a client.");
        return client;
    }

    public Server getServer()
    {
        return (Server) mc.getComponentInstance(Server.class);
    }

    public void connect()
    {
        getBus().connect();
    }

    public void disconnect()
    {
        getBus().disconnect();
    }

    public NodeAddress getLocalAddress()
    {
        return getBus().getAddress();
    }

    public Collection getNodeStatus()
    {
        return membership.getNodeStatus();
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
    }

    private Bus getBus()
    {
        // This allows us to have gridImpl and the bus in the same container.
        // Otherwise, we'd have circular dependency.
        return (Bus) mc.getComponentInstance(Bus.class);
    }
}
