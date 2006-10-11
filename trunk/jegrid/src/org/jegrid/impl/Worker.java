package org.jegrid.impl;

import org.jegrid.TaskRunnable;
import org.jegrid.NodeAddress;
import org.jegrid.TaskData;
import org.jegrid.GridException;
import org.apache.log4j.NDC;

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
    private final ServerImpl server;
    private final TaskInfo task;
    private Bus bus;
    private Latch goLatch;
    private Exception exception;
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
        NDC.push(task.getClient().toString() + "-" + task.getTaskId());
        try
        {
            doWork();
        }
        finally
        {
            NDC.pop();
            server.done(task);
        }
    }

    private void doWork()
    {
        // Get the next input from the client's queue of inputs for the task.
        try
        {
            // Wait for the client to say go.
            boolean okay = goLatch.attempt(GO_TIMEOUT);
            if (!okay)
                throw new GridException("Timeout waiting for 'go' from client.");

            // Paranoid checking.
            if (task.getClient() == null)
                throw new GridException("No client address!");
            if (task.getTaskClassName() == null)
                throw new GridException("No task class name!");
            runTask();
        }
        catch (GridException e)
        {
            handleException(e);
        }
        catch (Exception e)
        {
            GridException ge = new GridException(e);
            handleException(ge);
        }
    }

    private void runTask()
            throws Exception
    {
        // Priming read.
        TaskData input = nextInput(task.getClient(), task.getTaskId());
        if (shouldRun(input))
        {
            // Create the task instance and run until there isn't any more input.
            TaskRunnable taskInstance = server.instantiateTaskRunnable(task.getTaskClassName());
            while (shouldRun(input))
            {
                int inputId = input.getInputId();
                Serializable data = taskInstance.run(inputId, input.getData());
                TaskData output = new TaskData(inputId, data);
                bus.putOutput(task.getClient(), task.getTaskId(), output);
                input = nextInput(task.getClient(), task.getTaskId());
            }
        }
    }

    private boolean shouldRun(TaskData input) throws Exception
    {
        Exception ex = getException();
        if (ex != null)
            throw ex;
        return input != null;
    }

    private void handleException(GridException ge)
    {
        //noinspection EmptyCatchBlock
        try
        {
            bus.taskFailed(task.getClient(), task.getTaskId(), ge);
        }
        catch (Exception ignore)
        {
        }
    }

    private TaskData nextInput(NodeAddress client, int taskId)
            throws RpcTimeoutException
    {
        return bus.getNextInput(client, taskId);
    }


    public synchronized Exception getException()
    {
        return exception;
    }

    public synchronized void setException(Exception exception)
    {
        this.exception = exception;
    }

    public void go(TaskInfo taskInfo)
    {
        if (!task.equals(taskInfo))
            setException(new IllegalStateException("Go task " + taskInfo + " is not the same as assigned task " + task));
        if (task.getTaskClassName() == null && taskInfo.getTaskClassName() != null)
            task.setTaskClassName(taskInfo.getTaskClassName());
        goLatch.release();
    }
}
