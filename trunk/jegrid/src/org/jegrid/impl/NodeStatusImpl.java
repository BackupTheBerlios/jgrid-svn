package org.jegrid.impl;

import org.jegrid.NodeAddress;
import org.jegrid.NodeStatus;
import org.jegrid.Grid;

/**
 * The detailed state of a node in the grid.
 * <br>User: Joshua Davis
 * Date: Oct 3, 2006
 * Time: 7:17:01 AM
 */
public class NodeStatusImpl implements NodeStatus
{
    private NodeAddress address;
    private int type = Grid.TYPE_UNKNOWN;

    public NodeStatusImpl(NodeAddress address)
    {
        this.address = address;
    }

    public NodeAddress getNodeAddress()
    {
        return address;
    }

    public int getType()
    {
        return type;
    }

    public String toString()
    {
        return "NodeStatusImpl{" +
                "address=" + address +
                ", type=" + type +
                '}';
    }
}
