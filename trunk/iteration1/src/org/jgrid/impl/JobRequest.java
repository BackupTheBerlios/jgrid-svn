package org.jgrid.impl;

import org.jgrid.util.SerializationUtil;

import java.io.Serializable;
import java.io.IOException;

/**
 * Represents a request to start a job.
 * <br>User: Joshua Davis
 * <br>Date: Oct 22, 2005 Time: 3:00:32 PM
 */
class JobRequest extends JobMessage
{
    private byte[] inputBytes;

    public JobRequest(String jobId, String serviceClassName, Serializable input) throws IOException {
        super(jobId,serviceClassName);
        // Turn the object into a byte array so we don't have to de-serialize it until we are actually
        // executing the job and the class loader is set properly.
        this.inputBytes = SerializationUtil.objectToByteArray(input);
    }

    public Serializable getInput() throws IOException, ClassNotFoundException {
        return (Serializable) SerializationUtil.byteArrayToObject(inputBytes);
    }

    public void clear()
    {
        inputBytes = null;        
    }
}
