package org.jegrid;

/**
 * A typed exception indicating that there aren't any workers available to take the job.
 *
 * Better than checking a string in the exception message. This was introduced so that
 * code that calls Task.run() can retry in the event that there are no workers available.
 */
public class NoWorkersAvailableException extends GridException
{

    public NoWorkersAvailableException()
    {
        this("No workers are available");
    }

    public NoWorkersAvailableException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public NoWorkersAvailableException(String message)
    {
        super(message);
    }

    public NoWorkersAvailableException(Throwable e)
    {
        super(e);
    }
}
