package org.jegrid.impl;

import org.jegrid.Grid;
import org.jegrid.NodeAddress;
import org.jegrid.NodeStatus;
import org.jegrid.util.MicroContainer;

import java.util.Set;

/**
 * Internal interface for the main grid session.
 * <br>User: jdavis
 * Date: Sep 30, 2006
 * Time: 7:35:11 AM
 */
public interface GridImplementor extends Grid
{
    /**
     * Returns the server component, or null if this isn't a server.
     *
     * @return the server component, or null if this isn't a server.
     */
    Server getServer();

    /**
     * Returns the mark for the next membership change for a later call to waitForMembershipChange().
     *
     * @return the mark for the next membership change
     */
    int nextMembershipChange();

    /**
     * Waits for a membership change condition based on the mark.
     *
     * @param mark    the number of membership changes to wait for (from nextMembershipChange(), usually)
     * @param timeout the number of milliseconds to wait.
     */
    void waitForMembershipChange(int mark, long timeout);

    /**
     * Initializes the grid implementation with the given micro container.  This is
     * invoked once during GridConfiguration.configure().
     *
     * @param mc the micro container.
     */
    void initialize(MicroContainer mc);

    // Callback methods for grid membership invoked by the bus listener.

    /**
     * Invoked when the membership changes.
     *
     * @param joined       The addresses that joined up with the grid.
     * @param left         The addresses that have left the grid.
     * @param localAddress
     */
    void onMembershipChange(Set joined, Set left, NodeAddress localAddress);

    /**
     * Invoked when a node joins the grid for the first time.   Everybody says hello.
     *
     * @param from the status of the node that has just joined the grid
     */
    void onNodeStatus(NodeStatus from);

    void onNewCoordinator(NodeAddress address)
            ;

    void waitForServers() throws InterruptedException
            ;

    Object instantiateObject(String clazz)
            ;

    void doShutdownServers()
            ;

    void onNodeStopped(NodeAddress addr);

    boolean isMember(NodeAddress client);

    long getStartTime();

    String getHostName();
}
