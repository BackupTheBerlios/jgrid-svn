package org.jegrid;

import java.io.Serializable;

/**
 * Client code implements this to execute the desired algorithm on the grid.
 * <br> User: jdavis
 * Date: Oct 7, 2006
 * Time: 10:17:45 AM
 */
public interface TaskRunnable
{
    /**
     * Process the input, produce the output.
     * @param inputId The id of the input in the client's queue.
     * @param input The input.
     * @return The result.
     */
    Serializable run(int inputId,Serializable input);
}
