package org.jgrid.impl;

import org.jgrid.GridEventListener;
import org.jgrid.GridBus;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Keeps track of all the GridBus listeners, and notifies them when asked.
 * <br>User: Joshua Davis
 * <br>Date: Nov 12, 2005 Time: 7:59:14 AM
 */
public class Notifier
{
    private List eventListeners;
    private GridBus bus;

    public Notifier(GridBus bus)
    {
        this.bus = bus;
        this.eventListeners = new ArrayList();
    }

    void notifyConnected()
    {
        for (Iterator iterator = eventListeners.iterator(); iterator.hasNext();)
        {
            GridEventListener gridEventListener = (GridEventListener) iterator.next();
            gridEventListener.connected(bus);
        }
    }

    void addEventListener(GridEventListener listener)
    {
        synchronized (this)
        {
            eventListeners.add(listener);
        }
    }

    void removeEventListener(GridEventListener listener)
    {
        synchronized (this)
        {
            eventListeners.remove(listener);
        }
    }

    void notifyDisconnect()
    {
        for (Iterator iterator = eventListeners.iterator(); iterator.hasNext();)
        {
            GridEventListener gridEventListener = (GridEventListener) iterator.next();
            gridEventListener.disconnected(bus);
        }
    }

    void notifyPeersChanged()
    {
        for (Iterator iterator = eventListeners.iterator(); iterator.hasNext();)
        {
            GridEventListener gridEventListener = (GridEventListener) iterator.next();
            gridEventListener.peersChanged(bus);
        }
    }

    void notifyPeersUpdated()
    {
        for (Iterator iterator = eventListeners.iterator(); iterator.hasNext();)
        {
            GridEventListener gridEventListener = (GridEventListener) iterator.next();
            gridEventListener.peersUpdated(bus);
        }
    }

}
