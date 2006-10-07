package org.jegrid.jgroups;

import org.apache.log4j.Logger;
import org.jegrid.impl.Server;
import org.jegrid.impl.TaskInfo;
import org.jegrid.impl.AssignResponse;

/**
 * Handles RPCs from the RpcDispatcher in the JGroupsBus.
 * <br> User: jdavis
 * Date: Oct 7, 2006
 * Time: 11:06:40 AM
 */
public class RpcHandler
{
    private static Logger log = Logger.getLogger(RpcHandler.class);
    private Server server;

    public RpcHandler(Server server)
    {
        this.server = server;
    }

    public String _hello()
    {
        if (log.isDebugEnabled())
            log.debug("_hello");
        return "hi there!";
    }

    public String _goodbye()
    {
        return "see ya!";
    }

    // === Server messages ===

    public AssignResponse _assign(TaskInfo task)
    {
        // If we're not a server, return null.
        if (server == null)
            return null;
            // Otherwise, dispatch the method call to the server.
        else
            return server.onAssign(task);
    }
}
