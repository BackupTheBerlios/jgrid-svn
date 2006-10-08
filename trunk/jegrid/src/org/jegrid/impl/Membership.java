package org.jegrid.impl;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.CondVar;

import java.util.*;

import org.jegrid.GridException;
import org.jegrid.NodeAddress;
import org.jegrid.NodeStatus;
import org.jegrid.GridStatus;
import org.apache.log4j.Logger;

/**
 * A delegate that manages grid membership info.
 * <br> User: jdavis
 * Date: Oct 7, 2006
 * Time: 9:01:21 AM
 */
class Membership implements GridStatus
{
    private static Logger log = Logger.getLogger(Membership.class);

    private static final long TIMEOUT_FOR_FIRST_MEMBERSHIP_CHANGE = 10000;
    
    private int numberOfMembershipChanges;
    private Mutex membershipMutex;
    private CondVar membershipChanged;
    private Map allNodesByAddress = new HashMap();
    private GridImpl grid;

    public Membership(GridImpl grid)
    {
        membershipMutex = new Mutex();
        membershipChanged = new CondVar(membershipMutex);
        this.grid = grid;
    }

    public void waitForMembershipChange(int mark, long timeout)
    {
        acquireMutex();
        try
        {
            unsyncWaitForMembershipChange(mark, timeout);
        }
        finally
        {
            releaseMutex();
        }
    }

    public void onMembershipChange(Set joined, Set left)
    {
        acquireMutex();
        try
        {
            log.info("--- NODE " + grid.getLocalAddress() + " MEMBERSHIP CHANGE #" + numberOfMembershipChanges + " ---");
            for (Iterator iterator = joined.iterator(); iterator.hasNext();)
            {
                NodeAddress address = (NodeAddress) iterator.next();
                if (allNodesByAddress.containsKey(address))
                {
                    log.info("Node list already contains " + address);
                }
                else
                {
                    NodeStatusImpl node = new NodeStatusImpl(address);
                    allNodesByAddress.put(address, node);
                    log.info("Node " + node + " added.");
                }
            } // for
            for (Iterator iterator = left.iterator(); iterator.hasNext();)
            {
                NodeAddress address = (NodeAddress) iterator.next();
                if (allNodesByAddress.containsKey(address))
                {
                    allNodesByAddress.remove(address);
                    log.info("Removed " + address);
                }
                else
                {
                    log.info("Address " + address + " not found.");
                }
            } // for
            numberOfMembershipChanges++;
            membershipChanged.signal(); // Signal on the condition that the membership has changed.
        }
        finally
        {
            releaseMutex();
        }
    }

    public Collection getNodeStatus()
    {
        acquireMutex();
        try
        {
            unsyncWaitForMembershipChange(1, TIMEOUT_FOR_FIRST_MEMBERSHIP_CHANGE);
            return Collections.unmodifiableCollection(allNodesByAddress.values());           
        }
        finally
        {
            releaseMutex();
        }
    }

    public int nextMembershipChange()
    {
        acquireMutex();
        try
        {
            return numberOfMembershipChanges + 1;           
        }
        finally
        {
            releaseMutex();
        }
    }

    private void releaseMutex()
    {
        membershipMutex.release();
    }

    private void unsyncWaitForMembershipChange(int mark, long timeout)
    {
        // If the condition hasn't been met, wait for it.
        // If it still hasn't been met after waiting, throw an exception.
        if (numberOfMembershipChanges < mark)
        {
            try
            {
                if (log.isDebugEnabled())
                    log.debug("Waiting for membership change...");
                membershipChanged.timedwait(timeout);
            }
            catch (InterruptedException e)
            {
                throw new GridException(e);
            }
            if (numberOfMembershipChanges < mark)
                throw new GridException("Timeout waiting for membership change!");
        }
    }

    private void acquireMutex()
    {
        try
        {
            membershipMutex.acquire();
        }
        catch (InterruptedException e)
        {
            throw new GridException(e);
        }
    }

    public void onHello(NodeStatus from)
    {
        acquireMutex();
        try
        {
            updateStatus(from);
        }
        finally
        {
            releaseMutex();
        }
    }

    public void refreshStatus(NodeStatus[] ns)
    {
        acquireMutex();
        try
        {
            for (int i = 0; i < ns.length; i++)
                updateStatus(ns[i]);
        }
        finally
        {
            releaseMutex();
        }
    }

    private void updateStatus(NodeStatus nodeStatus)
    {
        if (nodeStatus == null)
            return;
        NodeAddress address = nodeStatus.getNodeAddress();
        if (allNodesByAddress.containsKey(address))
            allNodesByAddress.put(address, nodeStatus);
        else
            log.warn("Status from non-member? " + nodeStatus);
    }


    public int getNumberOfNodes()
    {
        acquireMutex();
        try
        {
            return allNodesByAddress.size();
        }
        finally
        {
            releaseMutex();
        }
    }

    public Iterator iterator()
    {
        // Copy the node statuses into a list and use that for the iterator to avoid
        // concurrent modification exceptions, etc.
        List list = new LinkedList();
        acquireMutex();
        try
        {
            list.addAll(allNodesByAddress.values());
        }
        finally
        {
            releaseMutex();
        }
        return list.iterator();
    }
}
