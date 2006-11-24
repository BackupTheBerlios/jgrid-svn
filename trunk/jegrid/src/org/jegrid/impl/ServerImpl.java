package org.jegrid.impl;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import org.apache.log4j.Logger;
import org.jegrid.*;

import java.util.*;

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
    private final WorkerMap workers;
    private int tasksAccepted;
    private long lastTaskAccepted;
    private GridConfiguration config;
    private int poolSize;
    private Thread runThread;

    public ServerImpl(GridConfiguration config, Bus bus, GridImplementor grid)
    {
        this.config = config;
        poolSize = config.getThreadPoolSize();
        if (poolSize == 0)
            throw new GridException("Cannot start a server with zero threads!");

        log.info("Starting server with " + poolSize + " threads.");
        this.bus = bus;
        this.grid = grid;
        this.pool = new WorkerThreadPool(poolSize, grid);
        shutdownLatch = new Latch();
        workers = new WorkerMap();
    }

    public synchronized int freeThreads()
    {
        return _freeThreads();
    }

    private int _freeThreads()
    {
        return poolSize - workers.size();
    }

    public int totalThreads()
    {
        return poolSize;
    }

    public void onGo(GoMessage goMessage)
    {
        if (goMessage == null)
            return;
        TaskId taskId = goMessage.getTaskId();
        InputProcessingWorker worker = findWorker(taskId);
        if (worker != null)
            worker.go(goMessage);
    }

    public void onRelease(TaskId id)
    {
        InputProcessingWorker worker = findWorker(id);
        if (worker != null)
        {
            worker.setReleased(true);
            done(id);
        }
    }

    public boolean onAssignTask(TaskRequest request)
    {
        if (log.isDebugEnabled())
            log.debug("onAssignTask()");
        synchronized (this)
        {
            int freeThreads = _freeThreads();
            if (freeThreads <= 0)
                return false;
            Task task = grid.getClient().createTask(request.getTaskKey());
            RemoteTaskWorker worker = new RemoteTaskWorker(this,
                    grid.getClient(), request, task.getTaskId());
            try
            {
                executeWorker(task.getTaskId(), worker);
            }
            catch (Exception e)
            {
                log.warn("Task not accepted due to :" + e);
                return false;
            }
            return true;
        }
    }

    public synchronized int tasksAccepted()
    {
        return tasksAccepted;
    }

    public synchronized long lastTaskAccepted()
    {
        return lastTaskAccepted;
    }

    public void doShutdown()
    {
        pool.shutdownNow();
        try
        {
            pool.awaitTerminationAfterShutdown();
        }
        catch (InterruptedException e)
        {
            log.warn("Interrupted.");
        }
        if (runThread != null)
            runThread.interrupt();
        shutdownLatch.release();
    }

    private synchronized InputProcessingWorker findWorker(TaskId id)
    {
        return workers.findWorker(id);
    }

    public AssignResponse onAssign(TaskId id)
    {
        if (log.isDebugEnabled())
            log.debug("onAssign() : " + id);

        // Allocate a thread from the pool and run the InputProcessingWorker.  This will loop
        // until there is no more input available from the client.
        // The worker will remain waiting for the 'go' command from the client.
        InputProcessingWorker worker = new InputProcessingWorker(this, id, bus, config.isDistributedLoggingEnabled());
        synchronized (this)
        {
            int freeThreads = _freeThreads();
            if (freeThreads <= 0)
                return new AssignResponse(bus.getAddress(), _freeThreads(), false);
            executeWorker(id, worker);
            return new AssignResponse(bus.getAddress(), _freeThreads(), true);
        }
    }

    private void executeWorker(TaskId id, Worker worker)
    {
        if (workers.contains(id))
            throw new GridException("Already working on " + id);
        workers.addWorker(id, worker);
        try
        {
            pool.execute(worker);
        }
        catch (ServerBusyException sbe)
        {
            log.info("This server is busy.");
            workers.removeWorker(id);
            throw sbe;
        }
        catch (InterruptedException e)
        {
            log.info("This server was interrupted.");
            workers.removeWorker(id);
            throw new GridException(e);
        }
        tasksAccepted++;
        lastTaskAccepted = System.currentTimeMillis();
        bus.broadcastNodeStatus();
    }

    public void run()
    {
        bus.connect();
        NodeAddress addr = bus.getAddress();
        try
        {
            runThread = Thread.currentThread();
            log.info("*** SERVER " + addr + " RUNNING ***");
            // Spin and wait for shutdownServers.
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
            log.info("*** SERVER " + addr + " STOPPED ***");
            bus.goodbye(addr);
            bus.disconnect();
        }
    }

    public void onMembershipChange(Set joined, Set left)
    {
        synchronized (this)
        {
            // If any server left that we care about, then
            // this task has errored.
            for (Iterator iterator = left.iterator(); iterator.hasNext();)
            {
                NodeAddress nodeAddress = (NodeAddress) iterator.next();
                Iterator iter = workers.iterateWorkersByClient(nodeAddress);
                while (iter.hasNext())
                {
                    InputProcessingWorker worker = (InputProcessingWorker) iter.next();
                    worker.setReleased(true);
                }
            }
        }
    }

    synchronized void done(TaskId id)
    {
        log.info("done() " + id);
        workers.removeWorker(id);
        bus.broadcastNodeStatus();
    }

    public GridImplementor getGrid()
    {
        return grid;
    }

    class WorkerMap
    {
        private Map workerMapByClient = new HashMap();
        private int size;

        public void addWorker(TaskId id, Worker worker)
        {
            Map map = findMapByClient(id.getClient());
            if (map == null)
            {
                map = new HashMap();
                workerMapByClient.put(id.getClient(), map);
            }
            map.put(id, worker);
            size++;
        }

        public Worker removeWorker(TaskId id)
        {
            Map map = findMapByClient(id.getClient());
            if (map != null)
            {
                size--;
                Worker worker = (Worker) map.remove(id);
                if (map.size() == 0)
                    workerMapByClient.remove(id.getClient());
                return worker;
            }
            else
                return null;
        }

        public InputProcessingWorker findWorker(TaskId id)
        {
            Map map = findMapByClient(id.getClient());
            if (map != null)
                return (InputProcessingWorker) map.get(id);
            else
                return null;
        }

        public Iterator iterateWorkersByClient(NodeAddress addr)
        {
            Map map = findMapByClient(addr);
            if (map != null)
                return map.values().iterator();
            else
                return Collections.EMPTY_LIST.iterator();
        }

        private Map findMapByClient(NodeAddress address)
        {
            return (Map) workerMapByClient.get(address);
        }

        public int size()
        {
            return size;
        }

        public boolean contains(TaskId task)
        {
            Map map = findMapByClient(task.getClient());
            return map != null && map.containsKey(task);
        }

        public boolean containsClient(NodeAddress nodeAddress)
        {
            return workerMapByClient.containsKey(nodeAddress);
        }
    }
}
