package org.jegrid.jgroups;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.jegrid.*;
import org.jegrid.impl.*;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;
import org.jgroups.blocks.GroupRequest;
import org.jgroups.protocols.AUTOCONF;

import java.util.ArrayList;
import java.util.List;

/**
 * JGroups implementation of the messaging layer.
 * <br>User: jdavis
 * Date: Sep 21, 2006
 * Time: 5:10:41 PM
 */
public class JGroupsBus implements Bus
{
    private static Logger log = Logger.getLogger(JGroupsBus.class);

    private static final long TIMEOUT = 10000;
    private static final Object[] NO_ARGS = new Object[0];
    private static final Class[] NO_TYPES = new Class[0];
    private static final long NEXT_INPUT_TIMEOUT = TIMEOUT * 3;

    private boolean running = false;
    private Channel channel;
    private GridConfiguration config;
    private JGroupsAddress localAddress;
    private JGroupsListener listener;
    private GridImplementor grid;
    private RpcDispatcher dispatcher;
    private NodeAddress coordinator;

    public JGroupsBus(GridConfiguration config, GridImplementor grid)
    {
        this.config = config;
        this.grid = grid;
    }

    Channel getChannel()
    {
        return channel;
    }

    public synchronized void connect()
    {
        if (running)
        {
            return;
        }
        log.info("Connecting...");
        doConnect();
        running = true;
        notify();
        log.info(getAddress() + " connected.");
    }

    private void doConnect()
    {
        try
        {
            if (channel == null)
            {
                // Before we create the JChannel, make sure UDP is working.
                checkUDP();
                // Use the DOM element configuration if it exists.
                if (config.getBusConfigurationElement() != null)
                    channel = new JChannel(config.getBusConfigurationElement());
                else // Otherwise use the configuration resource name.
                    channel = new JChannel(config.getBusConfiguration());
            }
            channel.setOpt(Channel.VIEW, Boolean.TRUE);
            channel.setOpt(Channel.GET_STATE_EVENTS, Boolean.TRUE);
            if (config.getGridName() == null || config.getGridName().length() == 0)
                throw new GridException("No grid name.  Please provide a grid name so the grid can federate.");
            // Before we connect, set up the listener.
            listener = new JGroupsListener(grid);
            dispatcher = new RpcDispatcher(channel, listener, new RpcHandler(grid));
            channel.addChannelListener(listener);       // Listens for connect/disconnect events.
            channel.connect(config.getGridName());      // Okay, connect the channel.
            if (log.isDebugEnabled())
                log.debug("doConnect() : channel connected.");
            broadcastNodeStatus();
        }
        // Re-throw GridExceptions
        catch (GridException e)
        {
            disconnect();
            throw e;
        }
        // Wrap other exceptions.
        catch (ChannelException e)
        {
            disconnect();
            throw new GridException(e);
        }
        // Log and wrap unexpected exceptions.        
        catch (Exception e)
        {
            disconnect();
            log.error("Unexpected exception: " + e, e);
            throw new GridException(e);
        }
    }

    private void checkUDP()
    {
        try
        {
            log.info("Auto detecting fragment size...");
            int size = AUTOCONF.senseMaxFragSizeStatic();
            log.info("maxFragSize=" + size);
        }
        catch (Exception ex)
        {
            String msg = "Unable to create a DatagramSocket: " + ex;
            log.error(msg, ex);
            throw new GridException(msg, ex);
        }
    }

    public synchronized void disconnect()
    {
        if (!running)
            return;

        JGroupsAddress addr = localAddress;
        localAddress = null;
        // Close the channel.
        if (channel != null)
        {
            channel.disconnect();
            channel.close();
            channel = null;
        }
        listener = null;
        running = false;
        notify();
        log.info(addr + " disconnected.");
    }

    public synchronized NodeAddress getAddress()
    {
        if (localAddress == null)
            localAddress = new JGroupsAddress(channel.getLocalAddress());
        return localAddress;
    }

    public void broadcastNodeStatus()
    {
        NodeStatus localStatus = grid.getLocalStatus();
        dispatcher.broadcast(
                null, "_status",
                new Object[]{localStatus}, new Class[]{NodeStatus.class},
                GroupRequest.GET_NONE, 0);
    }

    public TaskData getNextInput(TaskId taskId, TaskData output) throws RpcTimeoutException
    {
        Object o = dispatcher.call(taskId.getClient(), "_nextInput",
                new Object[]{taskId, localAddress, output},
                new Class[]{taskId.getClass(), NodeAddress.class, TaskData.class},
                GroupRequest.GET_ALL,
                NEXT_INPUT_TIMEOUT);   // Wait a little longer for this, sometimes the client is slow.
        return (TaskData) o;
    }

    public void taskFailed(TaskId taskId, GridException ge) throws RpcTimeoutException
    {
        log.warn("Task " + taskId + " failed with " + ge, ge);
        dispatcher.call(taskId.getClient(), "_taskFailed",
                new Object[]{taskId, ge},
                new Class[]{taskId.getClass(), ge.getClass()},
                GroupRequest.GET_ALL,
                TIMEOUT);
    }

    public void sayGoodbye()
    {
        dispatcher.broadcast(
                null, "_goodbye", NO_ARGS, NO_TYPES, GroupRequest.GET_NONE, 0);
    }

    /**
     * Send assign messages to the specified addresses and wait for the responses.
     *
     * @param servers the addresses of the servers to send the message to.
     * @param taskId  the task to assign.
     * @return The responses.
     */
    public AssignResponse[] assign(NodeAddress[] servers, TaskId taskId)
    {
        List responses = dispatcher.broadcast(servers, "_assign",
                new Object[]{taskId},
                new Class[]{taskId.getClass()},
                GroupRequest.GET_ALL,
                TIMEOUT);
        AssignResponse[] rv = new AssignResponse[responses.size()];
        for (int i = 0; i < rv.length; i++)
        {
            Object o = responses.get(i);
            if (log.isDebugEnabled())
                log.debug("assign() : Rsp #" + i + " " + o);
            rv[i] = null;
            if (o instanceof AssignResponse)
                rv[i] = (AssignResponse) o;
            else if (o instanceof ServerBusyException)
            {
                ServerBusyException sbe = (ServerBusyException) o;
                log.warn(sbe.getMessage());
            }
            else if (o instanceof Exception)
            {
                log.error(o, (Exception) o);
            }
        }
        return rv;
    }

    public void go(AssignResponse[] servers, GoMessage goMessage) throws Exception
    {
        if (servers == null || servers.length == 0)
            return;
        List list = new ArrayList(servers.length);
        for (int i = 0; i < servers.length; i++)
        {
            AssignResponse server = servers[i];
            if (server != null && server.accepted())
                list.add(server.getServer());
        }
        NodeAddress[] addrs = (NodeAddress[]) list.toArray(new NodeAddress[list.size()]);
        dispatcher.broadcastWithExceptionCheck(
                addrs, "_go",
                new Object[]{goMessage},
                new Class[]{goMessage.getClass()},
                GroupRequest.GET_ALL, TIMEOUT);
    }

    public void release(TaskId id) throws Exception
    {
        dispatcher.broadcastWithExceptionCheck(
                null, "_release",
                new Object[]{id},
                new Class[]{id.getClass()},
                GroupRequest.GET_NONE, 0);
    }

    public boolean assignTask(NodeAddress server, TaskRequest request) throws RpcTimeoutException
    {
        Object o = dispatcher.call(server, "_assignTask",
                new Object[]{request},
                new Class[]{TaskRequest.class},
                GroupRequest.GET_ALL,
                NEXT_INPUT_TIMEOUT);   // Wait a little longer for this, sometimes the client is slow.
        if (o == null)
            return false;
        Boolean accpted = (Boolean) o;
        return accpted.booleanValue();
    }

    public void apppend(TaskId taskId, LoggingEvent event) throws RpcTimeoutException
    {
        dispatcher.call(taskId.getClient(), "_append",
                new Object[]{taskId, event},
                new Class[]{taskId.getClass(), event.getClass()},
                GroupRequest.GET_NONE,
                0);
    }

    public void shutdownServers()
    {
        log.info("*** Sending _shutdownServers ... ***");
        dispatcher.broadcast(null, "_shutdownServers",
                new Object[0],
                new Class[0],
                GroupRequest.GET_NONE,
                0);
    }

    public NodeAddress getCoordinator()
    {
        return listener.getCoordinator();
    }

    public NodeStatus[] getGridStatus()
    {
        List responses;
        try
        {
            responses = dispatcher.broadcastWithExceptionCheck(
                    null, "_localStatus", NO_ARGS, NO_TYPES, GroupRequest.GET_ALL, TIMEOUT);
        }
        catch (GridException ge)
        {
            throw ge;
        }
        catch (Exception e)
        {
            throw new GridException(e);
        }
        List list = new ArrayList(responses.size());
        for (int i = 0; i < responses.size(); i++)
        {
            NodeStatus ns = (NodeStatus) responses.get(i);
            if (ns != null)
                list.add(ns);
        }
        return (NodeStatus[]) list.toArray(new NodeStatus[list.size()]);
    }
}
