// $Id:                                                                    $
package org.jgrid.impl;

import org.apache.log4j.Logger;
import org.jgroups.Address;
import org.jgroups.MembershipListener;
import org.jgroups.View;
import org.jgrid.Peers;
import org.jgrid.PeerInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.Collection;

/**
 * A set of peers dynamically updated by view notifications from the JGroups channel.
 * @author josh Jan 14, 2005 8:08:41 AM
 */
public class PeersImpl extends GridComponent implements MembershipListener, Peers
{
    private static final Logger log = Logger.getLogger(PeersImpl.class);
    private View view;
    private Vector everyoneButMe;
    private int viewsAccepted = 0;

    private static final int VIEW_WAIT_INTERVAL = 5000;

    public PeersImpl(GridBusImpl gridBusImpl)
    {
        super(gridBusImpl);
    }

    public void viewAccepted(View view)
    {
        String msg = "New view accepted";
        acceptView(msg, view);
    }

    private void acceptView(String msg, View view)
    {
        synchronized (this)
        {
            if (log.isDebugEnabled())
                log.debug("acceptView() : " + msg + " view id = " + view.getVid() + " view size = " + view.size());
            Vector newMembers = view.getMembers();
            if (newMembers != null)
            {
                this.view = view;
                Vector members = view.getMembers();
                everyoneButMe = new Vector();
                for (Iterator iterator = members.iterator(); iterator.hasNext();)
                {
                    Address address = (Address) iterator.next();
                    if (!address.equals(gridBus.getMyAddress()))
                        everyoneButMe.add(address);
                }
            }
            viewsAccepted++;
            this.notify();
        }
        if (gridBus != null)
            gridBus.notifyPeersChanged();
    }

    private String getLocalAdress()
    {
        return gridBus == null ? "(no address)" : gridBus.getLocalAddress();
    }

    public int size()
    {
        return view == null ? 0 : view.size() - 1;
    }

    public void waitForView()
    {
        synchronized (this)
        {
            if (log.isDebugEnabled())
                log.debug("waitForView() : waiting...");
            int current = viewsAccepted;
            // Wait until we're no longer on the same view.
            while (current == viewsAccepted && gridBus != null)
            {
                try
                {
                    this.wait(VIEW_WAIT_INTERVAL);
                }
                catch (InterruptedException e)
                {
                }
            }
            if (gridBus == null)
                log.info("Disconnected while waiting for view.");
        }
    }

    public void waitForPeers(int size)
    {
        int currentSize = 0;
        while (currentSize < size)
        {
            waitForView();
            currentSize = size();
            if (log.isDebugEnabled())
                log.debug("waitForPeers() : current size " + currentSize);
        }
    }

    public View getView()
    {
        return view;
    }

    public List getPeerInfoList()
    {
        if (view == null || gridBus == null)
            return new ArrayList(0);
        List members = view.getMembers();
        List list = new ArrayList(members.size());
        GridStateImpl state = getGridState();
        int i = 0;
        for (Iterator iterator = members.iterator(); iterator.hasNext(); i++)
        {
            Address member = (Address) iterator.next();
            NodeStateImpl nodeState = getNodeState(i,member,state);
            list.add(nodeState);
        }
        return list;
    }

    private GridStateImpl getGridState()
    {
        return gridBus.getGridState();
    }

    public Collection getAllPeers()
    {
        GridStateImpl state = getGridState();
        if (state == null)
            return null;
        else
            return state.getAllNodes();
    }
    
    private NodeStateImpl getNodeState(int i, Address member, GridStateImpl state)
    {
        if (state == null)
        {
            NodeStateImpl nodeState = new NodeStateImpl(member, "(unknown)");
            nodeState.setStatus(PeerInfo.STATUS_UNKNOWN);
            return nodeState;
        }
        else
        {
            NodeStateImpl nodeState = state.getNodeState(member);
            if (nodeState == null)  // Node state not in the grid state?
                nodeState = new NodeStateImpl(member, "(unknown)");
            else
            {
                // The node state was found in the current grid state.
                if (i == 0 && nodeState.getStatus() != PeerInfo.STATUS_SUSPECT)
                    nodeState.setStatus(PeerInfo.STATUS_COORDINATOR);
                else
                {
                    if (member.equals(gridBus.getMyAddress()))
                        nodeState.setStatus(PeerInfo.STATUS_SELF);
                    else
                        nodeState.setStatus(PeerInfo.STATUS_OK);
                }
            }
            return nodeState;
        }
    }

    public void suspect(Address address)
    {
        if (gridBus == null)
            return;
        if (log.isDebugEnabled())
            log.debug("suspect() : " + address);
        NodeStateImpl state = gridBus.getGridState().getNodeState(address);
        state.setStatus(PeerInfo.STATUS_SUSPECT);
        gridBus.notifyPeersChanged();
    }

    public void block()
    {
        log.info(getLocalAdress() + " ** Block ** ");
    }

    void disconnect()
    {
        view = null;
        viewsAccepted = 0;
        log.info(getLocalAdress() + " ** Stopped ** ");
    }

    void setInitialView(View view)
    {
        acceptView("Initial view", view);
    }

    public Vector everyoneButMe()
    {
        synchronized (this)
        {
            return everyoneButMe;
        }
    }
}
