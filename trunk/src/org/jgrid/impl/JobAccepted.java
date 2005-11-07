package org.jgrid.impl;

/**
 * Response from a server when a job has been accepted.
 * <br>User: Joshua Davis
 * <br>Date: Oct 22, 2005 Time: 4:57:06 PM
 */
class JobAccepted extends JobMessage
{
    public JobAccepted(String jobId, String serviceClassName)
    {
        super(jobId, serviceClassName);
    }
}
