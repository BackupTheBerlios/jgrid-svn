package org.jegrid.jgroups;

import org.apache.log4j.Logger;
import org.jegrid.*;
import org.jegrid.impl.GridImplementor;
import org.jegrid.impl.Bus;
import org.jgroups.*;
import org.jgroups.util.RspList;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.GroupRequest;

import java.net.DatagramSocket;
import java.net.InetAddress;
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
    private static final String DEFAULT_PROPS = "org/jegrid/jgroups/default.xml";
    private boolean running = false;
    private Channel channel;
    private GridConfiguration config;
    private Address address;
    private JGroupsAddress localAddress;
    private JGroupsListener listener;
    private GridImplementor grid;
    private MessageDispatcher dispatcher;

    public JGroupsBus(GridConfiguration config, GridImplementor gridImpl)
    {
        this.config = config;
        this.grid = gridImpl;
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
                if (config.getBusConfiguration() == null)
                    channel = new JChannel(DEFAULT_PROPS);
                else
                    channel = new JChannel(config.getBusConfiguration());
            }
            channel.setOpt(Channel.VIEW, Boolean.TRUE);
            channel.setOpt(Channel.GET_STATE_EVENTS, Boolean.TRUE);
            if (config.getGridName() == null || config.getGridName().length() == 0)
                throw new GridException("No grid name.  Please provide a grid name so the grid can federate.");
            // Before we connect, set up the listener.
            listener = new JGroupsListener(this, grid);
            dispatcher = new MessageDispatcher(channel,listener,listener);
            channel.addChannelListener(listener);       // Listens for connect/disconnect events.
            channel.connect(config.getGridName());      // Okay, connect the channel.
            if (log.isDebugEnabled())
                log.debug("doConnect() : channel connected.");
            address = channel.getLocalAddress();
            localAddress = new JGroupsAddress(address);
            Message m = createMessage();
            RspList list = dispatcher.castMessage(null,m, GroupRequest.GET_ALL,5000);
            log.info(list.toString());
        }
        catch (ChannelException e)
        {
            disconnect();
            log.error(e, e);
            throw new GridException(e);
        }
        catch (GridException e)
        {
            disconnect();
            throw e;
        }
    }

    private void checkUDP()
    {
        DatagramSocket sock = null;
        try
        {
            sock = new DatagramSocket();
            InetAddress local = InetAddress.getLocalHost();
            if (log.isDebugEnabled())
                log.debug("Local address is: " + local);
        }
        catch (Exception ex)
        {
            String msg = "Unable to create a DatagramSocket: " + ex;
            log.error(msg, ex);
            throw new GridException(msg, ex);
        }
        finally
        {
            if (sock != null)
                sock.close();
        }
    }

    Message createMessage()
    {
        return new Message(null, address, "hello!".getBytes());
    }

    public void disconnect()
    {
        synchronized (this)
        {
            if (!running)
                return;
            String localAddress = getAddress().toString();

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
}
