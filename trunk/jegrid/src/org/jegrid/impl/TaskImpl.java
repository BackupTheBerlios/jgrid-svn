package org.jegrid.impl;

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.Mutex;
import org.apache.log4j.Logger;
import org.jegrid.*;
import org.jegrid.jms.TaskRequest;

import java.io.Serializable;
import java.util.*;

/**
 * Client side representation of a task.
 * <br> User: jdavis
 * Date: Oct 7, 2006
 * Time: 12:05:37 PM
 */
public class TaskImpl implements Task
{
    private static final int DEFAULT_MAX_RETRIES = 3;


    private Logger log = Logger.getLogger(TaskImpl.class);

    private ClientImpl client;
    private TaskInfo info;
    private Mutex mutex;
    private List inputQueue;            // Queue of TaskData for workers.
    private Set unfinishedInputIds;     // Input ids that are queued, or in progress.
    private Set serverAddresses;        // The server addresses from the assignment.
    private Map inprogress;             // InputStatus by inputId - input that has been taken by a server.
    private Channel outputQueue;        // Queue of TaskData for the aggregator.
    private GridException failure;
    private int maxRetries = DEFAULT_MAX_RETRIES;
    private boolean running;        // True if we're already running.
    private static final int END_OF_OUTPUT = -1;
    private static final TaskData END = new TaskData(END_OF_OUTPUT, null);

    public TaskImpl(ClientImpl client, int taskId, String taskClass)
    {
        mutex = new Mutex();
        this.client = client;
        this.info = new TaskInfo(client.getBus().getAddress(), taskId, taskClass);
        this.inputQueue = new LinkedList();
        this.unfinishedInputIds = new HashSet();
        this.inprogress = new HashMap();
        this.serverAddresses = new HashSet();
        this.outputQueue = new LinkedQueue();
    }

    public int getTaskId()
    {
        return info.getTaskId();
    }

    public void addInput(Serializable input)
    {
        acquireMutex();
        try
        {
            int inputId = inputQueue.size();
            TaskData data = new TaskData(inputId, input);
            TaskInput is = new TaskInput(data);
            inputQueue.add(is);
            unfinishedInputIds.add(is.getInputId());
        }
        finally
        {
            mutex.release();
        }
    }

    /**
     * Invoked by a worker that has been assigned to this task.  It may also
     * have some output from the previous run.
     *
     * @param server The server that the worker is running on.
     * @param output The output, if there was any from the previously processed input on that worker.
     * @return The next input, or null if there is no more input.
     */
    public TaskData getNextInput(NodeAddress server, TaskData output)
    {
        acquireMutex();
        try
        {

            // If there is output from the server, then process it.
            if (output != null)
                handleOutput(output);

            // Return the next input.
            return nextInput(server);
        }
        finally
        {
            mutex.release();
        }
    }

    private TaskData nextInput(NodeAddress server)
    {
        // TODO: Retry unfinishedInputIds work
        // If the worker is asking for more input and it hasn't completed the last one we gave it
        // then give it the same input once again.  Perhaps it wasn't received last time.
        if (inputQueue.size() == 0)
        {
            if (log.isDebugEnabled())
                log.debug("Telling " + server + " that there is no more input.");
            return null;
        }
        else
        {
            if (!serverAddresses.contains(server))
            {
                // This simply means that some servers didn't respond before they started
                // asking for input.  That's okay, we'll let them have it.
                log.warn("Unassigned server " + server + " asking for input, adding to the list.");
                serverAddresses.add(server);
            }
            TaskInput is = (TaskInput) inputQueue.remove(0);
            is.setServer(server);
            // Remember the input status in the map in case we have a failure
            // during processing.  It can then be put back on the queue.
            inprogress.put(is.getInputId(), is);
            if (log.isDebugEnabled())
                log.debug("Input id " + is.getInputId() + " given to server " + server + ", " + inputQueue.size() + " remain.");
            return is.getInput();
        }
    }

    private void handleOutput(TaskData output)
    {
        Integer key = new Integer(output.getInputId());
        inprogress.remove(key);
        if (unfinishedInputIds.remove(key))
        {
            if (log.isDebugEnabled())
                log.debug("putOutput() : " + output.getInputId() + " finished, " + unfinishedInputIds.size() + " remain.");
            // Put the output on the queue for the aggregator thread.
            // We're on the reciever thread now, so we want to return as quickly as possible.
            try
            {
                outputQueue.put(output);
            }
            catch (InterruptedException e)
            {
                releaseTask(e);
            }
        }
        if (unfinishedInputIds.size() == 0)
        {
            if (log.isDebugEnabled())
                log.debug("All work finished, releasing....");
            releaseTask(null);
        }
    }

    private void releaseTask(Exception e)
    {
        try
        {
            Bus bus = client.getBus();
            bus.release(info);
        }
        catch (Exception e1)
        {
            log.warn("Unexpected exception while sending 'release' message: " + e1, e1);
        }
        if (e != null)
        {
            log.warn("Task failure: " + e);
            if (e instanceof GridException)
                failure = (GridException) e;
            else
                failure = new GridException(e);
        }
        client.onComplete(this);
        serverAddresses.clear();
        inprogress.clear();
        inputQueue.clear();
        try
        {
            outputQueue.put(END);
        }
        catch (InterruptedException e1)
        {
            throw new GridException(e1);
        }
        log.info("Task " + info.getTaskId() + " released.");
    }

    public void run(String taskClassName, Aggregator aggregator, int maxWorkers)
    {
        Bus bus = client.getBus();
        acquireMutex();
        try
        {
            if (running)
                throw new GridException("Already running.");
            info.setTaskClassName(taskClassName);

            // Get up to maxWorkers server addresses.  Don't get more than
            // what we need to process the queue of inputs.
            int serverCount = Math.min(maxWorkers, inputQueue.size());
            if (serverCount == 0)
                throw new GridException("Zero servers required.");
            int servers = sendAssign(serverCount, bus);
            if (servers == 0)
                throw new GridException("No available workers.");
            running = true; // We're running now.
            failure = null;
        }
        finally
        {
            mutex.release();
        }

        // Now, send the go message.
        bus.go(info);

        // Aggregate outputs from the queue using the caller's thread
        // until the 'END_OF_OUTPUT' is reached.
        aggregateOutput(aggregator);

        acquireMutex();
        try
        {
            running = false;
            // If there was an exception, throw it.
            if (failure != null)
                throw failure;
        }
        finally
        {
            mutex.release();
        }
    }

    private void aggregateOutput(Aggregator aggregator)
    {
        // Aggregate outputs from the queue.
        boolean loop = true;
        while (loop)
        {
            try
            {
                // Wait forever for something on the output queue.
                Object o = outputQueue.take();
                // If the output queue contains task data, aggregate it.
                if (o != null && o instanceof TaskData)
                {
                    TaskData output = (TaskData) o;
                    if (output.getInputId() == END_OF_OUTPUT) // This signals the end.
                        loop = false;
                    else if (aggregator != null)
                        aggregator.aggregate(output);
                }
                else
                    throw new IllegalStateException("Unexpected object on output queue: " + o);
            }
            catch (InterruptedException e)
            {
                loop = false;
                setFailure(e);
            }
        }
    }

    private void setFailure(Exception e)
    {
        acquireMutex();
        try
        {
            releaseTask(e);
        }
        finally
        {
            mutex.release();
        }
    }

    public void acquire()
    {
        // A little like a pre-assignment.  Send the assign message, but since we don't know the task class yet
        // nor do we have any inputs we're not going to send the 'go' message.
        Bus bus = client.getBus();
        acquireMutex();
        try
        {
            // Get one server, send the assign message.  The worker will wait for the 'go' message.
            sendAssign(1, bus);
        }
        finally
        {
            mutex.release();
        }
    }

    public void run(TaskRequest taskRequest)
    {
        for (Iterator iterator = taskRequest.getInput().iterator(); iterator.hasNext();)
        {
            Serializable input = (Serializable) iterator.next();
            addInput(input);
        }
        Aggregator aggregator = instantiateAggregator(taskRequest.getAggregatorClassName());
        run(taskRequest.getTaskClassName(), aggregator, taskRequest.getMaxWorkers());
    }

    public Aggregator instantiateAggregator(String className)
    {

        try
        {
            Class aClass = Thread.currentThread().getContextClassLoader().loadClass(className);
            return (Aggregator) aClass.newInstance();
        }
        catch (Exception e)
        {
            throw new GridException(e);
        }
    }

    public void release()
    {
        releaseTask(null);
    }

    private int sendAssign(int serverCount, Bus bus)
    {
        NodeAddress[] servers = client.getSeverAddresses(serverCount);
        if (servers == null || servers.length == 0)
            return 0;
        // Send the assign message while we have the mutex so
        // nobody can get input until we've processed the responses.
        AssignResponse[] responses = bus.assign(servers, info);
        // Now there are threads waiting on the servers for input!
        // Remember all the servers that responded.
        for (int i = 0; i < responses.length; i++)
        {
            AssignResponse response = responses[i];
            if (response != null)
            {
                if (log.isDebugEnabled())
                    log.debug("#" + i + " " + responses[i].getServer().toString());
                serverAddresses.add(response.getServer());
            }
        }
        return servers.length;
    }

    private void acquireMutex()
    {
        try
        {
            mutex.acquire();
        }
        catch (InterruptedException e)
        {
            throw new GridException(e);
        }
    }

    public void onFailure(GridException e)
    {
        acquireMutex();
        try
        {
            releaseTask(e);
        }
        finally
        {
            mutex.release();
        }
    }

    public void onMembershipChange(Set joined, Set left)
    {
        acquireMutex();
        try
        {
            // If any server left that we care about, then
            // this task has errored.
            for (Iterator iterator = left.iterator(); iterator.hasNext();)
            {
                NodeAddress nodeAddress = (NodeAddress) iterator.next();
                if (serverAddresses.contains(nodeAddress))
                    handleServerFailure(nodeAddress);
            }
        }
        finally
        {
            mutex.release();
        }
    }

    private void handleServerFailure(NodeAddress server)
    {
        // Find all inputs that were sent to the server
        for (Iterator inputs = inprogress.values().iterator(); inputs.hasNext();)
        {
            TaskInput is = (TaskInput) inputs.next();
            if (server.equals(is.getServer()))
            {
                is.incrementRetries();
                // If maximum retries has been exeeded, fail.
                if (is.getRetries() > maxRetries)
                {
                    GridException exception = new GridException("Server " + server
                            + " left the grid, max retries exceeded!");
                    onFailure(exception);
                    return;
                }
                // Otherwise, remove the input from the in progress map and
                // put it in the queue.
                inputs.remove();
                inputQueue.add(is);
            }
        }
    }
}
