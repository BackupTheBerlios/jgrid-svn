package org.jegrid.jgroups;

import org.apache.log4j.Logger;
import org.jegrid.*;
import org.jegrid.impl.*;
import org.jgroups.*;
import org.jgroups.protocols.AUTOCONF;
import org.jgroups.blocks.GroupRequest;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Vector;

/**
 * JGroups implementation of the messaging layer.
 * <br>User: jdavis
 * Date: Sep 21, 2006
 * Time: 5:10:41 PM
 */
public class JGroupsBus implements Bus
{
    private static Logger log = Logger.getLogger(JGroupsBus.class);
    private boolean running = false;
    private Channel channel;
    private GridConfiguration config;
    private Address address;
    private JGroupsAddress localAddress;
    private JGroupsListener listener;
    private GridImplementor grid;
    private RpcDispatcher dispatcher;
    private static final long TIMEOUT = 10000;
    private static final Object[] NO_ARGS = new Object[0];
    private static final Class[] NO_TYPES = new Class[0];
    private static final long NEXT_INPUT_TIMEOUT = TIMEOUT * 3;

    public JGroupsBus(GridConfiguration config, GridImplementor grid)
    {
        this.config = config;
        this.grid = grid;
    }

    Channel getChannel()
    {
        return channel;
    }

    public void connect()
    {
        synchronized (this)
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
    }

    private void doConnect()
    {
        try
        {
            if (channel == null)
            {
                // Before we create the JChannel, make sure UDP is working.
                checkUDP();
                channel = new JChannel(config.getBusConfiguration());
            }
            channel.setOpt(Channel.VIEW, Boolean.TRUE);
            channel.setOpt(Channel.GET_STATE_EVENTS, Boolean.TRUE);
            if (config.getGridName() == null || config.getGridName().length() == 0)
                throw new GridException("No grid name.  Please provide a grid name so the grid can federate.");
            // Before we connect, set up the listener.
            listener = new JGroupsListener(grid);
            RpcHandler handler = new RpcHandler(grid);
            dispatcher = new RpcDispatcher(channel, listener, listener, handler);
            channel.addChannelListener(listener);       // Listens for connect/disconnect events.
            channel.connect(config.getGridName());      // Okay, connect the channel.
            if (log.isDebugEnabled())
                log.debug("doConnect() : channel connected.");
            address = channel.getLocalAddress();
            localAddress = new JGroupsAddress(address);
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
            StringBuffer buf = new StringBuffer();
            buf.append("--- Network interfaces ---\n");
            Enumeration en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements())
            {
                NetworkInterface i = (NetworkInterface) en.nextElement();
                buf.append(i.toString());
            }
            InetAddress[] addresses = InetAddress.getAllByName(null);
            for (int i = 0; i < addresses.length; i++)
            {
                if (i > 0)
                    buf.append("\n");
                buf.append("address[").append(i).append("]=");
                buf.append(addresses[i].toString());
            }
            log.debug(buf.toString());
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

    public void disconnect()
    {
        synchronized (this)
        {
            if (!running)
                return;

            String localAddress = getAddress().toString();
            sayGoodbye();

            // Close the channel.
            if (channel != null)
            {
                channel.close();
                channel = null;
            }
            listener = null;
            address = null;
            running = false;
            notify();
            log.info(localAddress + " disconnected.");
        }
    }

    public NodeAddress getAddress()
    {
        return localAddress;
    }

    public void broadcastNodeStatus()
    {
        NodeStatus localStatus = grid.getLocalStatus();
        dispatcher.callRemoteMethods(
                null, "_status",
                new Object[]{localStatus}, new Class[]{NodeStatus.class},
                GroupRequest.GET_NONE, 0);
    }

    public TaskData getNextInput(NodeAddress client, int taskId, TaskData output) throws RpcTimeoutException
    {
        Address address = toAddress(client);
        try
        {
            Object o = dispatcher.callRemoteMethod(address, "_nextInput",
                    new Object[]{new Integer(taskId), localAddress, output},
                    new Class[]{Integer.class, NodeAddress.class, TaskData.class},
                    GroupRequest.GET_ALL,
                    NEXT_INPUT_TIMEOUT);   // Wait a little longer for this, sometimes the client is slow.
            checkForException(o);
            return (TaskData) o;
        }
        catch (GridException ge)
        {
            throw ge;
        }
        catch (TimeoutException e)
        {
            // NOTE: If this call times out it may mean that the client has already
            // given the server the input.
            throw new RpcTimeoutException(e);
        }
        catch (Exception e)
        {
            throw new GridException(e);
        }
    }

    private void checkForException(Object o)
            throws Exception
    {
        if (o instanceof Exception)
            throw(Exception) o;
    }

    public void taskFailed(NodeAddress client, int taskId, GridException ge)
    {
        Address address = toAddress(client);
        try
        {
            log.warn("Task " + taskId + " failed with " + ge, ge);
            dispatcher.callRemoteMethod(address, "_taskFailed",
                    new Object[]{new Integer(taskId), ge},
                    new Class[]{Integer.class, GridException.class},
                    GroupRequest.GET_ALL,
                    TIMEOUT);
        }
        catch (GridException g)
        {
            throw g;
        }
        catch (Exception e)
        {
            throw new GridException(e);
        }
    }

    public void sayGoodbye()
    {
        dispatcher.callRemoteMethods(
                null, "_goodbye", NO_ARGS, NO_TYPES, GroupRequest.GET_NONE, 0);
    }

    /**
     * Send assign messages to the specified addresses and wait for the responses.
     *
     * @param servers  the addresses of the servers to send the message to.
     * @param taskInfo the task to assign.
     * @return The responses.
     */
    public AssignResponse[] assign(NodeAddress[] servers, TaskInfo taskInfo)
    {
        Vector dests = new Vector();
        for (int i = 0; i < servers.length; i++)
            dests.add(toAddress(servers[i]));

        RspList responses = dispatcher.callRemoteMethods(dests, "_assign",
                new Object[]{taskInfo},
                new Class[]{taskInfo.getClass()},
                GroupRequest.GET_ALL,
                TIMEOUT);
        AssignResponse[] rv = new AssignResponse[responses.size()];
        for (int i = 0; i < rv.length; i++)
        {
            Rsp rsp = (Rsp) responses.elementAt(i);
            if (log.isDebugEnabled())
                log.debug("assign() : Rsp #" + i + " " + rsp.toString());
            rv[i] = (AssignResponse) rsp.getValue();
        }
        return rv;
    }

    public void go(TaskInfo taskInfo)
    {
        dispatcher.callRemoteMethods(
                null, "_go",
                new Object[]{taskInfo},
                new Class[]{taskInfo.getClass()},
                GroupRequest.GET_NONE, 0);
    }

    public void release(TaskInfo taskInfo)
    {
        dispatcher.callRemoteMethods(
                null, "_release",
                new Object[]{taskInfo},
                new Class[]{taskInfo.getClass()},
                GroupRequest.GET_NONE, 0);
    }

    public NodeStatus[] getGridStatus()
    {
        RspList responses = dispatcher.callRemoteMethods(
                null, "_localStatus", NO_ARGS, NO_TYPES, GroupRequest.GET_ALL, TIMEOUT);
        NodeStatus[] rv = new NodeStatus[responses.size()];
        for (int i = 0; i < rv.length; i++)
        {
            Rsp rsp = (Rsp) responses.elementAt(i);
            try
            {
                checkForException(rv[i]);
            }
            catch (GridException g)
            {
                throw g;
            }
            catch (Exception e)
            {
                throw new GridException(e);
            }
            rv[i] = (NodeStatus) rsp.getValue();
        }
        return rv;
    }

    private Address toAddress(NodeAddress nodeAddress)
    {
        return ((JGroupsAddress) nodeAddress).getAddress();
    }

    Address getJGroupsAddress()
    {
        return channel.getLocalAddress();
    }
}
