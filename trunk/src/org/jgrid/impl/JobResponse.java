package org.jgrid.impl;

import org.jgrid.util.SerializationUtil;

import java.io.Serializable;
import java.io.IOException;

/**
 * Response from a server when a job has been accepted.
 * <br>User: Joshua Davis
 * <br>Date: Oct 22, 2005 Time: 4:57:06 PM
 */
class JobResponse extends JobMessage
{
    private byte[] outputBytes;
    private Throwable throwable;
    private long startTime;
    private long endTime;

    public JobResponse(String jobId, String serviceClassName)
    {
        super(jobId, serviceClassName);
    }

    public void setOutput(Serializable output) throws IOException {
        this.outputBytes = SerializationUtil.objectToByteArray(output);
    }

    public Serializable getOutput() throws IOException, ClassNotFoundException {
        return (Serializable) SerializationUtil.byteArrayToObject(outputBytes);
    }

    public Throwable getThrowable()
    {
        return throwable;
    }

    public void setThrowable(Throwable throwable)
    {
        this.throwable = throwable;
    }

    public long getStartTime()
    {
        return startTime;
    }

    public void setStartTime(long startTime)
    {
        this.startTime = startTime;
    }

    public long getEndTime()
    {
        return endTime;
    }

    public void setEndTime(long endTime)
    {
        this.endTime = endTime;
    }

    public String toString()
    {
        return "JobResponse{" +
                "requestId='" + getRequestId() + '\'' +
                ", serviceClassName='" + getServiceClassName() + '\'' +
                ", outputBytes.length=" + outputBytes.length +
                ", throwable=" + throwable +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
