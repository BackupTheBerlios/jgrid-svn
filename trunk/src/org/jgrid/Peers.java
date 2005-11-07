// $Id:                                                                    $
package org.jgrid;

import java.util.Iterator;
import java.util.List;


/**
 * TODO: Add class javadoc
 *
 * @author josh Jan 19, 2005 7:39:22 AM
 */
public interface Peers
{
    /**
     * Returns the number of nodes in the grid, not including the local node.
     * @return the number of nodes in the grid, not including the local node.
     */
    int size();

    void waitForView();

    /**
     * Wait until the number or other nodes in the grid reaches 'size' or greater.
     * @param size the desired number of peers (not including the local node).
     */
    void waitForPeers(int size);

    List getPeerInfoList();
}
