package org.jgrid;

import java.io.Serializable;

/**
 * Client side interface to a job running on the grid.
 * <br>User: Joshua Davis
 * <br>Date: Oct 22, 2005 Time: 12:59:16 PM
 */
public interface Job
{
    /**
     * Begin asynchronously executing the service class with the input on the grid.
     * @param input the input to the service
     */
    void execute(Serializable input);


    /**
     * Wait forever for the result and take ownership of the result when it
     * arrives.
     * @return the result of the execution.
     * @param timeout
     */
    Serializable takeResult(long timeout);
}
