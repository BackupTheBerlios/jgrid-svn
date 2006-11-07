package org.jegrid;

/**
 * Aggregates the outputs produced by the InputProcessor for a task.
 * <br>User: Joshua Davis
 * Date: Oct 8, 2006
 * Time: 10:36:29 AM
 */
public interface Aggregator
{
    void aggregate(TaskData output);

    void done()
            ;
}
