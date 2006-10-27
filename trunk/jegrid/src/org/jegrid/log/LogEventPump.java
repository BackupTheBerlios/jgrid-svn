package org.jegrid.log;

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.jegrid.GridException;

/**
 * Pumps logging events from the bus into the local appenders asynchronously.
 * <br>
 * This avoids using the 'up' thread to process logging messages.
 * <br>User: Joshua Davis
 * Date: Oct 22, 2006
 * Time: 3:52:49 PM
 */
public class LogEventPump implements Runnable
{
    private Channel eventQueue;
    private Thread thread;

    public LogEventPump()
    {
        eventQueue = new LinkedQueue();
        thread = new Thread(this);
        // It is the user's responsibility to close appenders before
        // exiting.
        thread.setDaemon(true);
        // set the priority to lowest possible value
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setName("LogEventPump-" + thread.getName());
        thread.start();
    }

    /**
     * @noinspection EmptyCatchBlock
     */
    public void run()
    {
        Logger root = Logger.getRootLogger();
        LoggingEvent event;
        try
        {
            while ((event = (LoggingEvent) eventQueue.take()) != null)
            {
                root.callAppenders(event);
            }
        }
        catch (InterruptedException e)
        {
        }
    }

    public void append(LoggingEvent event)
    {
        try
        {
            eventQueue.put(event);
        }
        catch (InterruptedException e)
        {
            throw new GridException(e);
        }
    }
}
