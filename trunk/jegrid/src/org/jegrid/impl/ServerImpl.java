package org.jegrid.impl;

import org.jegrid.impl.Server;
import org.jegrid.GridConfiguration;
import org.jegrid.TaskRunnable;
import org.jegrid.impl.TaskData;

/**
 * The server processes jobs and tasks sent to it by clients and other servers.   This contains
 * a pool of worker threads that will get assigned to tasks by clients.
 * <br> User: jdavis
 * Date: Sep 30, 2006
 * Time: 7:49:59 AM
 */
public class ServerImpl  implements Server
{
    private WorkerThreadPool pool;
    private Bus bus;

    public ServerImpl(GridConfiguration config,Bus bus)
    {
        this.bus = bus;
        int poolSize = config.getThreadPoolSize();
        this.pool = new WorkerThreadPool(poolSize);
    }

    void onAssign(ServerTask task) throws InterruptedException
    {
        // Allocate a thread from the pool and run the Worker.  This will loop
        // until there is no more input available from the client.
        Worker runner = new Worker(this,task);
        pool.execute(runner);
    }

    public TaskData getNextInput(ServerTask task)
    {
        return null; // TODO: Implement this!
    }

    public TaskRunnable instantiateTaskRunnable(String taskClass)
    {

        return null; // TODO: Implement this!
    }

    public void putOutput(ServerTask task, TaskData output)
    {
        // TODO: Implement this.
    }
}
