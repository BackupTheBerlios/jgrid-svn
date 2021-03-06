package org.jegrid.impl;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.jegrid.*;

import java.io.Serializable;

/**
 * Common behavior and state for the local and 'server' workers.
 * <br>User: Joshua Davis
 * Date: Oct 22, 2006
 * Time: 8:40:41 AM
 */
abstract class AbstractInputProcessingWorker extends Worker implements TaskContext
{
    private static Logger log = Logger.getLogger(AbstractInputProcessingWorker.class);
    protected final TaskId id;
    private Latch goLatch;
    private Exception exception;
    private static final long GO_TIMEOUT = 10000;
    private boolean released;
    private String inputProcessorClassName;
    private Serializable sharedInput;

    public AbstractInputProcessingWorker(GridImplementor grid, TaskId id)
    {
        super(grid);
        this.id = id;
        this.goLatch = new Latch();
    }

    protected void processInput()
            throws Exception
    {
        if (inputProcessorClassName == null)
            throw new GridException("No task class name!");
        if (log.isDebugEnabled())
            log.debug("Worker started on " + id);
        // Priming read.
        TaskData input = nextInput(null);
        if (shouldRun(input))
        {
            // Create the task instance and run until there isn't any more input.
            InputProcessor inputProcessor = instantiateInputProcessor();
            try
            {
                loop(input, inputProcessor);
            }
            finally
            {
                if (inputProcessor instanceof LifecycleAware)
                {
                    LifecycleAware lifecycleAware = (LifecycleAware) inputProcessor;
                    lifecycleAware.terminate();
                }
            }
        }
    }

    private void loop(TaskData input, InputProcessor inputProcessor)
            throws Exception
    {
        while (shouldRun(input))
        {
            int inputId = input.getInputId();
            Serializable data;
            NDC.push("#" + inputId);
            try
            {
                data = inputProcessor.processInput(inputId, input.getData());
            }
            finally
            {
                NDC.pop();
            }
            TaskData output = new TaskData(inputId, data);
            input = nextInput(output);
        }
    }

    public String getInputProcessorClassName()
    {
        return inputProcessorClassName;
    }

    protected InputProcessor instantiateInputProcessor()
            throws IllegalAccessException, InstantiationException, ClassNotFoundException
    {

        return (InputProcessor) grid.instantiateObject(getInputProcessorClassName());
    }

    private boolean shouldRun(TaskData input) throws Exception
    {
        Exception ex = getException();
        if (ex != null)
            throw ex;
        return input != null;
    }

    protected abstract void handleException(GridException ge)
            ;

    protected abstract TaskData nextInput(TaskData output)
            throws RpcTimeoutException, InterruptedException
            ;

    public synchronized void setReleased(boolean flag)
    {
        released = flag;
    }

    public synchronized boolean isReleased()
    {
        return released;
    }

    public synchronized Exception getException()
    {
        return exception;
    }

    public void run()
    {
        log.debug("run() : ENTER");
        pushLoggingContext();
        try
        {
            // Get the next input from the client's queue of inputs for the task.
            // Wait for the client to say go.
            boolean okay = goLatch.attempt(GO_TIMEOUT);
            if (!okay)
                throw new GridException("Timeout waiting for 'go' from client.");
            // Paranoid checking.
            if (id.getClient() == null)
                throw new GridException("No client address!");
            processInput();
        }
        catch (GridException e)
        {
            handleException(e);
        }
        catch (Throwable t)
        {
            handleException(new GridException(t));
        }
        finally
        {
            popLoggingContext();
            try
            {
                done();
            }
            catch (InterruptedException e)
            {
                log.warn("Unexpected: " + e, e);
            }
        }
        log.debug("run() : LEAVE");
    }

    protected void popLoggingContext()
    {
        NDC.pop();
    }

    protected void pushLoggingContext()
    {
        NDC.push("[" + id.toString() + "]");
    }

    protected abstract void done() throws InterruptedException
            ;

    public synchronized void setException(Exception exception)
    {
        this.exception = exception;
    }

    public void go(GoMessage goMessage)
    {
        if (!goMessage.getTaskId().equals(this.id))
            setException(new IllegalStateException("Go task " + goMessage + " is not the same as assigned task " + this.id));
        this.inputProcessorClassName = goMessage.getProcessorClassName();
        this.sharedInput = goMessage.getSharedInput();
        goLatch.release();
    }


    public TaskId getTaskId()
    {
        return id;
    }

    public Object getSharedInput()
    {
        return sharedInput;
    }

    public Client getClient()
    {
        return grid.getClient();
    }
}
