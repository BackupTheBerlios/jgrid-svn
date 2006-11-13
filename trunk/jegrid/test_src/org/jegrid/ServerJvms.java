package org.jegrid;

import org.apache.log4j.Logger;
import org.jegrid.util.JavaProcess;
import org.jegrid.util.Util;

import java.io.IOException;

/**
 * TODO: Add class level javadoc
 * <br> User: jdavis
 * Date: Nov 12, 2006
 * Time: 10:16:28 AM
 */
public class ServerJvms
{
    private static Logger log = Logger.getLogger(ServerJvms.class);

    private String gridName;
    private int numberOfServers;
    private JavaProcess[] jvms;
    private Grid grid;
    private int numberOfThreads;

    public ServerJvms(Grid grid, int numberOfServers, int numberOfThreads)
    {
        this.grid = grid;
        this.gridName = grid.getGridName();
        this.numberOfServers = numberOfServers;
        this.numberOfThreads = numberOfThreads;
        jvms = new JavaProcess[numberOfServers];
        for (int i = 0; i < jvms.length; i++)
            jvms[i] = new JavaProcess(ServerMain.class.getName());
    }

    public void start() throws IOException, InterruptedException
    {
        for (int i = 0; i < jvms.length; i++)
        {
            JavaProcess jvm = jvms[i];
            jvm.setArgs(new String[] { gridName , Integer.toString(numberOfThreads)});
            jvm.start();
        }

        NodeAddress[] addresses;
        do
        {
            log.info("Waiting...");
            addresses = grid.getClient().waitForServers(-1);
            for (int i = 0; i < addresses.length; i++)
            {
                NodeAddress address = addresses[i];
                log.info("Server#" + (i+1) + " = " + address);
            }
            if (addresses.length < numberOfServers)
                Util.sleep(1000);
        } while (addresses.length < numberOfServers);
    }
    
    public void stop()
    {
        log.info("stop() : ENTER");
        for (int i = 0; i < jvms.length; i++)
        {
            JavaProcess jvm = jvms[i];
            jvm.kill();
        }
        log.info("stop() : LEAVE");
    }

    public void waitFor()
    {
        for (int i = 0; i < jvms.length; i++)
        {
            JavaProcess jvm = jvms[i];
            jvm.setArgs(new String[] { gridName });
            try
            {
                int rc = jvm.waitFor();
                log.info("rc = " + rc);
            }
            catch (InterruptedException e)
            {
                log.warn(e);
            }
        }
    }
}
