package org.jegrid.impl;

import org.jegrid.Client;
import org.jegrid.Task;
import org.jegrid.NodeAddress;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: Add class level javadoc
 * <br> User: jdavis
 * Date: Sep 30, 2006
 * Time: 7:49:52 AM
 */
public class ClientImpl implements Client
{
    private int nextTaskId = 1000;
    private Map tasksById = new HashMap();
    private Bus bus;
    private GridImplementor grid;

    public ClientImpl(Bus bus, GridImplementor grid)
    {
        this.bus = bus;
        this.grid = grid;
    }

    public Task newTask(String taskClassName)
    {
        TaskImpl task = new TaskImpl(this, nextTaskId(), taskClassName);
        tasksById.put(new Integer(task.getTaskId()), task);
        return task;
    }

    private int nextTaskId()
    {
        return nextTaskId++;
    }

    public Bus getBus()
    {
        return bus;
    }

    public NodeAddress[] getSeverAddresses()
    {
        return null;
    }
}
