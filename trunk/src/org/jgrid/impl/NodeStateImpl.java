package org.jgrid.impl;

import org.jgroups.Address;
import org.jgrid.PeerInfo;

import java.io.Serializable;

/**
 * <br>User: Joshua Davis
 * <br>Date: Oct 2, 2005 Time: 10:09:39 AM
 */
public class NodeStateImpl implements Serializable, PeerInfo
{
    private Address address;
    private String name;
    private int processors;
    private long freeMemory;
    private long totalMemory;
    private int status;
    private int freeThreads;
    private boolean server;

    public NodeStateImpl(Address address,String name)
    {
        this.address = address;
        this.name = name;
    }

    public String getAddress()
    {
        return (address == null) ? "" : address.toString();
    }

    public Address getNodeAddress()
    {
        return address;
    }

    public String getName()
    {
        return name;
    }

    public int getStatus()
    {
        return status;
    }

    public int getProcessors()
    {
        return processors;
    }

    public long getFreeMemory()
    {
        return freeMemory;
    }

    public long getTotalMemory()
    {
        return totalMemory;
    }

    public int getFreeThreads()
    {
        return freeThreads;
    }

    public String toString()
    {
        return "NodeStateImpl{" +
                "address=" + address +
                ", name='" + name + '\'' +
                ", processors=" + processors +
                ", freeMemory=" + freeMemory +
                ", totalMemory=" + totalMemory +
                ", status=" + status +
                ", freeThreads=" + freeThreads +
                ", server=" + server +
                '}';
    }

    public void updateRuntimeStats(Runtime rt)
    {
        processors = rt.availableProcessors();
        freeMemory = rt.freeMemory();
        totalMemory = rt.totalMemory();
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

    public void setFreeThreads(int freeThreads)
    {
        this.freeThreads = freeThreads;
    }

    public boolean isServer()
    {
        return server;
    }

    public void setServer(boolean server)
    {
        this.server = server;
    }
}
