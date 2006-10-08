package org.jegrid;

/**
 * Represents a connection to the grid.
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
     * Unknown node type.
     */
    int TYPE_UNKNOWN = -1;

    /**
     * Returns the client interface for submitting jobs and monitoring job status.
     *
     * @return The client interface.
     */
    Client getClient();

    /**
     * Connects to the grid.
     */
    void connect();


    /**
     * Disconnect from the grid.
     */
    void disconnect();

    /**
     * Returns the address of the local node.
     *
     * @return the address of the local node.
     */
    NodeAddress getLocalAddress();

    /**
     * Returns the status of the local node.
     *
     * @return the status of the local node
     */
    NodeStatus getLocalStatus();

    /**
     * Returns an unmodifiable collection of NodeStatus, one for very member of the grid.  This
     * will be the cached status that is on this grid node.
     * @return an an unmodifiable collection of NodeStatus, one for very member of the grid.
     * @param cached If true, the locally cached status will be used.  Otherwise, a
     * broadcast message will be sent out and the cache will be refreshed with the responses.
     */
    GridStatus getGridStatus(boolean cached);

    /**
     * Run the server loop on the current thread.  This will not return until the grid is
     * shut down.
     */
    void runServer();
}
