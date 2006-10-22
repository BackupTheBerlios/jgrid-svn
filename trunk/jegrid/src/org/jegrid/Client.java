package org.jegrid;

/**
 * Grid client interface.
 * <br> User: jdavis
 * Date: Sep 30, 2006
 * Time: 7:49:17 AM
 */
public interface Client
{
    /**
     * Creates a new task that can be executed on the grid servers.
     * The client portions of this task will run locally, and synchronously.
     *
     * @return the new task
     */
    Task createTask();

    /**
     * Runs the entire task remotely in the background.  The input queue, workers, output queue will
     * all happen on a server node.  This call will return as soon as a server node is found.
     *
     * @param request The task request, including all the input, etc.
     */
    void background(TaskRequest request);
}
