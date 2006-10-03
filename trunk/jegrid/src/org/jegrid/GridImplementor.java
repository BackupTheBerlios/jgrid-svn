package org.jegrid;

import java.util.Set;

/**
 * Internal interface for the main grid session.
 * <br>User: jdavis
 * Date: Sep 30, 2006
 * Time: 7:35:11 AM
 */
public interface GridImplementor extends Grid
{
    // Callback methods for grid membership invoked by the bus listener.
    void onMembershipChange(Set joined, Set left);
}
