package org.jegrid;

/**
 * Aggregates the results when tasks process input in parallel.
 * <br>User: Joshua Davis
 * Date: Oct 8, 2006
 * Time: 10:36:29 AM
 */
public interface Aggregator
{
    void aggregate(TaskData output);
}
