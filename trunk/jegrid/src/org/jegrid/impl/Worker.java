package org.jegrid.impl;

/**
 * TODO: Add class level javadoc.
 * <br>User: Joshua Davis
 * Date: Oct 21, 2006
 * Time: 8:59:58 PM
 */
abstract class Worker implements Runnable
{
    protected final GridImplementor grid;

    public Worker(GridImplementor grid)
    {
        this.grid = grid;
        if (grid == null)
            throw new IllegalArgumentException("No grid?");
    }

    public abstract void run();    

}
