package org.jegrid.impl;

import org.jegrid.TaskRunnable;
import org.jegrid.TaskData;
import org.jegrid.GridException;
import org.apache.log4j.NDC;
import org.apache.log4j.Logger;

import java.io.Serializable;

import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * A runnable that performs the work on a server.  It instantiates the task class and begins processing input from the
 * client using the task's run() method.  The output is then sent back to the client.  This is repeated until
 * there is no more input from the client, or there was some sort of error.
 * <br> User: jdavis
 * Date: Oct 7, 2006
 * Time: 10:10:32 AM
 */
public class Worker implements Runnable
{
    private static Logger log = Logger.getLogger(Worker.class);

    private final ServerImpl server;
    private final TaskInfo task;
    private Bus bus;
    private Latch goLatch;
    private Exception exception;
    private static final long GO_TIMEOUT = 10000;
    private boolean released;

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
        if (log.isDebugEnabled())
            log.debug("Worker started on " + task.getClient() + " " + task.getTaskId());
        // Priming read.
        TaskData input = nextInput(null);
        if (shouldRun(input))
        {
            // Create the task instance and run until there isn't any more input.
            TaskRunnable taskInstance = server.instantiateTaskRunnable(task.getTaskClassName());
            while (shouldRun(input))
            {
                int inputId = input.getInputId();
                Serializable data = taskInstance.run(inputId, input.getData());
                TaskData output = new TaskData(inputId, data);
                input = nextInput(output);
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

    private TaskData nextInput(TaskData output)
            throws RpcTimeoutException
    {
        if (isReleased())
        {
            if (log.isDebugEnabled())
                log.debug("Worker released from " + task);
            return null;
        }
        return bus.getNextInput(task.getClient(), task.getTaskId(), output);
    }


    public synchronized void setReleased(boolean flag)
    {
        released = flag;
    }

    public synchronized boolean isReleased()
    {
        return released;
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
