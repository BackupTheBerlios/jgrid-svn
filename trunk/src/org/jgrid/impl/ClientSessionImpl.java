// $Id:                                                                    $
package org.jgrid.impl;

import org.apache.log4j.Logger;
import org.jgrid.GridException;
import org.jgrid.Job;
import org.jgroups.Address;
import org.jgroups.blocks.GroupRequest;
import org.jgrid.ClientSession;
import org.jgrid.GridConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;
import java.io.IOException;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

/**
 * The client aspect of a grid node.
 *
 * @author josh Jan 19, 2005 7:24:39 AM
 */
public class ClientSessionImpl extends GridComponent implements ClientSession
{
    private static Logger log = Logger.getLogger(ClientSessionImpl.class);

    private static final int ACCEPT_TIMEOUT = 30000;

    /**
     * The queue of job requests that have not been accepted yet. *
     */
    private LoadBalancer loadBalancer;
    private Thread loadBalancerThread;

    public ClientSessionImpl(GridConfiguration config, GridBusImpl gridBus)
    {
        super(config, gridBus);
    }

    private LoadBalancer getLoadBalancer()
    {
        synchronized (this)
        {
            if (loadBalancer == null)
            {
                loadBalancer = new LoadBalancer(getGridBus());
                loadBalancerThread = new Thread(loadBalancer, "LoadBalancer");
                loadBalancerThread.setDaemon(true);
                loadBalancerThread.start();
                log.info("Load balancer started.");
            }
        }
        return loadBalancer;
    }

    public Job createJob(Class aClass)
    {
        return new JobImpl(this, aClass);
    }

    String nextRequestId()
    {
        return "jobreq" + getGridBus().getNextId();
    }

    public String getGridName()
    {
        return getGridBus().getConfig().getGridName();
    }

    public void putRequest(RequestState state) throws InterruptedException
    {
        // If the load balancer thread has not started yet, start it.
        getLoadBalancer().putRequest(state);
    }

    /**
     * Invoked by the receiver thread (message pump) when a job has completed.
     * @param response The message with the details about the completion.
     * @return The response (e.g. ACK).
     */
    public Object completed(JobResponse response)
    {
        synchronized(this)
        {
            if (loadBalancer == null)
            {
                log.error("No load balancer exists to recieve " + response);
                return MessageConstants.NACK;
            }
            else
            {
                return loadBalancer.completed(response);
            }
        }
    }
}
