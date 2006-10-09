package org.jegrid.impl;

import org.jegrid.*;
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
    private List queue;             // A queue of InputStatus - ready to be taken by servers
    private Set unfinished;         // The set of unfinished input, by input id.
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
        this.queue = new LinkedList();      // Queue of TaskData for workers. 
        this.unfinished = new HashSet();    // Input ids that are queued, or in progress.
        this.inprogress = new HashMap();    // TaskData input in progress, but inputId.
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
            unfinished.add(is.getInputId());
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
            if (queue.size() == 0)
                return null;
            else
            {
                if (!serverAddresses.contains(server))
                    throw new GridException("Unassigned server " + server + " asking for input.");
                TaskInput is = (TaskInput) queue.remove(0);
                is.setServer(server);
                // Remember the input status in the map in case we have a failure
                // during processing.  It can then be put back on the queue.
                inprogress.put(is.getInputId(), is);
                return is.getInput();
            }
        }
        finally
        {
            mutex.release();
        }
    }

    public void onComplete(TaskData output)
    {
        acquireMutex();
        try
        {
            Integer key = new Integer(output.getInputId());
            inprogress.remove(key);
            if (unfinished.remove(key))
            {
                if (aggregator != null)
                    aggregator.aggregate(output);
            }
            if (unfinished.size() == 0)
                finished.broadcast();
        }
        finally
        {
            mutex.release();
        }
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
            while (unfinished.size() > 0 && failure == null)
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

    void assign()
    {
        int serverCount = Math.min(maxWorkers, getNumberOfInputs());
        NodeAddress[] servers = client.getSeverAddresses(serverCount);
        if (servers == null || servers.length == 0)
            throw new GridException("No workers available.");
        // Send the assign message to all servers.
        Bus bus = client.getBus();
        acquireMutex();
        try
        {
            // Send the assign message while we have the mutex so
            // nobody can get input until we've processed the responses.
            AssignResponse[] responses = bus.assign(servers, info);
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
        }
        finally
        {
            mutex.release();
        }
    }

    private int getNumberOfInputs()
    {
        acquireMutex();
        try
        {
            return queue.size();
        }
        finally
        {
            mutex.release();
        }
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
            failure = e;
            // Signal finished.
            finished.broadcast();
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
                    fail(exception);
                    return;
                }
                // Otherwise, remove the input from the in progress map and
                // put it in the queue.
                inputs.remove();
                queue.add(is);
            }
        }
    }

    private void fail(GridException e)
    {
        failure = e;
        finished.broadcast();
    }

}
