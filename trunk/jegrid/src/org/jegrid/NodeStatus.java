package org.jegrid;

/**
 * Represents the status of a particular node in the grid.
 * <br> User: jdavis
 * Date: Oct 4, 2006
 * Time: 7:58:55 AM
 */
public interface NodeStatus
{
    /**
     * Returns the address of the node.
     *
     * @return the address of the node.
     */
    NodeAddress getNodeAddress();

    /**
     * Returns the address of the coordinator (as far as the node is concerned).
     *
     * @return the address of the coordinator
     */
    NodeAddress getCoordinator();

    /**
     * Returns the type of node.  (See Grid.TYPE_XXX)
     *
     * @return the type of node (See Grid.TYPE_XXX)
     */
    int getType();

    /**
     * The number of available workers on the node. Always zero for non-server nodes.
     *
     * @return the number of available workers on the node.
     */
    int getAvailableWorkers()
            ;

    /**
     * The total number of workers on the node. Always zero for non-server nodes.
     *
     * @return the total number of workers on the node
     */
    int getTotalWorkers()
            ;

    /**
     * The amount of free heap space on the node, in bytes.
     *
     * @return The amount of free heap space on the node, in bytes.
     */
    long getFreeMemory()
            ;
}
