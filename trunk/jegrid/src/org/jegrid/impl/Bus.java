package org.jegrid.impl;

import org.jegrid.NodeAddress;

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
}
