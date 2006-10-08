package org.jegrid;

import java.io.Serializable;

/**
 * The input or output of a task along with it's input ID.
 * <br> User: jdavis
 * Date: Oct 7, 2006
 * Time: 10:28:25 AM
 */
public class TaskData implements Serializable
{
    private int inputId;
    private Serializable data;

    public TaskData(int inputId, Serializable inputData)
    {
        this.inputId = inputId;
        this.data = inputData;
    }

    public int getInputId()
    {
        return inputId;
    }

    public Serializable getData()
    {
        return data;
    }
}
