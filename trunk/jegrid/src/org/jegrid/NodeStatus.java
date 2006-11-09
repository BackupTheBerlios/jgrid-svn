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

    /**
     * @return The total amount of heap space on the node, in bytes.
     */
    long getTotalMemory()
            ;

    /**
     * @return the start time of the node, in millis since the epoch.
     */
    long getStartTime()
            ;

    /**
     * @return the number of tasks accepted so far
     */
    int getTasksAccepted()
            ;

    /**
     * @return the time the last task was accepted, in millis since the epoch
     */
    long getLastTaskAccepted()
            ;

    /**
     * @return the time the last status message was received in millis since the epoch
     */
    long getStatusAsOf()
            ;

    /**
     * @return the name of the host the node is running on
     */
    String getHostName()
            ;
}
