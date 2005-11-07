package org.jgrid.impl;

import org.jgrid.GridConfiguration;

/**
 * Base class for server and client aspects of a grid node.
 * <br>User: Joshua Davis
 * <br>Date: Oct 22, 2005 Time: 5:49:32 PM
 */
class GridComponent
{
    protected GridConfiguration config;
    protected GridBusImpl gridBus;

    public GridComponent(GridBusImpl gridBus)
    {
        this.gridBus = gridBus;
        config = gridBus.getConfig();
    }

    public GridComponent(GridConfiguration config, GridBusImpl gridBus)
    {
        this.config = config;
        this.gridBus = gridBus;
    }

    public GridConfiguration getConfig()
    {
        return config;
    }

    public GridBusImpl getGridBus()
    {
        return gridBus;
    }

    protected String getLocalAddress()
    {
        return gridBus.getLocalAddress();
    }    
}
