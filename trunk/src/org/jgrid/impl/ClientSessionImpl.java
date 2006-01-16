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

/**
 * The client aspect of a grid node.
 *
 * @author josh Jan 19, 2005 7:24:39 AM
 */
public class ClientSessionImpl extends GridComponent implements ClientSession
{
    private static Logger log = Logger.getLogger(ClientSessionImpl.class);

    private Map jobsByRequestId = new HashMap();
    private static final int ACCEPT_TIMEOUT = 30000;

    public ClientSessionImpl(GridConfiguration config, GridBusImpl gridBus)
    {
        super(config, gridBus);
    }

    public Job createJob(Class aClass)
    {
        return new JobImpl(this, aClass);
    }

    List getServers()
    {
        connect();

        // Find a node to start the job on.
        GridStateImpl gridState = getGridBus().getGridState();
        if (gridState == null)
            throw new GridException("Cannot start: grid state is not available.");

        Collection nodeStates = gridState.getAllNodes();
        List serverList = new ArrayList(nodeStates.size());
        for (Iterator iterator = nodeStates.iterator(); iterator.hasNext();)
        {
            NodeStateImpl n = (NodeStateImpl) iterator.next();
            // Skip nodes with no available threads.
            if (n.getFreeThreads() == 0 || !n.isServer())
                continue;
            serverList.add(n);
        }
        return serverList;
    }

    private void connect()
    {
        if (!getGridBus().isRunning())
            getGridBus().connect();
    }

    String nextRequestId()
    {
        connect();
        return "jobreq" + getGridBus().getNextId();
    }

    public Object completed(JobResponse response) {
        String requestId = response.getRequestId();
        JobImpl job = removeJob(requestId);
        if (job == null)
            throw new GridException("Unepected response: " + response + "\nJob not found");
        try {
            return job.setResponse(response);
        } catch (Exception e) {
            throw new GridException(e);
        }
    }

    JobRequest createJobRequest(Class serviceClass, Serializable input) throws IOException {
        return new JobRequest(
                nextRequestId(),
                serviceClass.getName(),
                input
        );
    }

    public boolean accept(Address nodeAddress, JobImpl job, JobRequest request)
    {
        String requestId = request.getRequestId();
        if (lookupJob(requestId) != null)
            throw new GridException("Request " + request.getRequestId() + " was already accepted.");
        GridRpcDispatcher dispatcher = getGridBus().getDispatcher();
        try
        {
            if (log.isDebugEnabled())
                log.debug("accept() : Invoking _accept on " + nodeAddress + "...");
            // Tricky stuff...
            // We must add the job to the map here, because the server might call completed()
            // *before* we figure out what to do with the 'accept' response.
            addJob(requestId, job);
            Object response = dispatcher.callRemoteMethod(nodeAddress, "_accept", request,
                    GroupRequest.GET_ALL, ACCEPT_TIMEOUT);
            log.info(nodeAddress + " " + response);
            if (response != null && response instanceof JobAccepted)
            {
                JobAccepted jobAccepted = (JobAccepted) response;
                if (jobAccepted.getRequestId().equals(requestId))
                {
                    job.setAccepted(jobAccepted, nodeAddress);
                    log.info("accept() : request " + requestId + " accepted by " + nodeAddress);
                    return true;
                }
                else
                {
                    throw new GridException("Response from server didn't contain the right request id!\n"
                        + "request = " + request + "\nresponse = " + jobAccepted);
                }
            }
            // The server didn't accept the job.  Remove it.
            removeJob(requestId);
        }
        catch (Exception e)
        {
            log.error(e, e);
            removeJob(request.getRequestId());
        }
        return false;
    }

    private void addJob(String requestId, JobImpl job)
    {
        synchronized (this)
        {
            jobsByRequestId.put(requestId, job);
        }
    }

    private Object lookupJob(String requestId)
    {
        synchronized (this)
        {
            return jobsByRequestId.get(requestId);
        }
    }

    private JobImpl removeJob(String requestId)
    {
        synchronized (this)
        {
            return (JobImpl) jobsByRequestId.remove(requestId);
        }
    }

    public String getGridName() {
        return getGridBus().getConfig().getGridName();
    }
}
