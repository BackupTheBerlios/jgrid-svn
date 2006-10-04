package org.jgrid.impl;

import EDU.oswego.cs.dl.util.concurrent.WaitableInt;
import org.jgroups.Address;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * Maintains a lost of all Nodes of the grid. Does so by
 * maintaining a Map of NodeStateImpl to its Address.
 *
 * @see org.jgrid.impl.NodeStateImpl
 * @see org.jgroups.Address
 *
 * User: Joshua Davis<br>
 * Date: Oct 2, 2005<br>
 * Time: 10:07:40 AM<br>
 */
class GridStateImpl implements Serializable
{
    private Map nodeStateByAddress = new HashMap();
    private transient WaitableInt numberOfServers;

    NodeStateImpl getNodeState(Address address)
    {
        synchronized (this)
        {
            return (NodeStateImpl) nodeStateByAddress.get(address);
        }
    }

    public String toString()
    {
        return "GridStateImpl{" +
                "nodeStateByAddress=" + nodeStateByAddress +
                '}';
    }

    void handleUpdate(NodeStateImpl state)
    {
        add(state);
    }

    private void add(NodeStateImpl state)
    {
        NodeStateImpl old = null;
        synchronized(this)
        {
            Address key = state.getNodeAddress();
            if (nodeStateByAddress.containsKey(key))
                old = (NodeStateImpl) nodeStateByAddress.remove(key);
            nodeStateByAddress.put(key,state);
            if (numberOfServers == null)
            {
                int servers = 0;
                for (Iterator iterator = nodeStateByAddress.values().iterator(); iterator.hasNext();)
                {
                    NodeStateImpl n = (NodeStateImpl) iterator.next();
                    if (n.isServer())
                        servers++;
                }
                numberOfServers = new WaitableInt(servers);
            }
            else
            {
                // New server nodes, or existing nodes that have become servers.
                if (state.isServer() && (old == null || !old.isServer()))
                    numberOfServers.increment();
                // The node used to be a server, but now it's not.
                else if (!state.isServer() && old != null && old.isServer())
                    numberOfServers.decrement();
            }
        }
    }

    void initialize(NodeStateImpl state)
    {
        add(state);
    }

    public Collection getAllNodes()
    {
        synchronized(this)
        {
            return nodeStateByAddress.values();
        }
    }

    public void waitForServers() throws InterruptedException
    {
        numberOfServers.whenGreater(0,null);
    }
}
