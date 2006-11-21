package org.jegrid;

import java.util.Iterator;

/**
 * The status of the grid, including the status of each node.
 * <br>User: Joshua Davis
 * Date: Oct 8, 2006
 * Time: 9:45:44 AM
 */
public interface GridStatus
{
    /**
     * The number of nodes on the grid including clients, servers, and observers.
     *
     * @return the number of nodes on the grid
     */
    int getNumberOfNodes();

    /**
     * An iterator returning a NodeStatus for each node on the grid.
     *
     * @return iterator returning a NodeStatus for each node on the grid.
     */
    Iterator iterator();

    /**
     * The number of servers in the grid.
     *
     * @return the number of servers in the grid.
     */
    int getNumberOfServers();
}
