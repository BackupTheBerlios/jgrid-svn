package org.jegrid.impl;

import org.jegrid.TaskRunnable;
import org.jegrid.NodeAddress;
import org.jegrid.impl.TaskData;

import java.io.Serializable;

/**
 * TODO: Add class level javadoc
 * <br> User: jdavis
 * Date: Oct 7, 2006
 * Time: 10:10:32 AM
 */
public class Worker implements Runnable
{
    private ServerImpl server;
    private TaskInfo task;

    public Worker(ServerImpl server, TaskInfo task)
    {
        this.server = server;
        this.task = task;
    }

    public void run()
    {
        // Get the next input from the client's queue of inputs for the task.
        NodeAddress client = task.getClient();
        int taskId = task.getTaskId();
        TaskData input = server.getNextInput(client, taskId);
        TaskRunnable taskInstance = null;
        while (input != null)
        {
            if (taskInstance == null)
                taskInstance = server.instantiateTaskRunnable(task.getTaskClassName());
            int inputId = input.getInputId();
            Serializable data = taskInstance.run(inputId, input.getData());
            TaskData output = new TaskData(inputId, data);
            server.putOutput(client, taskId, inputId, output);
        }
    }
}
