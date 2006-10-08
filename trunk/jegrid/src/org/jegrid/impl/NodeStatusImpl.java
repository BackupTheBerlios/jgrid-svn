package org.jegrid.impl;

import org.jegrid.NodeAddress;
import org.jegrid.NodeStatus;
import org.jegrid.Grid;

import java.io.Serializable;

/**
 * The detailed state of a node in the grid.
 * <br>User: Joshua Davis
 * Date: Oct 3, 2006
 * Time: 7:17:01 AM
 */
public class NodeStatusImpl implements NodeStatus, Serializable
{
    private NodeAddress address;
    private int type = Grid.TYPE_UNKNOWN;
    private long freeMemory;
    private long totalMemory;
    private int freeThreads;
    private int totalThreads;

    public NodeStatusImpl(NodeAddress address)
    {
        this.address = address;
    }

    public NodeStatusImpl(NodeAddress address, int type, long freeMemory, long totalMemory, int freeThreads, int totalThreads)
    {
        this.address = address;
        this.type = type;
        this.freeMemory = freeMemory;
        this.totalMemory = totalMemory;
        this.freeThreads = freeThreads;
        this.totalThreads = totalThreads;
    }

    public NodeAddress getNodeAddress()
    {
        return address;
    }

    public int getType()
    {
        return type;
    }

    public int getFreeThreads()
    {
        return freeThreads;
    }

    public int getTotalThreads()
    {
        return totalThreads;
    }

    public long getFreeMemory()
    {
        return freeMemory;
    }

    public String toString()
    {
        return "NodeStatusImpl{" +
                "address=" + address +
                ", type=" + type +
                ", freeMemory=" + freeMemory +
                ", totalMemory=" + totalMemory +
                ", freeThreads=" + freeThreads +
                ", totalThreads=" + totalThreads +
                '}';
    }
}