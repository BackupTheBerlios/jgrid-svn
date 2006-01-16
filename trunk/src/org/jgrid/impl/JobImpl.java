package org.jgrid.impl;

import org.apache.log4j.Logger;
import org.jgroups.Address;
import org.jgroups.TimeoutException;
import org.jgroups.util.Promise;
import org.jgrid.GridException;
import org.jgrid.Job;

import java.io.Serializable;
import java.io.IOException;
import java.util.List;
import java.util.Iterator;

/**
 * Client side view of a job.
 * <br>User: Joshua Davis
 * <br>Date: Oct 22, 2005 Time: 4:24:17 PM
 */
class JobImpl implements Job {
    private static Logger log = Logger.getLogger(JobImpl.class);

    private Class serviceClass;
    private ClientSessionImpl clientSession;
    private String requestId;
    private JobAccepted accepted;
    private Address acceptor;
    private Promise resultPromise;

    public JobImpl(ClientSessionImpl clientSession, Class serviceClass) {
        this.clientSession = clientSession;
        this.serviceClass = serviceClass;
        resultPromise = new Promise();
    }

    public void start(Serializable input) {
        if (resultPromise.hasResult())
            throw new GridException("The job request executed, but the result has not been taken yet.");
        if (isAccepted())
            throw new GridException("The job request is running already.");

        JobRequest request = null;
        try {
            request = clientSession.createJobRequest(serviceClass, input);
        } catch (IOException e) {
            throw new GridException("Unable to create job request due to : " + e,e);
        }
        requestId = request.getRequestId();

        List servers = clientSession.getServers();

        // If there are no servers, wait for some.
        if (servers.size() == 0)
                throw new GridException("No servers in grid " + clientSession.getGridName());

        // TODO: Sort the list of servers so we try them in priority order.

        // Send the accept message to each server, stopping with the first one that returns
        // a 'JobAccepted' object.
        int attempts = 0;
        for (Iterator iterator = servers.iterator(); iterator.hasNext();) {
            NodeStateImpl server = (NodeStateImpl) iterator.next();
            attempts++;
            if (clientSession.accept(server.getNodeAddress(), this, request)) {
                log.info("Job accepted by " + acceptor);
                break;
            }
        }
        if (!isAccepted())
            throw new GridException("Job was not accepted (" + attempts + " attempts).");
    }

    private boolean isAccepted() {
        synchronized (this) {
            return accepted != null;
        }
    }

    public Serializable join(long timeout) {
        if (!isAccepted())
            throw new IllegalStateException("Job has not been accepted.  Invoke start() first.");

        Serializable result = null;
        try {
            result = (Serializable) resultPromise.getResultWithTimeout(timeout);
        }
        catch (TimeoutException e) {
            // Don't update the 'accepted' stuff.
            throw new GridException("Timed out wating for result for request " + requestId, e);
        }
        reset();
        return result;
    }

    public void startParallel(List inputList) {

    }

    private void reset() {
        synchronized (this) {
            accepted = null;    // Job is no longer 'accepted'.
            acceptor = null;
            resultPromise.reset();
        }
    }

    Object setResponse(JobResponse response) throws IOException, ClassNotFoundException {
        // NOTE: This might get called *before* setAccepted() so don't check
        // for the accepted status, just set the result object.
        resultPromise.setResult(response.getOutput());
        return MessageConstants.ACK;
    }

    void setAccepted(JobAccepted jobAccepted, Address acceptor) {
        synchronized (this) {
            accepted = jobAccepted;
            this.acceptor = acceptor;
        }
    }
}
