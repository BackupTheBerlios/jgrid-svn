package org.jgroups.blocks;

/**
 * Thorwn by the modified RpcDispatcher when there is a marshalling problem.
 * <br>User: jdavis
 * Date: Nov 6, 2006
 * Time: 10:39:55 AM
 */
public class MarshallingException extends Exception
{
    public MarshallingException(Throwable cause)
    {
        super(cause);
    }
}
