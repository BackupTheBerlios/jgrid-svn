package org.jegrid.impl;

/**
 * Thrown when a worker times out trying to get input.
 * <br>User: Joshua Davis
 * Date: Oct 9, 2006
 * Time: 6:13:34 PM
 */
public class RpcTimeoutException extends Exception
{

    public RpcTimeoutException()
    {
        super();
    }

    public RpcTimeoutException(String message)
    {
        super(message);
    }

    public RpcTimeoutException(Throwable cause)
    {
        super(cause);
    }

    public RpcTimeoutException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
