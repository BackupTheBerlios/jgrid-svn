/* Created on Feb 10, 2006 by agautam */
package org.jgrid.test;

import java.io.IOException;

import org.jgrid.GridBus;
import org.jgrid.GridConfiguration;
import org.jgrid.util.JavaProcess;
import org.jgroups.util.Util;


/**
 * Common methods required to setup grid. Mostly extracted from GridTest.
 *
 * @author agautam
 */
public class GridSetupHelper
{

    public static GridBus connect(GridConfiguration gridConfig)
    {
        GridBus gridBus = gridConfig.getGridBus();
        gridBus.connect();
        return gridBus;
    }

    public static JavaProcess[] startServers(int serverCount) throws IOException
    {
        // Start a few JVMS.
        JavaProcess[] p = new JavaProcess[serverCount];
        for (int i = 0; i < p.length; i++)
        {
            p[i] = new JavaProcess("org.jgrid.ServerMain");
            p[i].start();
        }
        return p;
    }

    public static void stopServers(GridBus gridBus, JavaProcess[] p)
    {
        System.out.println("##### STOP #####");
        gridBus.broadcastStop();
        System.out.println("##### Waiting for processes #####");
        // Start a thread that will interrupt in a few seconds.
        final Thread interrupted = Thread.currentThread();
        Thread interrupter = new Thread(new Runnable() {
            public void run()
            {
                Util.sleep(10000);
                System.out.println("Interrupting...");
                interrupted.interrupt();
            }
        });
        interrupter.setDaemon(true);
        interrupter.start();
        boolean[] stopped = new boolean[p.length];
        for (int i = 0; i < stopped.length; i++)
            stopped[i] = false;

        for (int i = 0; i < p.length; i++)
        {
            JavaProcess javaProcess = p[i];
            try
            {
                javaProcess.waitFor();
            }
            catch (InterruptedException e)
            {
                System.out.println("Interrupted while waiting for processes.");
                break;
            }
            stopped[i] = true;
        }

        for (int i = 0; i < p.length; i++)
        {
            if (stopped[i])
                continue;
            JavaProcess javaProcess = p[i];
            System.out.println("Killing " + javaProcess + ", it didn't stop by itself.");
            javaProcess.kill();
        }

        System.out.println("##### GRID STOPPED #####");
    }
}
