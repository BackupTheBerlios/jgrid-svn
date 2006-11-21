package org.jegrid.jgroups;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.jegrid.*;
import org.jegrid.impl.*;

/**
 * Handles RPCs from the GridRpcDispatcher in the JGroupsBus.
 * <br> User: jdavis
 * Date: Oct 7, 2006
 * Time: 11:06:40 AM
 */
public class RpcHandler
{
    private static Logger log = Logger.getLogger(RpcHandler.class);

    private GridImplementor grid;

    public RpcHandler(GridImplementor grid)
    {
        this.grid = grid;
    }

    // === General messages ===

    public String _status(NodeStatus from)
    {
        grid.onNodeStatus(from);
        return "hi there!";
    }

    public String _goodbye(NodeAddress addr)
    {
        grid.onNodeStopped(addr);
        return "see ya!";
    }

    public NodeStatus _localStatus()
    {
        return grid.getLocalStatus();
    }

    public void _shutdownServers()
    {
        grid.doShutdownServers();
    }

    // === Server messages ===

    public AssignResponse _assign(TaskId id)
    {
        Server server = grid.getServer();
        // If we're not a server, return null.
        if (server == null)
        {
            log.warn("_assign: No server here.");
            return null;
        }
        // Otherwise, dispatch the method call to the server.
        else
        {
            if (log.isDebugEnabled())
                log.debug("_assign");
            AssignResponse response = server.onAssign(id);
            if (log.isDebugEnabled())
                log.debug("_assign returning : " + response.toString());
            return response;
        }
    }

    public boolean _assignTask(TaskRequest request)
    {
        Server server = grid.getServer();
        // If we're not a server, return null.
        if (server == null)
        {
            log.warn("_assignTask: No server here.");
            return false;
        }
        else
        {
            return server.onAssignTask(request);
        }
    }

    public void _go(GoMessage goMessage)
    {
        Server server = grid.getServer();
        // Ignore go messages to non-servers.
        if (server != null)
        {
            if (log.isDebugEnabled())
                log.debug("_go");
            server.onGo(goMessage);
        }
    }

    public void _release(TaskId id)
    {
        Server server = grid.getServer();
        // Ignore go messages to non-servers.
        if (server != null)
        {
            if (log.isDebugEnabled())
                log.debug("_release");
            server.onRelease(id);
        }
    }

    // === Client messages ===

    public TaskData _nextInput(TaskId taskId, NodeAddress server, TaskData output)
    {
        ClientImplementor client = (ClientImplementor) grid.getClient();
        if (client == null)
        {
            log.warn("No client here, sorry.");
            return null;
        }
        else
            return client.getNextInput(taskId, server, output);
    }

    public void _taskFailed(TaskId taskId, GridException t)
    {
        if (log.isDebugEnabled())
            log.debug("_taskFailed");
        ClientImplementor client = (ClientImplementor) grid.getClient();
        if (client == null)
        {
            log.warn("No client here.");
        }
        else
            client.taskFailed(taskId, t);
    }

    public void _append(TaskId taskId, LoggingEvent event)
    {
        //hmm... now what
        ClientImplementor client = (ClientImplementor) grid.getClient();
        if (client == null)
        {
            log.warn("No client here.");
        }
        else
            client.append(taskId, event);

    }
}
