package org.jegrid.impl;

import org.jegrid.TaskRunnable;
import org.jegrid.NodeAddress;
import org.jegrid.TaskData;
import org.jegrid.GridException;

import java.io.Serializable;

import EDU.oswego.cs.dl.util.concurrent.Latch;

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
    private Latch goLatch;
    private static final long GO_TIMEOUT = 10000;

    public Worker(ServerImpl server, TaskInfo task, Bus bus)
    {
        this.server = server;
        this.task = task;
        this.bus = bus;
        this.goLatch = new Latch();
    }

    public void run()
    {
        try
        {
            doWork();
        }
        finally
        {
            server.done(task);
        }
    }

    private void doWork()
    {
        // Get the next input from the client's queue of inputs for the task.
        NodeAddress client = task.getClient();
        int taskId = task.getTaskId();
        try
        {
            // Wait for the client to say go.
            boolean okay = goLatch.attempt(GO_TIMEOUT);
            if (!okay)
                throw new GridException("Timeout waiting for 'go' from client.");
            TaskData input = nextInput(client, taskId);
            TaskRunnable taskInstance = server.instantiateTaskRunnable(task.getTaskClassName());
            while (input != null)
            {
                int inputId = input.getInputId();
                Serializable data = taskInstance.run(inputId, input.getData());
                TaskData output = new TaskData(inputId, data);
                bus.putOutput(client, taskId, output);
                input = nextInput(client, taskId);
            }
        }
        catch (Exception e)
        {
            //noinspection EmptyCatchBlock
            try
            {
                bus.taskFailed(client, taskId, new GridException(e));
            }
            catch (Exception ignore)
            {
            }
        }
    }

    private TaskData nextInput(NodeAddress client, int taskId)
            throws RpcTimeoutException
    {
        return bus.getNextInput(client, taskId);
    }

    public void go()
    {
        goLatch.release();
    }

    public void release()
    {
        // TODO: Maybe interrupt the thread or something.
    }
}
