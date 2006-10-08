package org.jegrid;

import java.util.Iterator;

/**
 * The status of the grid, including the status of each node.
 * <br>User: Joshua Davis
 * Date: Oct 8, 2006
 * Time: 9:45:44 AM
 */
public interface GridStatus
{
    int getNumberOfNodes();
    
    Iterator iterator();
}
