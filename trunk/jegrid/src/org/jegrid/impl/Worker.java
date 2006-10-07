package org.jegrid.impl;

import org.jegrid.TaskRunnable;
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
    private ServerTask task;

    public Worker(ServerImpl server, ServerTask task)
    {
        this.server = server;
        this.task = task;
    }

    public void run()
    {
        // Get the next input from the client's queue of inputs for the task.
        TaskData input = server.getNextInput(task);
        TaskRunnable taskInstance = null;
        while (input != null)
        {
            if (taskInstance == null)
                taskInstance = server.instantiateTaskRunnable(task.getTaskClass());
            int id = input.getInputId();
            Serializable data = taskInstance.run(id,input.getData());
            TaskData output = new TaskData(id,data);
            server.putOutput(task,output);
        }
    }
}
