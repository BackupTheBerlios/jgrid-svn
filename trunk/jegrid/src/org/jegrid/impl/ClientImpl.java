package org.jegrid.impl;

import org.jegrid.*;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * The client - manages a list of tasks.
 * <br> User: jdavis
 * Date: Sep 30, 2006
 * Time: 7:49:52 AM
 */
public class ClientImpl implements ClientImplementor
{
    private int nextTaskId = 1000;
    private final Map tasksById = new HashMap();
    private Bus bus;
    private GridImplementor grid;
    private Comparator serverComparator;
    private static Logger log = Logger.getLogger(ClientImpl.class);

    public ClientImpl(Bus bus, GridImplementor grid)
    {
        this.bus = bus;
        this.grid = grid;
        this.serverComparator = new ServerComparator();
    }

    private int nextTaskId()
    {
        synchronized (this)
        {
            return nextTaskId++;
        }
    }

    public Bus getBus()
    {
        return bus;
    }

    NodeAddress[] getSeverAddresses(int max)
    {
        // Get the cached status.
        GridStatus status = grid.getGridStatus(true);
        Iterator iter = status.iterator();
        List list = new LinkedList();
        while (iter.hasNext())
        {
            NodeStatus node = (NodeStatus) iter.next();
            if (node.getType() == Grid.TYPE_SERVER && node.getFreeThreads() > 0)
                list.add(node);
        }
        if (list.size() == 0)
            return new NodeAddress[0];
        // Sort the list, put the most capable servers first.
        Collections.sort(list, serverComparator);
        // Get only the minimum number of servers.
        list = list.subList(0, Math.min(list.size(), max));
        NodeAddress[] rv = new NodeAddress[list.size()];
        int i = 0;
        for (Iterator iterator = list.iterator(); iterator.hasNext();)
        {
            NodeStatus nodeStatus = (NodeStatus) iterator.next();
            rv[i] = nodeStatus.getNodeAddress();
            i++;
        }
        return rv;
    }

    public Task createTask(Class taskClass)
    {
        String taskClassName = taskClass.getName();
        return createTask(taskClassName);
    }

    public Task createTask(String taskClassName)
    {
        TaskImpl task = new TaskImpl(this, nextTaskId(), taskClassName);
        addTask(task);
        return task;
    }

    private void addTask(TaskImpl task)
    {
        synchronized (tasksById)
        {
            tasksById.put(new Integer(task.getTaskId()), task);
        }
    }

    public Task createTask()
    {
        TaskImpl task = new TaskImpl(this, nextTaskId(), null);
        addTask(task);
        return task;
    }

    public TaskData getNextInput(int taskId, NodeAddress server)
    {
        TaskImpl task = findTask(taskId);
        if (task == null)
        {
            if (log.isDebugEnabled())
                log.debug("getNextInput() : No task " + taskId);
            return null;
        }
        return task.getNextInput(server);
    }

    private TaskImpl findTask(int taskId)
    {
        Integer key = new Integer(taskId);
        TaskImpl task;
        synchronized (tasksById)
        {
            task = (TaskImpl) tasksById.get(key);
        }
        return task;
    }

    public void putOutput(int taskId, TaskData output)
    {
        TaskImpl task = findTask(taskId);
        if (task == null)
        {
            if (log.isDebugEnabled())
                log.debug("putOutput() : No task " + taskId);
            return;
        }
        task.putOutput(output);
    }

    public void taskFailed(int taskId, GridException throwable)
    {
        TaskImpl task = findTask(taskId);
        if (task == null)
        {
            if (log.isDebugEnabled())
                log.debug("taskFailed() : No task " + taskId);
            return;
        }
        task.onFailure(throwable);
    }

    public void onMembershipChange(Set joined, Set left)
    {
        synchronized (tasksById)
        {
            for (Iterator iterator = tasksById.values().iterator(); iterator.hasNext();)
            {
                TaskImpl task = (TaskImpl) iterator.next();
                task.onMembershipChange(joined, left);
            }
        }
    }

    public void onComplete(TaskImpl task)
    {
        synchronized (tasksById)
        {
            tasksById.remove(new Integer(task.getTaskId()));
            if (log.isDebugEnabled())
                log.info("Task complete: " + task.getTaskId());
        }
    }
}
