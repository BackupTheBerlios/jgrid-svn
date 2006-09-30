package org.jegrid.impl;

import org.jegrid.*;
import org.jegrid.util.MicroContainer;

/**
 * TODO: Add class level javadoc.
 * <br>User: jdavis
 * Date: Sep 21, 2006
 * Time: 5:34:18 PM
 */
public class GridImpl implements GridImplementor
{
    private GridConfiguration config;
    private MicroContainer mc;

    public GridImpl(GridConfiguration config, MicroContainer mc)
    {
        this.config = config;
        this.mc = mc;
    }

    public Client getClient()
    {
        Client client = (Client) mc.getComponentInstance(Client.class);
        if (client == null)
            throw new GridException("This configuration is not a client.");
        return client;
    }

    public Server getServer()
    {
        Server server = (Server) mc.getComponentInstance(Server.class);
        if (server == null)
            throw new GridException("This configuration is not a server");
        return server;
    }

    private Bus getBus()
    {
        return (Bus)mc.getComponentInstance(Bus.class);
    }
    public void connect()
    {
        getBus().connect();
    }

    public void disconnect()
    {
        getBus().disconnect();
    }
}
