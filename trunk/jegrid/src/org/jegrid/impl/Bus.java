package org.jegrid.impl;

import org.jegrid.NodeAddress;
import org.jegrid.NodeStatus;
import org.jegrid.TaskData;
import org.jegrid.GridException;

/**
 * TODO: Add class level comments.
 * <br>User: jdavis
 * Date: Sep 30, 2006
 * Time: 7:32:12 AM
 */
public interface Bus
{
    void connect();

    void disconnect();

    NodeAddress getAddress();

    AssignResponse[] assign(NodeAddress[] servers, TaskInfo taskInfo)
            ;

    void go(TaskInfo info)
            ;

    NodeStatus[] getGridStatus()
            ;

    void broadcastNodeStatus()
            ;

    TaskData getNextInput(NodeAddress client, int taskId, TaskData output) throws RpcTimeoutException
            ;

    void taskFailed(NodeAddress client, int taskId, GridException ge)
            ;

    void release(TaskInfo info)
            ;

}
