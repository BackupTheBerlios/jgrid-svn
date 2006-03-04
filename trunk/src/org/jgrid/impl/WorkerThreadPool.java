package org.jgrid.impl;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import org.apache.log4j.Logger;

/**
 * The pool of worker threads that execute the jobs accepted by a server.  This is just like
 * PooledExecutor except:
 * <ol>
 * <li>It will not queue requests when the pool is busy.</li>
 * <li>It notifies the 'server' component whenever a worker becomes available.</li>
 * </ol>
 * <br>User: Joshua Davis
 * <br>Date: Oct 24, 2005 Time: 9:10:59 AM
 */
public class WorkerThreadPool extends PooledExecutor implements PooledExecutor.BlockedExecutionHandler
{
    private static Logger log = Logger.getLogger(WorkerThreadPool.class);

    private ServerImpl server;

    public WorkerThreadPool(ServerImpl server, int serverThreadPoolSize)
    {
        super(serverThreadPoolSize);
        this.server = server;
        // Set up the thread pool so it will throw ServerBusyException when
        // it cannot accept the job.
        setBlockedExecutionHandler(this);
    }

    public boolean blockedAction(Runnable command)
    {
        throw new ServerBusyException("Server thread pool is busy!");
    }

    public void execute(Runnable command) throws InterruptedException
    {
        super.execute(command);
        log.info("Executing " + command);
    }

    protected synchronized void workerDone(Worker w)
    {
        super.workerDone(w);
        server.updateNodeStatistics();
    }
}
