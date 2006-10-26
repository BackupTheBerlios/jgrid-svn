package org.jegrid.impl;

import org.jegrid.TaskId;

import java.io.Serializable;

/**
 * Message sent to a worker to tell it to start processing.
 * <br>User: Joshua Davis
 * Date: Oct 26, 2006
 * Time: 7:33:34 AM
 */
public class GoMessage implements Serializable
{
    private TaskId taskId;
    private String processorClassName;
    private Serializable sharedInput;

    public GoMessage(TaskId taskId, String processorClassName, Serializable sharedInput)
    {
        this.taskId = taskId;
        this.processorClassName = processorClassName;
        this.sharedInput = sharedInput;
    }

    public TaskId getTaskId()
    {
        return taskId;
    }

    public String getProcessorClassName()
    {
        return processorClassName;
    }

    public Serializable getSharedInput()
    {
        return sharedInput;
    }
}
