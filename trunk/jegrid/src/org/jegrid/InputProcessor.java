package org.jegrid;

import java.io.Serializable;

/**
 * Processes an input for a task, producing output.
 * <br> User: jdavis
 * Date: Oct 7, 2006
 * Time: 10:17:45 AM
 */
public interface InputProcessor
{
    /**
     * Process the input, produce the output.
     *
     * @param inputId The id of the input in the client's queue.
     * @param input   The input.
     * @return The result.
     */
    Serializable processInput(int inputId, Serializable input);
}
