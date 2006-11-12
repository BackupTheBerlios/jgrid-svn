package org.jegrid.impl;

import org.jegrid.TaskId;
import org.jegrid.TaskRequest;

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
     * @param id the task id
     * @return A response indicating whether this server can reserve a thread.
     */
    AssignResponse onAssign(TaskId id);

    /**
     * Runs the server loop.  Returns only on shutdownServers.
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
     * @param goMessage the go message
     */
    void onGo(GoMessage goMessage);

    /**
     * Invoked by the client when it considers a task complete (errored, or successful)
     *
     * @param id the task id
     */
    void onRelease(TaskId id);

    /**
     * Invoked by the client when it wants to hand the entire task over to this server.
     *
     * @param request The task request.
     * @return true if it was accepted, false if not
     */
    boolean onAssignTask(TaskRequest request)
            ;

    /**
     * Returns the number of tasks accepted by this server.
     * @return the number of tasks accepted by this server.
     */
    int tasksAccepted()
            ;

    /**
     * @return the timestamp of the last accepted task
     */
    long lastTaskAccepted()
            ;

    void doShutdown()
            ;
}
