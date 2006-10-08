package org.jegrid.impl;

import org.jegrid.*;

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
    private ClientImpl client;
    private TaskInfo info;
    private Mutex mutex;
    private CondVar finished;
    private List queue;
    private Set unfinished;
    private Set serverAddresses;
    private Aggregator aggregator;
    private GridException failure;

    public TaskImpl(ClientImpl client, int taskId, String taskClass)
    {
        mutex = new Mutex();
        finished = new CondVar(mutex);
        this.client = client;
        this.info = new TaskInfo(client.getBus().getAddress(), taskId, taskClass);
        this.queue = new LinkedList();
        this.unfinished = new HashSet();
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
            TaskData data = new TaskData(inputId,input);
            queue.add(data);
            unfinished.add(new Integer(inputId));
        }
        finally
        {
            mutex.release();
        }
    }

    public TaskData getNextInput()
    {
        acquireMutex();
        try
        {
            if (queue.size() == 0)
                return null;
            else
                return (TaskData) queue.remove(0);
        }
        finally{
            mutex.release();
        }
    }
    
    public void onComplete(TaskData output)
    {
        acquireMutex();
        try
        {
            Integer key = new Integer(output.getInputId());
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
        int serverCount = Math.min(maxWorkers,getNumberOfInputs());
        NodeAddress[] servers = client.getSeverAddresses(serverCount);
        if (servers == null || servers.length == 0)
            throw new GridException("No workers available.");
        // Send the assign message to all servers.
        Bus bus = client.getBus();
        this.aggregator = aggregator;
        failure = null;
        AssignResponse[] responses = bus.assign(servers,info);
        acquireMutex();
        try
        {
            // Remember all the servers that responded.
            for (int i = 0; i < responses.length; i++)
            {
                AssignResponse response = responses[i];
                if (response != null)
                    serverAddresses.add(response.getServer());
            }
            // Wait for the last result to be posted.
            finished.await();
            if (failure != null)
                throw failure;
        }
        catch (InterruptedException e)
        {
            throw new GridException(e);
        }
        finally{
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
                {
                    failure = new GridException("Server " + nodeAddress + " left the grid!");
                    finished.broadcast();
                }
            }
        }
        finally
        {
            mutex.release();
        }
    }
}
