package org.jgrid.impl;

import org.jgroups.Address;

import java.io.Serializable;

/**
 * Represents a request to execute a job.
 * <br>User: Joshua Davis
 * <br>Date: Oct 22, 2005 Time: 3:00:32 PM
 */
class JobRequest extends JobMessage
{
    private Serializable input;

    public JobRequest(String jobId, String serviceClassName, Serializable input)
    {
        super(jobId,serviceClassName);
        this.input = input;
    }

    public Serializable getInput()
    {
        return input;
    }
}
