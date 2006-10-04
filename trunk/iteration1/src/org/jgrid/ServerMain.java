package org.jgrid;

import org.apache.log4j.Logger;
import org.jgrid.GridConfiguration;
import org.jgrid.Server;

/**
 * Main program that starts a server daemon process.
 * <br>User: Joshua Davis
 * <br>Date: Oct 9, 2005 Time: 9:33:16 AM
 */
public class ServerMain
{
    private static Logger log = Logger.getLogger(ServerMain.class);

    public static void main(String[] args)
    {
        log.info("Hello.");
        GridConfiguration config = new GridConfiguration();
        Server server = config.getServer();
        server.run();
        log.info("Goodbye.");
    }
}
