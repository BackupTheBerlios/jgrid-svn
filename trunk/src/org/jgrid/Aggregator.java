package org.jgrid;

import java.io.Serializable;

/**
 * Aggregates results from jobs executed on servers.
 * <br>User: Joshua Davis
 * Date: Feb 28, 2006
 * Time: 7:25:11 AM
 */
public interface Aggregator
{
    void aggregate(Serializable output, int index);

    Object finish();

    void initialize(int size, boolean parallel);
}
