package org.jegrid.impl;

/**
 * Server implementation.
 * <br> User: jdavis
 * Date: Sep 30, 2006
 * Time: 7:49:11 AM
 */
public interface Server
{
    /**
     * Client wants to reserve a worker thread for a particular task.
     *
     * @param task the task
     * @return A response indicating whether this server can reserve a thread.
     */
    AssignResponse onAssign(TaskInfo task);

    /**
     * Runs the server loop.  Returns only on shutdown.
     */
    void run();

    /**
     * The number of free threads.
     *
     * @return the number of free threads.
     */
    int freeThreads();

    /**
     * The maximum number of worker threads.
     *
     * @return the maximum number of worker threads.
     */
    int totalThreads();

    /**
     * A client is allowing the worker on this task to proceed.
     *
     * @param task the task from the client
     */
    void onGo(TaskInfo task);

    /**
     * Invoked by the client when it considers a task complete (errored, or successful)
     *
     * @param task the task that is complete.
     */
    void onRelease(TaskInfo task);
}
