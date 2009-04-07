package org.jegrid.jgroups;

import org.apache.log4j.Logger;
import org.jegrid.NodeAddress;
import org.jegrid.impl.GridImplementor;
import org.jgroups.*;

import java.util.*;

/**
 * Translates JGroups messages (e.g. membership changes) into grid callbacks.
 * <br> User: jdavis
 * Date: Sep 30, 2006
 * Time: 9:35:11 PM
 */
public class JGroupsListener implements ChannelListener, MessageListener, MembershipListener
{
    private static Logger log = Logger.getLogger(JGroupsListener.class);
    private GridImplementor grid;
    private View currentView;
    private NodeAddress coordinator;
    boolean connected = false;
    private List<View> deferredView = new ArrayList<View>(); // views that happened before the channel was connected.

    public JGroupsListener(GridImplementor grid)
    {
        this.grid = grid;
    }

    public void channelConnected(Channel channel)
    {
        log.info("channelConnected() " + channel.getLocalAddress());

        while (true)
        {
            View view;
            synchronized (this)
            {
                connected = true;
                // Take any views that happened before the connection into a local variable so we can un-sync.
                if (deferredView.isEmpty())
                    break;  // if there were none then break out of the loop
                else
                    view = deferredView.remove(0);  // or process any deferred views to make sure we are in synch
            }
            // If there was a view before the channel was connected, then process that view now.
            if (view != null)
                doAcceptView(view, true);
        }
    }

    public void channelDisconnected(Channel channel)
    {
        log.info("channelDisconnected() " + channel.getLocalAddress());
        synchronized (this)
        {
            connected = false;
        }
    }

    public void channelClosed(Channel channel)
    {
        log.info("channelClosed() " + channel.getLocalAddress());
        synchronized (this)
        {
            connected = false;
        }
    }

    public void channelShunned()
    {
        // Contribution from M. Henrikson
        log.info("channelShunned()");
        synchronized (this)
        {
            connected = false;
        }
    }

    public void channelReconnected(Address addr)
    {
        log.info("channelReconnected() " + addr);
        synchronized (this)
        {
            connected = true;
        }
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
        doAcceptView(newView, false);
    }

    private void doAcceptView(View newView, boolean deferred)
    {
        log.info("viewAccepted(" + newView + ") deferred=" + deferred);
        // Diff the views.
        ViewDiff diff;
        NodeAddress coord;
        synchronized (this)
        {
            if (!connected)
            {
                log.info("Not yet connected, deferring view for connect...");
                deferredView.add(newView);
                return;
            }
            diff = new ViewDiff(currentView, newView);
            if (diff.isCoordinatorChanged())
            {
                coord = new JGroupsAddress(diff.getCoordinator());
                log.info("*** Coordinator changed from " + coordinator + " to " + coord);
                coordinator = coord;
            }
            else
            {
                // Otherwise, the coordinator hasn't changed so get it out in to a local
                // so we don't have to sync again.
                coord = coordinator;
            }
            currentView = newView;
        } // synchronized

        // Notify about coordinator changes first.
        if (diff.isCoordinatorChanged())
            grid.onNewCoordinator(coord);

        // Create a set of JGroupsAddresses for the diff.
        Set joined = toNodeAddresses(diff.getJoined());
        Set left = toNodeAddresses(diff.getLeft());
        // Notify all the other components about the membership change.
        NodeAddress localAddress = grid.getLocalAddress();
        grid.onMembershipChange(joined, left, localAddress);
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

    NodeAddress getCoordinator()
    {
        synchronized (this)
        {
            return coordinator;
        }
    }
}
