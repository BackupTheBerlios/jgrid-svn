package org.jgrid.impl;

import org.apache.log4j.Logger;
import org.jgroups.Address;
import org.jgroups.TimeoutException;
import org.jgroups.util.Promise;
import org.jgrid.GridException;
import org.jgrid.Job;

import java.io.Serializable;

/**
 * TODO: Add class level javadoc
 * <br>User: Joshua Davis
 * <br>Date: Oct 22, 2005 Time: 4:24:17 PM
 */
class JobImpl implements Job
{
    private static Logger log = Logger.getLogger(JobImpl.class);

    private Class serviceClass;
    private ClientSessionImpl clientSession;
    private String requestId;
    private JobAccepted accepted;
    private Address acceptor;
    private Promise resultPromise;

    public JobImpl(ClientSessionImpl clientSession, Class serviceClass)
    {
        this.clientSession = clientSession;
        this.serviceClass = serviceClass;
        resultPromise = new Promise();
    }

    public void execute(Serializable input)
    {
        if (resultPromise.hasResult())
            throw new GridException("The job request executed, but the result has not been taken yet.");
        if (isAccepted())
            throw new GridException("The job request is running already.");

        JobRequest request = clientSession.createJobRequest(serviceClass, input);
        requestId = request.getRequestId();

        NodeStateImpl[] servers = clientSession.getServers();

        // TODO: If there are no servers, wait for some.

        // TODO: Sort the list of servers so we try them in priority order.

        // Send the accept message to each server, stopping with the first one that returns
        // a 'JobAccepted' object.
        int attempts = 0;
        for (int i = 0; i < servers.length; i++)
        {
            NodeStateImpl server = servers[i];
            attempts++;
            if (clientSession.accept(server.getNodeAddress(), this, request))
            {
                log.info("Job accepted by " + acceptor);
                break;
            }
        }
        if (!isAccepted())
            throw new GridException("Job was not accepted (" + attempts + " attempts).");
    }

    private boolean isAccepted()
    {
        synchronized (this)
        {
            return accepted != null;
        }
    }

    public Serializable takeResult(long timeout)
    {
        if (!isAccepted())
            throw new IllegalStateException("Job has not been accepted.  Invoke execute() first.");

        Serializable result = null;
        try
        {
            result = (Serializable) resultPromise.getResultWithTimeout(timeout);
        }
        catch (TimeoutException e)
        {
            // Don't update the 'accepted' stuff.
            throw new GridException("Timed out wating for result for request " + requestId, e);
        }
        reset();
        return result;
    }

    private void reset()
    {
        synchronized (this)
        {
            accepted = null;    // Job is no longer 'accepted'.
            acceptor = null;
            resultPromise.reset();
        }
    }

    Object setResponse(JobResponse response)
    {
        // NOTE: This might get called *before* setAccepted() so don't check
        // for the accepted status, just set the result object.
        resultPromise.setResult(response.getOutput());
        return MessageConstants.ACK;
    }

    void setAccepted(JobAccepted jobAccepted, Address acceptor)
    {
        synchronized (this)
        {
            accepted = jobAccepted;
            this.acceptor = acceptor;
        }
    }
}
