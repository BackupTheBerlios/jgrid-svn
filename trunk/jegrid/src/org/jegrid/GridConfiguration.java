package org.jegrid;

import org.w3c.dom.Element;

/**
 * Holds configuration properties for JEGrid.<br>
 * <br>User: jdavis
 * Date: Sep 21, 2006
 * Time: 5:15:46 PM
 */
public class GridConfiguration
{
    private String gridName;

    public Element getBusConfiguration()
    {
        return null;
    }

    /**
     * Returns the name of the grid.  All nodes on the same network with the same grid name will federate
     * into the same grid.
     * @return the name of the grid
     */
    public String getGridName()
    {
        return gridName;
    }
}
