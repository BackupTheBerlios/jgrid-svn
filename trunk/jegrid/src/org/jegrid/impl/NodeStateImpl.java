package org.jegrid.impl;

import org.jegrid.NodeAddress;

/**
 * The detailed state of a node in the grid.
 * <br>User: Joshua Davis
 * Date: Oct 3, 2006
 * Time: 7:17:01 AM
 */
public class NodeStateImpl
{
    private NodeAddress address;

    public NodeStateImpl(NodeAddress address)
    {
        this.address = address;
    }

    public String toString()
    {
        return "NodeStateImpl{" +
                "address=" + address +
                '}';
    }
}
