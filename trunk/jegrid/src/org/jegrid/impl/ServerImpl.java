package org.jegrid.impl;

import org.jegrid.impl.Server;
import org.jegrid.GridConfiguration;
import org.jegrid.TaskRunnable;
import org.jegrid.NodeAddress;
import org.jegrid.GridException;
import org.jegrid.impl.TaskData;
import org.apache.log4j.Logger;

/**
 * The server processes jobs and tasks sent to it by clients and other servers.   This contains
 * a pool of worker threads that will get assigned to tasks by clients.
 * <br> User: jdavis
 * Date: Sep 30, 2006
 * Time: 7:49:59 AM
 */
public class ServerImpl implements Server
{
    private static Logger log = Logger.getLogger(ServerImpl.class);
    private WorkerThreadPool pool;
    private Bus bus;

    public ServerImpl(GridConfiguration config, Bus bus)
    {
        this.bus = bus;
        int poolSize = config.getThreadPoolSize();
        this.pool = new WorkerThreadPool(poolSize);
    }

    public AssignResponse onAssign(TaskInfo task)
    {
        if (log.isDebugEnabled())
            log.debug("onAssign() : " + task);

        // Allocate a thread from the pool and run the Worker.  This will loop
        // until there is no more input available from the client.
        Worker runner = new Worker(this, task);
        try
        {
            pool.execute(runner);
            return new AssignResponse(bus.getAddress(), pool.getFreeThreads());
        }
        catch (InterruptedException e)
        {
            throw new GridException(e);
        }
    }

    public TaskData getNextInput(NodeAddress client, int taskId)
    {
        return null; // TODO: Implement this!
    }

    public TaskRunnable instantiateTaskRunnable(String taskClass)
    {

        return null; // TODO: Implement this!
    }

    public void putOutput(NodeAddress client, int taskId, int id, TaskData output)
    {
    }
}
