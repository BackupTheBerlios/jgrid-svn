package org.jegrid.impl;

import org.jegrid.Task;
import org.jegrid.NodeAddress;

/**
 * Client side representation of a task.
 * <br> User: jdavis
 * Date: Oct 7, 2006
 * Time: 12:05:37 PM
 */
public class TaskImpl implements Task
{
    private ClientImpl client;
    private TaskInfo info;

    public TaskImpl(ClientImpl client, int taskId, String taskClass)
    {
        this.client = client;
        this.info = new TaskInfo(client.getBus().getAddress(), taskId, taskClass);
    }

    public int getTaskId()
    {
        return info.getTaskId();
    }

    public void start()
    {
        NodeAddress[] servers = client.getSeverAddresses();
        // Send the assign message.
        Bus bus = client.getBus();
    }
}
