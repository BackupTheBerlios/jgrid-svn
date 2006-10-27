package org.jegrid;

/**
 * Contextual information about the currently running task.
 * <br>User: Joshua Davis
 * Date: Oct 27, 2006
 * Time: 6:52:43 AM
 */
public interface TaskContext
{
    /**
     * The id of the current task.
     *
     * @return The id of the current task.
     */
    TaskId getTaskId();

    /**
     * The shared input for the current task (may be null).  This is created
     * by the client and sent to all workers.
     * NOTE: Changes in this object will not be reflected on other nodes,
     * so it is recommended to treat this as immutable.
     *
     * @return the shared input
     */
    Object getSharedInput();

    /**
     * If a worker wants to parallelize further, it can use this client interface.
     *
     * @return the client interface.
     */
    Client getClient();
}
