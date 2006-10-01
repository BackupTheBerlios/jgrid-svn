package org.jegrid.jgroups;

import org.jgroups.*;
import org.apache.log4j.Logger;

/**
 * TODO: Add class level javadoc
 * <br> User: jdavis
 * Date: Sep 30, 2006
 * Time: 9:35:11 PM
 */
public class JGroupsListener implements ChannelListener, Receiver
{
    private static Logger log = Logger.getLogger(JGroupsListener.class);
    private JGroupsBus bus;

    public JGroupsListener(JGroupsBus jGroupsBus)
    {
        this.bus = jGroupsBus;
    }

    public void channelConnected(Channel channel)
    {
        log.info("channelConnected() " + channel);
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void channelDisconnected(Channel channel)
    {
        log.info("channelDisconnected() " + channel);
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void channelClosed(Channel channel)
    {
        log.info("channelClosed() " + channel);
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void channelShunned()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void channelReconnected(Address addr)
    {
        log.info("channelReconnected() " + addr);
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void receive(Message msg)
    {
        log.info("receive " + msg);
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte[]        getState()
    {
        return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setState(byte[] state)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void viewAccepted(View new_view)
    {
        log.info("viewAccepted " + new_view);
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void suspect(Address suspected_mbr)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void block()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
