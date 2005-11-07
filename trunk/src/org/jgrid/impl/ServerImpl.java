// $Id:                                                                    $
package org.jgrid.impl;

import org.apache.log4j.Logger;
import org.jgroups.Address;
import org.jgroups.SuspectedException;
import org.jgroups.TimeoutException;
import org.jgroups.util.Util;
import org.jgroups.blocks.GroupRequest;
import org.jgrid.GridConfiguration;
import org.jgrid.Server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A server that executes services using a thread pool, sending results back to
 * the clients.
 * @author josh Jan 4, 2005 9:41:06 PM
 */
public class ServerImpl extends GridComponent implements Server
{
    private static final Logger log = Logger.getLogger(ServerImpl.class);
    private ServerThreadPool threadPool;
    private Map classLoadersByServiceClass;
    private static final long SEND_COMPLETED_TIMEOUT = 10000;
    private boolean running = false;

    public ServerImpl(GridConfiguration config, GridBusImpl gridBus)
    {
        super(config,gridBus);
        classLoadersByServiceClass = new HashMap();
        threadPool = new ServerThreadPool(this,config.getServerThreadPoolSize());
    }

    public void run()
    {
        getGridBus().connect();
        // The MessagePump thread will handle all the incoming messages, so this
        // thread can be a heartbeat/status broadcaster.
        synchronized (this)
        {
            running = true;
        }
        while (getGridBus().isRunning())
        {
            updateNodeStatistics();
            Util.sleep(getConfig().getStatisticsUpdateInterval());
        }
        synchronized (this)
        {
            running = false;
        }
    }

    public int getFreeThreads()
    {
        return threadPool.getMaximumPoolSize() - threadPool.getPoolSize();
    }

    void updateNodeStatistics()
    {
        // Update the node state with available memory statistics.
        Runtime rt = Runtime.getRuntime();
        NodeStateImpl state = getGridBus().getMyState();
        state.setServer(isRunning());
        state.updateRuntimeStats(rt);
        state.setFreeThreads(getFreeThreads());
        if (log.isDebugEnabled())
            log.debug("updateNodeStatistics() : Broadcasting updated node statistics...");
        getGridBus().broadcastNodeStateChange();
    }

    private boolean isRunning()
    {
        synchronized(this)
        {
            return running;
        }
    }

    public Object handleAccept(JobRequest req)
    {
        Object rv = null;
        try
        {
            ServiceRunner runner = new ServiceRunner(this, req);
            runner.setRequestMessage(GridRpcTarget.getLocalMessage());
            threadPool.execute(runner);
            rv = new JobAccepted(req.getRequestId(),req.getServiceClassName());
        }
        catch (Exception e)
        {
            rv = e;
        }
        updateNodeStatistics(); // Update the grid: The status of this node may have changed.
        return rv;
    }

    ClassLoader getClassLoader(String serviceClassName)
    {
        ClassLoader classLoader;
        synchronized (this)
        {
            classLoader = (ClassLoader) classLoadersByServiceClass.get(serviceClassName);
            if (classLoader == null)
            {
                classLoader = new GridClassLoader(Thread.currentThread().getContextClassLoader(),
                        serviceClassName,
                        getGridBus());
                classLoadersByServiceClass.put(serviceClassName,classLoader);
            }
        }
        return classLoader;
    }

    void sendResponse(JobResponse response, Address source) throws IOException
    {
        try
        {
            Object ack = gridBus.getDispatcher().callRemoteMethod(
                    source,"_completed",response,GroupRequest.GET_ALL,SEND_COMPLETED_TIMEOUT);
            if (MessageConstants.ACK.equals(ack))
                log.info("Job request : " + response.getRequestId() + " completed.");
            else
                log.warn("Client did not acknowledge result!");
        }
        catch (TimeoutException e)
        {
            log.warn(e,e);
        }
        catch (SuspectedException e)
        {
            log.warn(e,e);
        }
        updateNodeStatistics();
    }
}
