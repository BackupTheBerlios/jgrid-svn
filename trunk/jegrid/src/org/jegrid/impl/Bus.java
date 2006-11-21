package org.jegrid.impl;

import org.apache.log4j.spi.LoggingEvent;
import org.jegrid.*;

/**
 * Internal interface to the communication bus.
 * <br>User: jdavis
 * Date: Sep 30, 2006
 * Time: 7:32:12 AM
 */
public interface Bus
{
    void connect();

    void disconnect();

    NodeAddress getAddress();

    AssignResponse[] assign(NodeAddress[] servers, TaskId taskId)
            ;

    void go(AssignResponse[] servers, GoMessage goMessage) throws Exception
            ;

    NodeStatus[] getGridStatus()
            ;

    void broadcastNodeStatus()
            ;

    TaskData getNextInput(TaskId taskId, TaskData output) throws RpcTimeoutException
            ;

    void taskFailed(TaskId taskId, GridException ge) throws RpcTimeoutException
            ;

    void release(TaskId taskId) throws Exception
            ;

    boolean assignTask(NodeAddress address, TaskRequest request) throws RpcTimeoutException
            ;

    void apppend(TaskId id, LoggingEvent event) throws RpcTimeoutException
            ;

    void shutdownServers()
            ;

    NodeAddress getCoordinator();

    void goodbye(NodeAddress addr);
}
