package org.jegrid;

import java.io.Serializable;

/**
 * The unique id for a task.
 * <br> User: jdavis
 * Date: Oct 7, 2006
 * Time: 10:10:21 AM
 */
public class TaskId implements Serializable
{
    private NodeAddress client;
    private int taskId;
    private Serializable taskKey;

    public TaskId(NodeAddress client, int taskId, Serializable taskKey)
    {
        this.client = client;
        this.taskId = taskId;
        this.taskKey = taskKey;
    }

    public TaskId(TaskId id)
    {
        this(id.client, id.taskId, null);
    }

    public NodeAddress getClient()
    {
        return client;
    }

    public int getTaskId()
    {
        return taskId;
    }

    public Serializable getTaskKey()
    {
        return taskKey;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskId taskInfo = (TaskId) o;

        if (taskId != taskInfo.taskId) return false;
        if (!client.equals(taskInfo.client)) return false;

        return true;
    }

    public int hashCode()
    {
        int result;
        result = client.hashCode();
        result = 31 * result + taskId;
        return result;
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(client);
        buf.append("-");
        buf.append(taskId);
        return buf.toString();
    }
}
