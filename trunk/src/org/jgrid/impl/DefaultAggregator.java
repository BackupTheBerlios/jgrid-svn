package org.jgrid.impl;

import org.jgrid.Aggregator;

import java.io.Serializable;

/**
 * The default aggregator.  Collects results into an array.
 * <br>User: Joshua Davis
 * Date: Feb 28, 2006
 * Time: 7:25:50 AM
 */
public class DefaultAggregator implements Aggregator
{
    private Serializable[] results;
    private boolean parallel;

    public void initialize(int size, boolean parallel)
    {
        results = new Serializable[size];
        this.parallel = parallel;
    }

    public void aggregate(Serializable output, int index)
    {
        results[index] = output;
    }

    public Object finish()
    {
        if (parallel)
            return results;
        else
            return results[0];
    }
}
