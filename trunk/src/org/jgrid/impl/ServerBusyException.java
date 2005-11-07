// $Id:                                                                    $
package org.jgrid.impl;

/**
 * Thrown when a server's thread pool is busy.
 * @author josh Jan 5, 2005 7:01:51 AM
 */
public class ServerBusyException extends RuntimeException
{
    public ServerBusyException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ServerBusyException(String message)
    {
        super(message);
    }

    public ServerBusyException(Exception e)
    {
        super(e);
    }
}
