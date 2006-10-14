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
     *
     * @return the new task
     */
    Task createTask();
}
