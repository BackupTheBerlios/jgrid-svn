package org.jegrid.jgroups;

import org.apache.log4j.Logger;
import org.jegrid.*;
import org.jgroups.*;

/**
 * JGroups implementation of the messaging layer.
 * <br>User: jdavis
 * Date: Sep 21, 2006
 * Time: 5:10:41 PM
 */
public class JGroupsBus implements Bus
{
    private static Logger log = Logger.getLogger(JGroupsBus.class);
    private static final String DEFAULT_PROPS = "default.xml";
    private boolean running = false;
    private Channel channel;
    private GridConfiguration config;
    private Address address;
    private JGroupsAddress localAddress;
    private JGroupsListener listener;
    private GridImplementor grid;

    public JGroupsBus(GridConfiguration config, GridImplementor gridImpl)
    {
        this.config = config;
        this.grid = gridImpl;
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
            listener = new JGroupsListener(this,grid);
            channel.addChannelListener(listener);
            channel.setReceiver(listener);
            channel.connect(config.getGridName());
            if (log.isDebugEnabled())
                log.debug("doConnect() : channel connected.");
            address = channel.getLocalAddress();
            localAddress = new JGroupsAddress(address);
            Message m = new Message(null, address, "hello!".getBytes());
            channel.send(m);                                
        }
        catch (ChannelException e)
        {
            disconnect();
            throw new GridException(e);
        }
        catch (GridException e)
        {
            disconnect();
            throw e;
        }
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
