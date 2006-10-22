package org.jegrid.impl;

import org.jegrid.TaskId;

import java.io.Serializable;

/**
 * Information about a task that is going to be assigned.
 * <br> User: jdavis
 * Date: Oct 7, 2006
 * Time: 10:10:21 AM
 */
public class TaskInfo extends TaskId implements Serializable
{
    private TaskId id;
    // TODO: Get rid of this.
    private String taskClassName;

    public TaskInfo(TaskId id, String taskClassName)
    {
        super(id);
        this.taskClassName = taskClassName;
    }

    public String getTaskClassName()
    {
        return taskClassName;
    }

    public void setTaskClassName(String taskClassName)
    {
        this.taskClassName = taskClassName;
    }
}
