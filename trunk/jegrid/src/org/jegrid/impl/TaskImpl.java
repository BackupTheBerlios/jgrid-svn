package org.jegrid.impl;

import org.jegrid.*;
import org.jegrid.jms.TaskRequest;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.*;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.CondVar;

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
    private CondVar finished;
    private List queue;             // Queue of TaskData for workers.
    private Set unfinishedInputIds; // Input ids that are queued, or in progress.
    private Set serverAddresses;    // The server addresses from the assignment.
    private Map inprogress;         // InputStatus by inputId - input that has been taken by a server.
    private Aggregator aggregator;  // The thing that is aggregating the results.
    private GridException failure;
    private int maxWorkers;
    private int maxRetries = DEFAULT_MAX_RETRIES;

    public TaskImpl(ClientImpl client, int taskId, String taskClass)
    {
        mutex = new Mutex();
        finished = new CondVar(mutex);
        this.client = client;
        this.info = new TaskInfo(client.getBus().getAddress(), taskId, taskClass);
        this.queue = new LinkedList();
        this.unfinishedInputIds = new HashSet();
        this.inprogress = new HashMap();
        this.serverAddresses = new HashSet();
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
            int inputId = queue.size();
            TaskData data = new TaskData(inputId, input);
            TaskInput is = new TaskInput(data);
            queue.add(is);
            unfinishedInputIds.add(is.getInputId());
        }
        finally
        {
            mutex.release();
        }
    }

    public TaskData getNextInput(NodeAddress server)
    {
        acquireMutex();
        try
        {
            // TODO: Retry unfinishedInputIds work
            // If the worker is asking for more input and it hasn't completed the last one we gave it
            // then give it the same input once again.  Perhaps it wasn't received last time.

            if (queue.size() == 0)
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
                TaskInput is = (TaskInput) queue.remove(0);
                is.setServer(server);
                // Remember the input status in the map in case we have a failure
                // during processing.  It can then be put back on the queue.
                inprogress.put(is.getInputId(), is);
                if (log.isDebugEnabled())
                    log.debug("Input id " + is.getInputId() + " given to server " + server + ", " + queue.size() + " remain.");
                return is.getInput();
            }
        }
        finally
        {
            mutex.release();
        }
    }

    public void putOutput(TaskData output)
    {
        acquireMutex();
        try
        {
            Integer key = new Integer(output.getInputId());
            inprogress.remove(key);
            if (unfinishedInputIds.remove(key))
            {
                if (log.isDebugEnabled())
                    log.debug("putOutput() : " + output.getInputId() + " finished, " + unfinishedInputIds.size() + " remain.");
                if (aggregator != null)
                    aggregator.aggregate(output);
            }
            if (unfinishedInputIds.size() == 0)
                releaseTask(null);
        }
        finally
        {
            mutex.release();
        }
    }

    private void releaseTask(GridException e)
    {
        failure = e;
        // Signal finished.
        finished.broadcast();
        client.onComplete(this);
        Bus bus = client.getBus();
        bus.release(info);
        serverAddresses.clear();
        inprogress.clear();
        queue.clear();
    }

    public void run(Aggregator aggregator, int maxWorkers)
    {
        this.aggregator = aggregator;
        this.maxWorkers = maxWorkers;
        failure = null;

        assign();

        // Wait for the last result to be posted or a failure.
        acquireMutex();
        try
        {
            while (unfinishedInputIds.size() > 0 && failure == null)
                finished.await();
            if (failure != null)
                throw failure;
        }
        catch (InterruptedException e)
        {
            throw new GridException(e);
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
        info.setTaskClassName(taskRequest.getTaskClassName());
        for (Iterator iterator = taskRequest.getInput().iterator(); iterator.hasNext();)
        {
            Serializable input = (Serializable) iterator.next();
            addInput(input);
        }
        Aggregator aggregator = instantiateAggregator(taskRequest.getAggregatorClassName());
        run(aggregator, taskRequest.getMaxWorkers());
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

    void assign()
    {
        Bus bus = client.getBus();
        acquireMutex();
        try
        {
            // Get up to maxWorkers server addresses.  Don't get more than
            // what we need to process the queue of inputs.
            int serverCount = Math.min(maxWorkers, queue.size());
            if (serverCount == 0)
            {
                if (log.isDebugEnabled())
                    log.debug("No assignment needed.");
                return;
            }
            int servers = sendAssign(serverCount, bus);
            if (servers == 0)
                throw new GridException("No available workers.");
        }
        finally
        {
            mutex.release();
        }

        // Now, send the go message.
        bus.go(info);
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
                queue.add(is);
            }
        }
    }
}
