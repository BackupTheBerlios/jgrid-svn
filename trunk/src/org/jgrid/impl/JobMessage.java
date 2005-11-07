package org.jgrid.impl;

import java.io.Serializable;

/**
 * General message about a job.
 * <br>User: Joshua Davis
 * <br>Date: Oct 22, 2005 Time: 4:44:51 PM
 */
class JobMessage implements Serializable
{
    private String requestId;
    private String serviceClassName;

    public JobMessage(String requestId, String serviceClassName)
    {
        this.requestId = requestId;
        this.serviceClassName = serviceClassName;
    }

    public String getRequestId()
    {
        return requestId;
    }

    public String getServiceClassName()
    {
        return serviceClassName;
    }


    public String toString()
    {
        return "JobMessage{" +
                "requestId='" + requestId + '\'' +
                ", serviceClassName='" + serviceClassName + '\'' +
                '}';
    }
}
