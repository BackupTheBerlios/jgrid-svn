package org.jegrid.impl;

import org.jegrid.Grid;

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

    // Callback methods for grid membership invoked by the bus listener.
    void onMembershipChange(Set joined, Set left);

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
}
