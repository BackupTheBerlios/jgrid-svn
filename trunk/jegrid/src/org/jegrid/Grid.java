package org.jegrid;

/**
 * TODO: Add class level javadoc.
 * <br>User: jdavis
 * Date: Sep 21, 2006
 * Time: 5:04:13 PM
 */
public interface Grid
{
    /**
     * An observer simply monitors the grid.  Grid instances of this type cannot
     * do anything but look at the status.
     */
    int TYPE_OBSERVER = 0;
    /**
     * Clients can submit jobs to the grid.
     */
    int TYPE_CLIENT = 1;
    /**
     * Servers process jobs that clients submit.
     */
    int TYPE_SERVER = 2;

    /**
     * Returns the client interface for submitting jobs and monitoring job status.
     * @return The client interface.
     */
    Client getClient();

    /**
     * Returns the server interfaces for directly monitoring a server.
     */
    Server getServer();

    void connect();

    void disconnect();
}
