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
    private static final long serialVersionUID = -8698235516175764007L;

    private NodeAddress address;
    private NodeAddress coordinator;
    private int type = Grid.TYPE_UNKNOWN;
    private long freeMemory;
    private long totalMemory;
    private int freeThreads;
    private int totalThreads;

    public NodeStatusImpl(NodeAddress address)
    {
        this.address = address;
    }

    public NodeStatusImpl(NodeAddress address, int type, NodeAddress coordinator, long freeMemory, long totalMemory, int freeThreads, int totalThreads)
    {
        this.address = address;
        this.type = type;
        this.coordinator = address;
        this.freeMemory = freeMemory;
        this.totalMemory = totalMemory;
        this.freeThreads = freeThreads;
        this.totalThreads = totalThreads;
    }

    public NodeAddress getNodeAddress()
    {
        return address;
    }

    public NodeAddress getCoordinator()
    {
        return coordinator;
    }

    public int getType()
    {
        return type;
    }

    public int getAvailableWorkers()
    {
        return freeThreads;
    }

    public int getTotalWorkers()
    {
        return totalThreads;
    }

    public long getFreeMemory()
    {
        return freeMemory;
    }

    private String getNodeType()
    {
        switch (type)
        {
            case Grid.TYPE_OBSERVER:
                return "OBSERVER";
            case Grid.TYPE_SERVER:
                return "SERVER  ";
            case Grid.TYPE_CLIENT:
                return "CLIENT  ";
            case Grid.TYPE_UNKNOWN:
            default:
                return "UNKNOWN ";

        }
    }

    public String toString()
    {
        return "NodeStatusImpl{" +
                "address=" + address +
                ", type=" + getNodeType() +
                ", coordinator=" + coordinator +
                ", freeMemory=" + freeMemory +
                ", totalMemory=" + totalMemory +
                ", freeThreads=" + freeThreads +
                ", totalThreads=" + totalThreads +
                '}';
    }
}