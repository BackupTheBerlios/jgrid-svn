package org.jgrid.impl;

import org.jgroups.Address;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

/**
 * User: Joshua Davis<br>
 * Date: Oct 2, 2005<br>
 * Time: 10:07:40 AM<br>
 */
class GridStateImpl implements Serializable
{
    private Map nodeStateByAddress = new HashMap();

    NodeStateImpl getNodeState(Address address)
    {
        return (NodeStateImpl) nodeStateByAddress.get(address);
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
        synchronized(this)
        {
            nodeStateByAddress.put(state.getNodeAddress(),state);
        }
    }

    void initialize(NodeStateImpl state)
    {
        add(state);
    }

    public Collection getAllNodes()
    {
        return nodeStateByAddress.values();
    }
}
