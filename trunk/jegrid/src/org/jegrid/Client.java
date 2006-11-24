package org.jegrid;

import java.io.Serializable;

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
     * @param taskKey the task key - unused by JEGrid, but passed along to the caller
     *                for easy identification of a particular task.
     * @return the new task
     */
    Task createTask(Serializable taskKey);

    /**
     * Runs the entire task remotely in the background.  The input queue, workers, output queue will
     * all happen on a server node.  This call will return as soon as a server node is found.
     *
     * @param request The task request, including all the input, etc.
     */
    void background(TaskRequest request);

    /**
     * Waits for at most specified number of servers to become available.
     *
     * @param max The maximum number of servers needed.
     * @return An array of server addresses.
     * @throws InterruptedException if the thread is interrupted.
     */
    NodeAddress[] waitForServers(int max) throws InterruptedException
            ;
}
