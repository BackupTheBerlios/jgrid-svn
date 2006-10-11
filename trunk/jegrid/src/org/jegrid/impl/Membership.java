package org.jegrid.impl;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.CondVar;

import java.util.*;

import org.jegrid.*;
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
    private Map unknownNodes = new HashMap();
    private GridImpl grid;
    private NodeAddress coordinator;

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
                if (nodeExists(address))
                {
                    log.info("Node list already contains " + address);
                }
                else
                {
                    // Don't make up a new status for the local node.  If the
                    // address is mine, then use my own status.
                    NodeStatus node = (address.equals(grid.getLocalAddress())) ?
                            grid.getLocalStatus() :
                            new NodeStatusImpl(address);
                    addNode(address, node);
                    log.info("Node " + node + " added.");
                }
            } // for
            for (Iterator iterator = left.iterator(); iterator.hasNext();)
            {
                NodeAddress address = (NodeAddress) iterator.next();
                if (nodeExists(address))
                {
                    removeNode(address);
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

    private void removeNode(NodeAddress address)
    {
        allNodesByAddress.remove(address);
        unknownNodes.remove(address);
        log.info("Removed " + address);
    }

    private boolean nodeExists(NodeAddress address)
    {
        return allNodesByAddress.containsKey(address);
    }

    private void addNode(NodeAddress address, NodeStatus node)
    {
        allNodesByAddress.put(address, node);
        switch (node.getType())
        {
            case Grid.TYPE_UNKNOWN:
                unknownNodes.put(address, node);
                break;
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

    public void onNodeStatus(NodeStatus from)
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

        // Don't use the status for the local node.  If the
        // address is mine, then use my own status.
        NodeStatus node = (address.equals(grid.getLocalAddress())) ?
                grid.getLocalStatus() :
                nodeStatus;

        NodeStatus old = findNode(address);
        if (old != null)
        {
//            if (old.getType() == Grid.TYPE_UNKNOWN)
//                log.info("Node type resolved: " + node);
            addNode(address, node);
        }
        else
            log.warn("Status from non-member? " + node);
    }

    private NodeStatus findNode(NodeAddress address)
    {
        return (NodeStatus) allNodesByAddress.get(address);
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

    public void onNewCoordinator(NodeAddress address)
    {
        log.info("=== NEW COORDINATOR: " + address + " ===");
        coordinator = address;
    }

    public NodeAddress getCoordinator()
    {
        return coordinator;
    }

    public int getNumberOfUnknownNodes()
    {
        acquireMutex();
        try
        {
            return unknownNodes.size();
        }
        finally
        {
            releaseMutex();
        }
    }
}
