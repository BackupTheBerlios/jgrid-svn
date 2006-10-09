package org.jegrid.impl;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import org.apache.log4j.Logger;
import org.jegrid.*;

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
    private Latch shutdownLatch;
    private GridImplementor grid;

    public ServerImpl(GridConfiguration config, Bus bus, GridImplementor grid)
    {
        this.bus = bus;
        this.grid = grid;
        int poolSize = config.getThreadPoolSize();
        this.pool = new WorkerThreadPool(poolSize);
        shutdownLatch = new Latch();
    }


    public int freeThreads()
    {
        return pool.getFreeThreads();
    }

    public int totalThreads()
    {
        return pool.getMaximumPoolSize();
    }

    public AssignResponse onAssign(TaskInfo task)
    {
        if (log.isDebugEnabled())
            log.debug("onAssign() : " + task);

        // Allocate a thread from the pool and run the Worker.  This will loop
        // until there is no more input available from the client.
        Worker runner = new Worker(this, task, bus);
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

    public void run()
    {
        bus.connect();
        
        try
        {
            log.info("Server running...");
            // Spin and wait for shutdown.
            long wait = 0;
            long lastwait = wait;
            while (!shutdownLatch.attempt(wait))
            {
                // Refresh the grid status on this node.
                GridStatus status = grid.getGridStatus(false);
                // Set the wait time to be a multiple of the number of
                // nodes in the grid to avoid chatter on large grids.
                wait = 5000 * status.getNumberOfNodes();
                if (wait != lastwait)
                {
                    log.debug("Refresh wait changed from " + lastwait + " to " + wait + " milliseconds.");
                    lastwait = wait;
                }
            }
        }
        catch (InterruptedException e)
        {
            log.warn("Interrupted.");
        }
        finally
        {
            bus.disconnect();
        }
    }

    public TaskRunnable instantiateTaskRunnable(String taskClass)
            throws IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        
        Class aClass = Thread.currentThread().getContextClassLoader().loadClass(taskClass);
        return (TaskRunnable) aClass.newInstance();
    }
}
