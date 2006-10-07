package org.jegrid;

import org.jegrid.impl.Server;

import java.util.Collection;

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
     * Returns an unmodifiable collection of NodeStatus, one for very member of the grid.
     *
     * @return an an unmodifiable collection of NodeStatus, one for very member of the grid.
     */
    Collection getNodeStatus();

}
