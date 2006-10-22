package org.jegrid.impl;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.jegrid.GridException;
import org.jegrid.InputProcessor;
import org.jegrid.TaskData;
import org.jegrid.TaskId;

/**
 * A runnable that performs the work on a server.  It instantiates the task class and begins processing input from the
 * client using the task's processInput() method.  The output is then sent back to the client.  This is repeated until
 * there is no more input from the client, or there was some sort of error.
 * <br> User: jdavis
 * Date: Oct 7, 2006
 * Time: 10:10:32 AM
 */
public class InputProcessingWorker extends AbstractInputProcessingWorker
{
    private static Logger log = Logger.getLogger(InputProcessingWorker.class);

    private Bus bus;
    private Appender appender;

    public InputProcessingWorker(ServerImpl server, TaskId task, Bus bus)
    {
        super(server, task);
        this.bus = bus;
    }

    protected void done()
    {
        server.done(id);
    }

    protected InputProcessor instantiateInputProcessor()
            throws IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        return server.instantiateInputProcessor(getInputProcessorClassName());
    }

    protected void handleException(GridException ge)
    {
        //noinspection EmptyCatchBlock
        try
        {
            bus.taskFailed(id, ge);
        }
        catch (Exception ignore)
        {
        }
    }


    protected void pushLoggingContext()
    {
        super.pushLoggingContext();
        // Add a new appender to the root category that sends messages to the client.
        if (appender == null)
        {
            appender = new GridAppender(bus, id);
        }
        Logger.getRootLogger().addAppender(appender);
    }


    protected void popLoggingContext()
    {
        // Remove the appender
        Logger.getRootLogger().removeAppender(appender);
        super.popLoggingContext();
    }

    protected TaskData nextInput(TaskData output)
            throws RpcTimeoutException
    {
        if (isReleased())
        {
            if (log.isDebugEnabled())
                log.debug("InputProcessingWorker released from " + id);
            return null;
        }
        return bus.getNextInput(id, output);
    }


}
