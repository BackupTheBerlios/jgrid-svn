package org.jegrid.impl;

import org.jegrid.*;

import java.util.*;

/**
 * TODO: Add class level javadoc
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

    public ClientImpl(Bus bus,GridImplementor grid)
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
        // Get the real status.
        GridStatus status = grid.getGridStatus(false);
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
        Collections.sort(list,serverComparator);
        // Get only the minimum number of servers.
        list = list.subList(0,Math.min(list.size(), max));
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
        synchronized(tasksById)
        {
            tasksById.put(new Integer(task.getTaskId()), task);
        }
        return task;
    }

    public TaskData getNextInput(int taskId, NodeAddress server)
    {
        TaskImpl task = findTask(taskId);
        return task.getNextInput(server);
    }

    private TaskImpl findTask(int taskId)
    {
        Integer key = new Integer(taskId);
        TaskImpl task;
        synchronized(tasksById)
        {
            task = (TaskImpl) tasksById.get(key);
        }
        return task;
    }

    public void putOutput(int taskId, TaskData output)
    {
        TaskImpl task = findTask(taskId);
        task.onComplete(output);
    }

    public void taskFailed(int taskId, GridException throwable)
    {
        TaskImpl task = findTask(taskId);
        task.onFailure(throwable);
    }

    public void onMembershipChange(Set joined, Set left)
    {
        synchronized(tasksById)
        {
            for (Iterator iterator = tasksById.values().iterator(); iterator.hasNext();)
            {
                TaskImpl task = (TaskImpl) iterator.next();
                task.onMembershipChange(joined,left);
            }
        }
    }
}
