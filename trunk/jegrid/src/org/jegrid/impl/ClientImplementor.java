package org.jegrid.impl;

import org.apache.log4j.spi.LoggingEvent;
import org.jegrid.*;

import java.util.Set;

/**
 * Internal interface for clients
 * <br>User: Joshua Davis
 * Date: Oct 8, 2006
 * Time: 7:15:37 AM
 */
public interface ClientImplementor extends Client
{
    TaskData getNextInput(TaskId taskId, NodeAddress server, TaskData output)
            ;

    void taskFailed(TaskId taskId, GridException throwable)
            ;

    void onMembershipChange(Set joined, Set left)
            ;

    void append(TaskId taskId, LoggingEvent event)
            ;
}
