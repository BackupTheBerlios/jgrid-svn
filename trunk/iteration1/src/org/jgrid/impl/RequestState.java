package org.jgrid.impl;

import org.jgroups.Address;

import java.io.Serializable;
import java.io.IOException;

/**
 * Represents the client side state of a job request issued to the grid.
 * <br>User: Joshua Davis
 * Date: Mar 4, 2006
 * Time: 7:35:54 AM
 */
public class RequestState
{
    private JobImpl job;
    private JobRequest request;
    private int index;
    private JobAccepted accepted;
    private Address acceptor;
    private JobResponse response;

    public RequestState(JobImpl job, JobRequest request, int index)
    {
        this.job = job;
        this.request = request;
        this.index = index;
    }

    public JobRequest getRequest()
    {
        return request;
    }

    public String getRequestId()
    {
        return request.getRequestId();
    }

    public void setAccepted(JobAccepted jobAccepted, Address nodeAddress)
    {
        accepted = jobAccepted;
        acceptor = nodeAddress;
        request.clear();  // Discard the input, it was sent to the server.
    }

    public Object completed(JobResponse response)
    {
        this.response = response;
        job.onCompletion(this);
        return MessageConstants.ACK;
    }

    public Serializable getOutput() throws IOException, ClassNotFoundException
    {
        return response.getOutput();
    }

    public void clearOutput()
    {
        response.clear();
    }

    public int getIndex()
    {
        return index;
    }
}
