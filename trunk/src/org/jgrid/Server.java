// $Id:                                                                    $
package org.jgrid;

import org.jgroups.Message;

/**
 * TODO: Add class javadoc
 *
 * @author josh Jan 29, 2005 10:24:30 AM
 */
public interface Server extends Runnable
{
    void run();

    int getFreeThreads();

}
