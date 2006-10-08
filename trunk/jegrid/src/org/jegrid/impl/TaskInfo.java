package org.jegrid.impl;

import org.jegrid.NodeAddress;

import java.io.Serializable;

/**
 * Information about a task that is going to be assigned.
 * <br> User: jdavis
 * Date: Oct 7, 2006
 * Time: 10:10:21 AM
 */
public class TaskInfo implements Serializable
{
    private NodeAddress client;
    private int taskId;
    private String taskClassName;

    public TaskInfo(NodeAddress client, int taskId, String taskClassName)
    {
        this.client = client;
        this.taskId = taskId;
        this.taskClassName = taskClassName;
    }

    public NodeAddress getClient()
    {
        return client;
    }

    public int getTaskId()
    {
        return taskId;
    }

    public String getTaskClassName()
    {
        return taskClassName;
    }

}
