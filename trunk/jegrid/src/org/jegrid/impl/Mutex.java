package org.jegrid.impl;

import org.jegrid.GridException;

/**
 * Wrapper around the Mutex in concurrent.jar
 * <br>User: Joshua Davis
 * Date: Oct 27, 2006
 * Time: 7:04:51 AM
 */
public class Mutex extends EDU.oswego.cs.dl.util.concurrent.Mutex
{
    public void acquire()
    {
        try
        {
            super.acquire();
        }
        catch (InterruptedException e)
        {
            throw new GridException(e);
        }
    }
}
