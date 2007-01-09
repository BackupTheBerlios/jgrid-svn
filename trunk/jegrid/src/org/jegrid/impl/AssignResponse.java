package org.jegrid.impl;

import org.jegrid.NodeAddress;
import org.jegrid.NodeStatus;

import java.io.Serializable;

/**
 * Generated by the server in response to an assign message.
 * <br> User: jdavis
 * Date: Oct 7, 2006
 * Time: 11:43:07 AM
 */
public class AssignResponse implements Serializable
{
    private boolean accepted;
    private NodeStatus status;

    public AssignResponse(NodeStatus status, boolean accepted)
    {
        this.status = status;
        this.accepted = accepted;
    }

    public NodeAddress getServer()
    {
        return status.getNodeAddress();
    }

    public String toString()
    {
        return "AssignResponse{" +
                "accepted=" + accepted +
                ", status=" + status +
                '}';
    }

    public boolean accepted()
    {
        return accepted;
    }

    public NodeStatus getNodeStatus()
    {
        return status;
    }
}
