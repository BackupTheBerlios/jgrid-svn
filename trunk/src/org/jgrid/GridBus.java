// $Id:                                                                    $
package org.jgrid;

/**
 * TODO: Add class javadoc
 *
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
}
