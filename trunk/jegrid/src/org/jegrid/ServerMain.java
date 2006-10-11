package org.jegrid;

/**
 * Starts a server with the default networking config.
 * If you are using an IPv4 network on Linux, don't forget to set -Djava.net.preferIPv4Stack=true
 * <br>User: Joshua Davis
 * Date: Oct 8, 2006
 * Time: 9:09:58 AM
 */
public class ServerMain
{
    public static void main(String[] args)
    {
        try
        {
            GridConfiguration config = new GridConfiguration();
            config.setGridName(args[0]);
            config.setType(Grid.TYPE_SERVER);
            Grid grid = config.configure();
            grid.runServer();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
