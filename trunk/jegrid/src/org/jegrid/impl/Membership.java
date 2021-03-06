package org.jegrid.impl;

import EDU.oswego.cs.dl.util.concurrent.CondVar;
import org.apache.log4j.Logger;
import org.jegrid.*;
import org.jegrid.util.Mutex;

import java.util.*;

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
    private static final long REFRESH_TIMEOUT = 3000;

    private int numberOfMembershipChanges;
    private Mutex membershipMutex;
    private CondVar membershipChanged;
    private CondVar serversNotFull;
    private Map allNodesByAddress = new HashMap();
    private Map unknownNodes = new HashMap();
    private Map serverNodes = new HashMap();
    private GridImpl grid;
    private long lastRefresh;

    public Membership(GridImpl grid)
    {
        membershipMutex = new Mutex();
        membershipChanged = new CondVar(membershipMutex);
        serversNotFull = new CondVar(membershipMutex);
        this.grid = grid;
    }

    public void waitForMembershipChange(int mark, long timeout)
    {
        membershipMutex.acquire();
        try
        {
            unsyncWaitForMembershipChange(mark, timeout);
        }
        finally
        {
            releaseMutex();
        }
    }

    public void onMembershipChange(Set joined, Set left, NodeAddress localAddress)
    {
        membershipMutex.acquire();
        try
        {
            log.info("--- NODE " + localAddress + " MEMBERSHIP CHANGE #" + numberOfMembershipChanges + " ---");
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
                    NodeStatus node = (address.equals(localAddress)) ?
                            grid.getLocalStatus() :
                            new NodeStatusImpl(address);
                    putNode(address, node, null);
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
        if (serverNodes.remove(address) != null)
        {
            log.debug("Removed server: " + address + " there are now " + serverNodes.size());
        }
    }

    private boolean nodeExists(NodeAddress address)
    {
        return allNodesByAddress.containsKey(address);
    }

    private void putNode(NodeAddress address, NodeStatus node, NodeStatus old)
    {
        allNodesByAddress.put(address, node);
        switch (node.getType())
        {
            case Grid.TYPE_UNKNOWN:
                unknownNodes.put(address, node);
                break;
            case Grid.TYPE_SERVER:
                serverNodes.put(address, node);
                // If the old status was 'no free threads' and the new is not
                // then notify anyone waiting on available threads.
                if (node.getAvailableWorkers() > 0)
                {
                    if (old == null)
                        serversNotFull.signal();
                    else if (old.getAvailableWorkers() == 0)
                        serversNotFull.signal();
                }
                break;
        }
    }

    public Collection getNodeStatus()
    {
        membershipMutex.acquire();
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
        membershipMutex.acquire();
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

    public void onNodeStatus(NodeStatus from)
    {
        membershipMutex.acquire();
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
        membershipMutex.acquire();
        try
        {
            lastRefresh = System.currentTimeMillis();
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
        if (address == null)
        {
            log.warn("Node status with no address? " + nodeStatus);
            throw new RuntimeException("Node status with no address? " + nodeStatus);
        }

        // Don't use the status for the local node.  If the
        // address is mine, then use my own status.
        NodeAddress localAddress = grid.getLocalAddress();
        boolean isLocal = address.equals(localAddress);
        NodeStatus node = isLocal ?
                grid.getLocalStatus() :
                nodeStatus;

        NodeStatus old = findNode(address);
        if (old != null || isLocal)
            putNode(address, node, old);
        else
        {
            putNode(address, node, null);
            log.info("Node " + node + " added. (membership change is " + numberOfMembershipChanges + ")");
        }
    }

    private NodeStatus findNode(NodeAddress address)
    {
        return (NodeStatus) allNodesByAddress.get(address);
    }


    public int getNumberOfNodes()
    {
        membershipMutex.acquire();
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
        membershipMutex.acquire();
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

    public int getNumberOfServers()
    {
        membershipMutex.acquire();
        try
        {
            return serverNodes.size();
        }
        finally
        {
            releaseMutex();
        }
    }

    public int getNumberOfUnknownNodes()
    {
        membershipMutex.acquire();
        try
        {
            return unknownNodes.size();
        }
        finally
        {
            releaseMutex();
        }
    }

    public boolean waitForServers(long timeout) throws InterruptedException
    {
        membershipMutex.acquire();
        try
        {
            for (Iterator iter = serverNodes.values().iterator(); iter.hasNext();)
            {
                NodeStatus n = (NodeStatus) iter.next();
                if (n.getAvailableWorkers() > 0)
                    return true;
            }
            // Wait for the 'not full' signal for a while.
            return serversNotFull.timedwait(timeout);
        }
        finally
        {
            releaseMutex();
        }
    }

    public boolean needsRefresh()
    {
        membershipMutex.acquire();
        try
        {
            if (unknownNodes.size() > 0)
                return true;
            if (System.currentTimeMillis() - lastRefresh > REFRESH_TIMEOUT)
                return true;
        }
        finally
        {
            releaseMutex();
        }
        return false;
    }

    public void onNodeStopped(NodeAddress addr)
    {
        membershipMutex.acquire();
        try
        {
            // Remove the node from all the maps.
            removeNode(addr);
            // Add the node as an 'unknown'.
            putNode(addr, new NodeStatusImpl(addr), null);
        }
        finally
        {
            releaseMutex();
        }
    }

    public boolean isMember(NodeAddress address)
    {
        membershipMutex.acquire();
        try
        {
            return allNodesByAddress.containsKey(address);
        }
        finally
        {
            releaseMutex();
        }
    }
}
