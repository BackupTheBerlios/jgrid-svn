package org.jegrid.jgroups;

import org.jgroups.*;
import org.jgroups.blocks.PullPushAdapter;
import org.jgroups.blocks.RequestHandler;
import org.apache.log4j.Logger;
import org.jegrid.impl.GridImplementor;

import java.util.Vector;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Translates JGroups messages into grid callbacks.
 * <br> User: jdavis
 * Date: Sep 30, 2006
 * Time: 9:35:11 PM
 */
public class JGroupsListener implements ChannelListener, MessageListener, MembershipListener
{
    private static Logger log = Logger.getLogger(JGroupsListener.class);
    private JGroupsBus bus;
    private GridImplementor grid;
    private View currentView;

    public JGroupsListener(JGroupsBus jGroupsBus, GridImplementor grid)
    {
        this.bus = jGroupsBus;
        this.grid = grid;
    }

    public void channelConnected(Channel channel)
    {
        log.info("channelConnected() " + channel.getLocalAddress());
    }

    public void channelDisconnected(Channel channel)
    {
        log.info("channelDisconnected() " + channel.getLocalAddress());
    }

    public void channelClosed(Channel channel)
    {
    }

    public void channelShunned()
    {
    }

    public void channelReconnected(Address addr)
    {
        log.info("channelReconnected() " + addr);
    }

    public void receive(Message msg)
    {
        log.info("receive " + msg);
    }

    public byte[] getState()
    {
        return new byte[0];
    }

    public void setState(byte[] state)
    {
    }

    public void viewAccepted(View newView)
    {
        log.info("viewAccepted " + newView);
        // Diff the views.
        ViewDiff diff = new ViewDiff(currentView, newView);
        // Create a set of JGroupsAddresses for the diff.
        Set joined = toNodeAddresses(diff.getJoined());
        Set left = toNodeAddresses(diff.getLeft());
        grid.onMembershipChange(joined, left);
        currentView = newView;
    }

    private Set toNodeAddresses(Set joined)
    {
        Set set = new HashSet(joined.size());
        for (Iterator iterator = joined.iterator(); iterator.hasNext();)
        {
            Address address = (Address) iterator.next();
            set.add(new JGroupsAddress(address));
        }
        return set;
    }

    public void suspect(Address suspected_mbr)
    {
    }

    public void block()
    {
    }
}
