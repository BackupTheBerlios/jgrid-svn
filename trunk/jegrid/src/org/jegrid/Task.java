package org.jegrid;

import java.io.Serializable;

/**
 * The client-side interface for a task that will run on the grid.
 * A task has the following attributes:
 * <ol>
 * <li>An input processor class name - This class must implement InputProcessor.  It will be used
 * to process the input.</li>
 * <li>A list of inputs - These objects will form a queue that is fed to the InputProcessor on the grid for processing.</li>
 * <li>An aggregator - This object will aggregate the results of each input as they happen.</li>
 * <li>maxWorkers - The maximum number of servers that will be used to process the input.</li>
 * </ol>
 * The caller has the option of choosing to run the aggregation, queues and tracking either locally
 * via the run() method, or remotely using Client.background(TaskRequest).
 * <br> User: jdavis
 * Date: Oct 7, 2006
 * Time: 12:07:15 PM
 */
public interface Task
{
    /**
     * Returns the unique id for the task on the client.
     *
     * @return the unique id for the task on the client
     */
    TaskId getTaskId()
            ;

    /**
     * Add an input to the task.  This will be queued and processed in the order it was received.
     *
     * @param input an input to the task
     */
    void addInput(Serializable input)
            ;

    /**
     * Process all the inputs on the grid with the specified task class.
     *
     * @param inputProcessorClass The name of the task class, which implements InputProcessor and will be used by the workers
     *                            to process the input and produce the output.
     * @param aggregator          An object that will be used to aggregate all the results.  The calling thread on the client
     *                            will be used to invoke the methods on the aggregator.
     * @param maxWorkers          The maximum number of workers to be assigned to this task.
     * @param useLocalWorker      Set to true to use the local thread to process input.
     */
    void run(String inputProcessorClass, Aggregator aggregator, int maxWorkers, boolean useLocalWorker)
            ;

    /**
     * Assigns one worker to this task, blocking until a worker is available.
     */
    void acquire()
            ;

    /**
     * Process the inputs specfied in the task request, just like the other run method.
     *
     * @param taskRequest    Object containing the taskClassName, a list of input objects, the aggregator class name,
     * @param useLocalWorker Set to true to use the local thread to process input.
     */
    void run(TaskRequest taskRequest, boolean useLocalWorker)
            ;

    /**
     * Releases any workers that were assigned to this task.
     */
    void release()
            ;
}
