package org.jegrid.impl;

/**
 * TODO: Add class level javadoc.
 * <br>User: Joshua Davis
 * Date: Oct 21, 2006
 * Time: 8:59:58 PM
 */
abstract class Worker implements Runnable
{
    protected final ServerImpl server;

    public Worker(ServerImpl server)
    {
        this.server = server;
    }
}
