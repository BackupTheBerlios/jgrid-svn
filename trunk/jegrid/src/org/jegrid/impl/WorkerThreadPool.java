package org.jegrid.impl;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.ThreadFactory;
import org.apache.log4j.Logger;
import org.jegrid.Grid;

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
class WorkerThreadPool extends PooledExecutor
{
    private static Logger log = Logger.getLogger(WorkerThreadPool.class);

    private int nextThread = 1;
    private Grid grid;

    public WorkerThreadPool(int poolSize, Grid grid)
    {
        super(poolSize);
        this.grid = grid;
        // Set up the thread pool so it will throw ServerBusyException when
        // it cannot accept the job.
        setBlockedExecutionHandler(new BlockedExecHandler());
        // Also, use this class as a thread factory.
        setThreadFactory(new Factory());
    }

    public void execute(Runnable command) throws InterruptedException
    {
        log.info("Executing " + command);
        super.execute(command);
    }

    protected synchronized void workerDone(Worker w)
    {
        super.workerDone(w);
    }

    /**
     * Handles blocked execution by throwing an exception.
     */
    private class BlockedExecHandler implements BlockedExecutionHandler
    {
        public boolean blockedAction(Runnable command) throws InterruptedException
        {
            throw new ServerBusyException("Server " + grid.getLocalAddress() + " : Thread pool is busy!");
        }
    }

    private class Factory implements ThreadFactory
    {
        public Thread newThread(Runnable command)
        {
            Thread thread = new Thread(command, "Worker-" + nextThread++);
            // Set the priority to *almost* the lowest possible value
            // The workers need to yield to the networking threads. 
            thread.setPriority(Thread.MIN_PRIORITY + 1);
            log.info("New thread created: " + thread.getName());
            return thread;
        }
    }
}
