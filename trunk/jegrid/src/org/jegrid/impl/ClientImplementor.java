package org.jegrid.impl;

import org.jegrid.Client;
import org.jegrid.TaskData;
import org.jegrid.GridException;
import org.jegrid.NodeAddress;

import java.util.Set;

/**
 * Internal interface for clients
 * <br>User: Joshua Davis
 * Date: Oct 8, 2006
 * Time: 7:15:37 AM
 */
public interface ClientImplementor extends Client
{
    TaskData getNextInput(int taskId, NodeAddress server)
            ;

    void putOutput(int taskId, TaskData output)
            ;

    void taskFailed(int taskId, GridException throwable)
            ;

    void onMembershipChange(Set joined, Set left)
            ;
}
