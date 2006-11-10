package org.jegrid;

/**
 * Used for testing org.jegrid.util.JavaProcess.
 * <br>User: jdavis
 * Date: Nov 10, 2006
 * Time: 9:31:04 AM
 */
public class TestMain
{
    public static void main(String[] args)
    {
        try
        {
            System.out.println("Hello.");
            long sleeptime = -1;
            if (args.length >= 1)
            {
                sleeptime = Long.parseLong(args[0]);
            }

            if (sleeptime > 0)
            {
                System.out.println("Going to sleep for " + sleeptime + "ms ...");
                Thread.sleep(sleeptime);
            }
            System.out.println("Goodbye.");
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }
}
