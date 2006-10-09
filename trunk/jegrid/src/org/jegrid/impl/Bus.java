package org.jegrid.impl;

import org.jegrid.NodeAddress;
import org.jegrid.NodeStatus;
import org.jegrid.TaskData;
import org.jegrid.GridException;

import java.util.Collection;

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

    NodeStatus[] getGridStatus()
            ;

    void sayHello()
            ;

    TaskData getNextInput(NodeAddress client, int taskId)
            ;

    void putOutput(NodeAddress client, int taskId, TaskData output)
            ;

    void taskFailed(NodeAddress client, int taskId, GridException ge)
            ;
}