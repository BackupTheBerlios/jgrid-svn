package org.jegrid.log;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.jegrid.TaskId;
import org.jegrid.impl.Bus;
import org.jegrid.impl.RpcTimeoutException;

/**
 * A Log4J Appender that sends logging events back to the client.
 * <br>This has to use a separate thread to send the messages because otherwise the logging events that happend while
 * the event is being sent over the bus will cause an infinite loop.  Fortunately, Log4J has
 * the handy AsyncAppender class.
 * <br>User: Joshua Davis
 * Date: Oct 22, 2006
 * Time: 3:07:08 PM
 */
public class GridAppender extends AsyncAppender implements Appender
{
    private static Logger log = Logger.getLogger(GridAppender.class);
    private Bus bus;
    private TaskId id;

    public GridAppender(Bus bus, final TaskId id)
    {
        this.bus = bus;
        this.id = id;
        addFilter(new GridFilter(id));
        addAppender(new BusAppender());
    }

    private static class GridFilter extends Filter
    {
        private final TaskId id;

        public GridFilter(TaskId id)
        {
            this.id = id;
        }

        public int decide(LoggingEvent event)
        {
            String ndc = event.getNDC();
            if (ndc == null || ndc.indexOf(id.toString()) < 0)
                return DENY;
            else
            {
                String name = event.getLoggerName();
                if (name == null)
                    return DENY;
                else if (name.indexOf("org.jgroups") > 0)
                    return DENY;
                else if (name.indexOf("org.jegrid.jgroups") > 0)
                    return DENY;
                else if (name.indexOf("org.jegrid.impl") > 0)
                    return DENY;
                return NEUTRAL;
            }
        }
    }

    /**
     * The asynchronous underlying appender that sends the events back to the client over the bus.
     */
    private class BusAppender extends AppenderSkeleton
    {
        protected void append(LoggingEvent event)
        {
            try
            {
                bus.apppend(id, event);
            }
            catch (RpcTimeoutException e)
            {
                log.error(e, e);
            }
        }

        public boolean requiresLayout()
        {
            return false;
        }

        public void close()
        {
        }
    }
}
