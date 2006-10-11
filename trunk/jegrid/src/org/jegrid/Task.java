package org.jegrid;

import org.jegrid.jms.TaskRequest;

import java.io.Serializable;

/**
 * TODO: Add class level javadoc
 * <br> User: jdavis
 * Date: Oct 7, 2006
 * Time: 12:07:15 PM
 */
public interface Task
{
    int getTaskId()
            ;

    void addInput(Serializable input)
            ;

    void run(Aggregator aggregator, int maxWorkers)
            ;

    void acquire()
            ;

    void run(TaskRequest taskRequest)
            ;

    void release()
            ;
}
