package org.jegrid.jgroups;

import org.apache.log4j.Logger;
import org.jegrid.GridConfiguration;
import org.jegrid.GridException;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;

/**
 * JGroups implementation of the messaging layer.
 * <br>User: jdavis
 * Date: Sep 21, 2006
 * Time: 5:10:41 PM
 */
public class JGroupsBus
{
    private static Logger log = Logger.getLogger(JGroupsBus.class);

    private boolean running = false;
    private Channel channel;
    private GridConfiguration config;
    private String localAddress;

    public void connect() {
        synchronized (this) {
            if (running) {
                return;
            }
            log.info("Connecting...");
            doConnect();
            running = true;
            notify();
            log.info(getLocalAddress() + " connected.");
        }
    }

    public String getLocalAddress()
    {
        return localAddress;
    }

    private void doConnect() {
        try {
            if (channel == null)
                channel = new JChannel(config.getBusConfiguration());
            channel.setOpt(Channel.VIEW, Boolean.TRUE);
            channel.setOpt(Channel.GET_STATE_EVENTS, Boolean.TRUE);
            channel.connect(config.getGridName());
            if (log.isDebugEnabled())
                log.debug("doConnect() : channel connected.");
            localAddress = channel.getLocalAddress().toString();
        }
        catch (ChannelException e) {
            disconnect();
            throw new GridException(e);
        }
        catch (GridException e) {
            disconnect();
            throw e;
        }
    }

    public void disconnect() {
        synchronized (this) {
            if (!running)
                return;
            String localAddress = getLocalAddress();

            // Close the channel.
            if (channel != null) {
                channel.close();
                channel = null;
            }
            running = false;
            notify();
            log.info(localAddress + " disconnected.");
        }
    }

}
