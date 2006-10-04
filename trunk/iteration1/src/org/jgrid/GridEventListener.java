package org.jgrid;

import org.jgrid.GridBus;

/**
 * Implement this to receive notifications regarding events in the grid.
 * <br>User: Joshua Davis
 * <br>Date: Oct 2, 2005 Time: 8:41:37 AM
 */
public interface GridEventListener
{
    void connected(GridBus bus);

    void disconnected(GridBus bus);

    void peersChanged(GridBus bus);

    void peersUpdated(GridBus gridBus);
}
