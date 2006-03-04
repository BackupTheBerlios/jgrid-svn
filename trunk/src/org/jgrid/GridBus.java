// $Id:                                                                    $
package org.jgrid;

/**
 * The connectivity for the grid.   Peers provides federation information about the group of nodes.
 * @author josh Jan 19, 2005 7:27:19 AM
 */
public interface GridBus
{
    String getLocalAddress();

    void connect();

    void disconnect();

    void broadcastStop();

    Peers getPeers();

    boolean isRunning();

    void addEventListener(GridEventListener listener);

    void removeEventListener(GridEventListener listener);

    GridConfiguration getConfig();

    void startServer();

    void setChannel(Object jgroupsChannel);

    Object getChannel();
}
