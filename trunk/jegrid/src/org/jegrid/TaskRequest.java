package org.jegrid;

import java.io.Serializable;
import java.util.List;

/**
 * A complete task request as a serializable object.  Used for remotely executing the entire
 * task or for putting tasks on JMS queues, etc.
 * <br>User: Joshua Davis
 * Date: Oct 8, 2006
 * Time: 4:17:15 PM
 */
public class TaskRequest implements Serializable
{
    private String inputProcessorClassName;
    private String aggregatorClassName;
    private int maxWorkers;
    private List input;

    public TaskRequest(String inputProcessorClassName, String aggregatorClassName, int maxWorkers, List input)
    {
        this.inputProcessorClassName = inputProcessorClassName;
        this.aggregatorClassName = aggregatorClassName;
        this.maxWorkers = maxWorkers;
        this.input = input;
    }

    public String getInputProcessorClassName()
    {
        return inputProcessorClassName;
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
