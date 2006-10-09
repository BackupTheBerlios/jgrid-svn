package org.jegrid.impl;

import org.jegrid.TaskRunnable;
import org.jegrid.NodeAddress;
import org.jegrid.TaskData;
import org.jegrid.GridException;

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
    private Bus bus;

    public Worker(ServerImpl server, TaskInfo task, Bus bus)
    {
        this.server = server;
        this.task = task;
        this.bus = bus;
    }

    public void run()
    {
        // Get the next input from the client's queue of inputs for the task.
        NodeAddress client = task.getClient();
        int taskId = task.getTaskId();
        try
        {
            TaskData input = null;
            // TODO: Wait for the 'go' message from the client.
            input = bus.getNextInput(client, taskId);
            TaskRunnable taskInstance = server.instantiateTaskRunnable(task.getTaskClassName());
            while (input != null)
            {
                int inputId = input.getInputId();
                Serializable data = taskInstance.run(inputId, input.getData());
                TaskData output = new TaskData(inputId, data);
                bus.putOutput(client, taskId, output);
                input = bus.getNextInput(client, taskId);
            }
        }
        catch (Exception e)
        {
            //noinspection EmptyCatchBlock
            try
            {
                bus.taskFailed(client,taskId,new GridException(e));
            }
            catch (Exception ignore)
            {
            }
        }
    }
}
