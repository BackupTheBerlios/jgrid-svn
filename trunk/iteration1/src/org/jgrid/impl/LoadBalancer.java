package org.jgrid.impl;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import org.apache.log4j.Logger;
import org.jgrid.GridException;
import org.jgroups.Address;
import org.jgroups.SuspectedException;
import org.jgroups.TimeoutException;
import org.jgroups.blocks.GroupRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Assigns job requests to available servers from a queue.
 * <br>User: Joshua Davis
 * Date: Mar 4, 2006
 * Time: 8:02:47 AM
 */
public class LoadBalancer implements Runnable
{
    private static Logger log = Logger.getLogger(LoadBalancer.class);

    private LinkedQueue acceptQueue;
    private GridBusImpl gridBus;
    private RequestStateMap activeRequests;

    private static final int POLL_INTERVAL = 10000;
    private static final int ACCEPT_TIMEOUT = 5000;

    public LoadBalancer(GridBusImpl gridBus)
    {
        this.gridBus = gridBus;
        this.acceptQueue = new LinkedQueue();
        this.activeRequests = new RequestStateMap();
    }

    public void run()
    {
        log.info("*** Started. ***");
        // Consume RequestStates from the queue, but only if there are servers ready to
        // accept them.
        try
        {
            //noinspection InfiniteLoopStatement
            for (; ;)
            {
                processRequests();
            }
        }
        catch (InterruptedException e)
        {
            log.error("Unexpected " + e,e);
        }
        finally
        {
            log.info("*** Exiting. ***");
        }
    }

    private void processRequests()
            throws InterruptedException
    {
        RequestState state;
        // Wait for a request.
        if (log.isDebugEnabled())
           log.debug("processRequests() : Waiting for requests...");
        state = (RequestState) acceptQueue.poll(POLL_INTERVAL);
        if (state != null)
        {
            String requestId = state.getRequestId();
            log.info("Request " + requestId);
            if (activeRequests.contains(requestId))
                throw new GridException("Request " + state.getRequestId() + " was already acceptAcknowledged.");

            // Get the list of servers.
            List servers = getServers();
            GridRpcDispatcher dispatcher = gridBus.getDispatcher();

            // Tricky stuff...
            // We must add the job to the map here, because the server might call completed()
            // *before* we figure out what to do with the 'accept' response.
            activeRequests.put(state);

            boolean acceptAcknowledged = false;
            try
            {
                for (Iterator iterator = servers.iterator(); !acceptAcknowledged && iterator.hasNext();)
                {
                    NodeStateImpl server = (NodeStateImpl) iterator.next();
                    acceptAcknowledged = sendAccept(server, dispatcher, state);
                }
            }
            finally
            {
                if (!acceptAcknowledged)
                    activeRequests.remove(requestId);
            }
        }
    }

    private boolean sendAccept(NodeStateImpl server, GridRpcDispatcher dispatcher,
                               RequestState state)
    {
        String requestId = state.getRequestId();
        boolean accepted = false;
        Address nodeAddress = server.getNodeAddress();
        // Send the job to the server.
        Object response;
        try
        {
            response = dispatcher.callRemoteMethod(nodeAddress, "_accept",
                    state.getRequest(),
                    GroupRequest.GET_ALL,
                    ACCEPT_TIMEOUT);
        }
        catch (TimeoutException e)
        {
            log.info("Server " + nodeAddress + " timed out.  Trying another node.");
            response = null;
        }
        catch (SuspectedException e)
        {
            log.info("Server " + nodeAddress + " is suspect.  Trying another node.");
            response = null;
        }
        log.info("Server " + nodeAddress + " responded with " + response);
        if (response != null && response instanceof JobAccepted)
        {
            JobAccepted jobAccepted = (JobAccepted) response;
            if (jobAccepted.getRequestId().equals(requestId))
            {
                state.setAccepted(jobAccepted, nodeAddress);
                log.info("accept() : request " + requestId + " accepted by " + nodeAddress);
                accepted = true;
            }
            else
            {
                throw new GridException("Response from server didn't contain the right request id!\n"
                        + "request = " + state + "\nresponse = " + jobAccepted);
            }
        }
        return accepted;
    }

    public void putRequest(RequestState state) throws InterruptedException
    {
        acceptQueue.put(state);
    }

    /**
     * Returns the list of all known servers on the grid as a List of NodeStateImpl.
     *
     * @return a list of NodeStateImpl, one for each server on the grid.
     */
    List getServers() throws InterruptedException
    {
        GridStateImpl gridState = gridBus.getGridState();
        if (gridState == null)
            throw new GridException("Cannot start: grid state is not available.");

        List serverList = new ArrayList();
        while (serverList.size() == 0)
        {
            log.info("Waiting for servers...");
            gridState.waitForServers();
            Collection nodeStates = gridState.getAllNodes();
            for (Iterator iterator = nodeStates.iterator(); iterator.hasNext();)
            {
                NodeStateImpl n = (NodeStateImpl) iterator.next();
                // Skip nodes with no available threads.
                if (n.getFreeThreads() == 0 || !n.isServer())
                    continue;
                serverList.add(n);
            }
            log.info("Found " + serverList.size() + " available servers.");
        }
        return serverList;
    }

    public Object completed(JobResponse response)
    {
        String requestId = response.getRequestId();
        if (log.isDebugEnabled())
           log.debug("completed() : " + requestId);
        RequestState state = activeRequests.remove(requestId);
        if (state == null)
        {
            log.warn("Not waiting for " + requestId);
            return MessageConstants.NACK;
        }
        return state.completed(response);
    }
}
