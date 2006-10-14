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
    private int poolSize;
    private final WorkerMap workers;

    public ServerImpl(GridConfiguration config, Bus bus, GridImplementor grid)
    {
        poolSize = config.getThreadPoolSize();
        this.bus = bus;
        this.grid = grid;
        this.pool = new WorkerThreadPool(poolSize);
        shutdownLatch = new Latch();
        workers = new WorkerMap();
    }

    public int freeThreads()
    {
        synchronized (this)
        {
            return _freeThreads();
        }
    }

    private int _freeThreads()
    {
        return poolSize - workers.size();
    }

    public int totalThreads()
    {
        return poolSize;
    }

    public void onGo(TaskInfo task)
    {
        Worker worker = findWorker(task);
        if (worker != null)
            worker.go(task);
        else
            log.info("Not working on " + task);
    }

    public void onRelease(TaskInfo task)
    {
        Worker worker = findWorker(task);
        if (worker != null)
        {
            worker.setReleased(true);
            done(task);
        }
        else
            log.info("Not working on " + task);
    }

    private synchronized Worker findWorker(TaskInfo task)
    {
        return workers.findWorker(task);
    }

    public AssignResponse onAssign(TaskInfo task)
    {
        if (log.isDebugEnabled())
            log.debug("onAssign() : " + task);

        // Allocate a thread from the pool and run the Worker.  This will loop
        // until there is no more input available from the client.
        // The worker will remain waiting for the 'go' command from the client.
        Worker worker = new Worker(this, task, bus);
        try
        {
            synchronized (this)
            {
                int freeThreads = _freeThreads();
                if (freeThreads <= 0)
                    return null;
                if (workers.contains(task))
                    throw new GridException("Already working on " + task);
                workers.addWorker(task, worker);
                pool.execute(worker);
                bus.broadcastNodeStatus();
                return new AssignResponse(bus.getAddress(), _freeThreads());
            }
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
                    Worker worker = (Worker) iter.next();
                    worker.setReleased(true);
                }
            }
        }
    }

    synchronized void done(TaskInfo task)
    {
        workers.removeWorker(task);
        bus.broadcastNodeStatus();
    }

    class WorkerMap
    {
        private Map workerMapByClient = new HashMap();
        private int size;

        public void addWorker(TaskInfo info, Worker worker)
        {
            Map map = findMapByClient(info.getClient());
            if (map == null)
            {
                map = new HashMap();
                workerMapByClient.put(info.getClient(), map);
            }
            map.put(info, worker);
            size++;
        }

        public Worker removeWorker(TaskInfo info)
        {
            Map map = findMapByClient(info.getClient());
            if (map != null)
            {
                size--;
                Worker worker = (Worker) map.remove(info);
                if (map.size() == 0)
                    workerMapByClient.remove(info.getClient());
                return worker;
            }
            else
                return null;
        }

        public Worker findWorker(TaskInfo info)
        {
            Map map = findMapByClient(info.getClient());
            if (map != null)
                return (Worker) map.get(info);
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

        public boolean contains(TaskInfo task)
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
