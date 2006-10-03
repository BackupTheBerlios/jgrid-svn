package org.jegrid.impl;

import org.jegrid.*;
import org.jegrid.util.MicroContainer;
import org.apache.log4j.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

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
    private Map allNodesByAddress = new HashMap();

    public GridImpl(GridConfiguration config, MicroContainer mc)
    {
        this.config = config;
        this.mc = mc;
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
        Server server = (Server) mc.getComponentInstance(Server.class);
        if (server == null)
            throw new GridException("This configuration is not a server");
        return server;
    }

    private Bus getBus()
    {
        return (Bus)mc.getComponentInstance(Bus.class);
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

    // Callback methods for grid membership invoked by the bus listener.

    public void onMembershipChange(Set joined, Set left)
    {
        synchronized (allNodesByAddress)
        {
            for (Iterator iterator = joined.iterator(); iterator.hasNext();)
            {
                NodeAddress address = (NodeAddress) iterator.next();
                if (allNodesByAddress.containsKey(address))
                {
                    log.info("Node list already contains " + address);
                }
                else
                {
                    NodeStateImpl node = new NodeStateImpl(address);
                    allNodesByAddress.put(address,node);
                    log.info("Node " + node + " added.");
                }
            } // for
            for (Iterator iterator = left.iterator(); iterator.hasNext();)
            {
                NodeAddress address = (NodeAddress) iterator.next();
                if (allNodesByAddress.containsKey(address))
                {
                    allNodesByAddress.remove(address);
                    log.info("Removed " + address);
                }
                else
                {
                    log.info("Address " + address + " not found.");
                }
            } // for
        } // sync
    }
}
