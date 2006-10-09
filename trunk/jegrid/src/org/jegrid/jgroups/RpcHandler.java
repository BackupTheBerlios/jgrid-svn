package org.jegrid.jgroups;

import org.apache.log4j.Logger;
import org.jegrid.impl.*;
import org.jegrid.NodeStatus;
import org.jegrid.TaskData;
import org.jegrid.GridException;
import org.jegrid.NodeAddress;

/**
 * Handles RPCs from the RpcDispatcher in the JGroupsBus.
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

    public String _hello(NodeStatus from)
    {
        if (log.isDebugEnabled())
            log.debug("_hello from " + from);
        grid.onHello(from);
        return "hi there!";
    }

    public String _goodbye()
    {
        if (log.isDebugEnabled())
            log.debug("_goodbye");
        return "see ya!";
    }

    public NodeStatus _localStatus()
    {
        if (log.isDebugEnabled())
            log.debug("_localStatus");
        return grid.getLocalStatus();
    }

    // === Server messages ===

    public AssignResponse _assign(TaskInfo task)
    {
        if (log.isDebugEnabled())
            log.debug("_assign");
        Server server = grid.getServer();
        // If we're not a server, return null.
        if (server == null)
        {
            log.warn("No server here.");
            return null;
        }
        // Otherwise, dispatch the method call to the server.
        else
        {
            AssignResponse response = server.onAssign(task);
            if (log.isDebugEnabled())
                log.debug("_assign returning : " + response.toString());
            return response;
        }
    }

    // === Client messages ===

    public TaskData _nextInput(Integer taskId, NodeAddress server)
    {
        if (log.isDebugEnabled())
            log.debug("_nextInput");
        ClientImplementor client = (ClientImplementor) grid.getClient();
        if (client == null)
        {
            log.warn("No client here, sorry.");
            return null;
        }
        else
            return client.getNextInput(taskId.intValue(), server);
    }

    public void _putOutput(Integer taskId, TaskData output)
    {
        if (log.isDebugEnabled())
            log.debug("_putOutput");
        ClientImplementor client = (ClientImplementor) grid.getClient();
        if (client == null)
        {
            log.warn("No client here.");
        }
        else
            client.putOutput(taskId.intValue(), output);
    }

    public void _taskFailed(Integer taskId, GridException t)
    {
        if (log.isDebugEnabled())
            log.debug("_taskFailed");
        ClientImplementor client = (ClientImplementor) grid.getClient();
        if (client == null)
        {
            log.warn("No client here.");
        }
        else
            client.taskFailed(taskId.intValue(), t);
    }
}
