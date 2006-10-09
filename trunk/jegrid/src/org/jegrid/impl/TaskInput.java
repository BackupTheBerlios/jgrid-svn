package org.jegrid.impl;

import org.jegrid.NodeAddress;
import org.jegrid.TaskData;

/**
 * The status of an input element for a task.
 * Input id, the input data, and the server that it went to for processing.
 */
class TaskInput
{
    private NodeAddress server;
    private TaskData input;
    private Integer inputId;
    private int retries;

    public TaskInput(TaskData data)
    {
        input = data;
        inputId = new Integer(data.getInputId());
        retries = 0;
    }

    public NodeAddress getServer()
    {
        return server;
    }

    public void setServer(NodeAddress server)
    {
        this.server = server;
    }

    public TaskData getInput()
    {
        return input;
    }

    public Integer getInputId()
    {
        return inputId;
    }

    public int getRetries()
    {
        return retries;
    }

    public void incrementRetries()
    {
        retries++;
    }
}
