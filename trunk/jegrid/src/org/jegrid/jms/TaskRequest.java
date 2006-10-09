package org.jegrid.jms;

import java.io.Serializable;
import java.util.List;

/**
 * A JMS task request
 * <br>User: Joshua Davis
 * Date: Oct 8, 2006
 * Time: 4:17:15 PM
 */
public class TaskRequest implements Serializable
{
    private String taskClassName;
    private String aggregatorClassName;
    private int maxWorkers;
    private List input;

    public TaskRequest(String taskClassName, String aggregatorClassName, int maxWorkers, List input)
    {
        this.taskClassName = taskClassName;
        this.aggregatorClassName = aggregatorClassName;
        this.maxWorkers = maxWorkers;
        this.input = input;
    }

    public String getTaskClassName()
    {
        return taskClassName;
    }

    public List getInput()
    {
        return input;
    }

    public String getAggregatorClassName()
    {
        return aggregatorClassName;
    }

    public int getMaxWorkers()
    {
        return maxWorkers;
    }
}
