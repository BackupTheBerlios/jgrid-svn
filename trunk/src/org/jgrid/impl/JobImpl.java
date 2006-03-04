package org.jgrid.impl;

import org.jgrid.GridException;
import org.jgrid.Job;
import org.jgrid.Aggregator;
import org.jgrid.Service;
import org.jgroups.util.Promise;
import org.jgroups.TimeoutException;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.io.IOException;
import java.util.*;

/**
 * Client side view of a job.
 * <br>User: Joshua Davis
 * <br>Date: Oct 22, 2005 Time: 4:24:17 PM
 */
class JobImpl implements Job
{
    private static Logger log = Logger.getLogger(JobImpl.class);

    private int status;
    private Class serviceClass;
    private ClientSessionImpl clientSession;
    private Aggregator aggregator;
    private final Set waitingFor;
    private Promise resultPromise;

    public JobImpl(ClientSessionImpl clientSession, Class serviceClass)
    {
        if (!Service.class.isAssignableFrom(serviceClass))
            throw new GridException("Class " + serviceClass.getName() + " does not implement " + Service.class.getName());
        this.serviceClass = serviceClass;
        this.clientSession = clientSession;
        aggregator = new DefaultAggregator();
        status = STATUS_CREATED;
        waitingFor = new HashSet();
        resultPromise = new Promise();
    }

    public void start(Serializable input)
    {
        doStart(Arrays.asList(new Object[] { input }), false);
    }

    public Serializable join(long timeout)
    {
        if (status == STATUS_CREATED)
            throw new IllegalStateException("Job has not been started.  Invoke start() first.");
        Serializable result;
        try
        {
            result = (Serializable) resultPromise.getResultWithTimeout(timeout);
        }
        catch (TimeoutException e)
        {
            throw new GridException(e);
        }
        // Back to the initial state.
        status = STATUS_CREATED;
        return result;
    }

    public void startParallel(List inputList)
    {
        doStart(inputList, true);
    }

    private void doStart(List inputList, boolean parallel)
    {
        if (status == STATUS_FINISHED)
            throw new GridException("The job request executed, but the result has not been taken yet.");
        if (status == STATUS_STARTED)
            throw new GridException("The job request is running already.");
        int size = inputList.size();
        if (!parallel && size > 1)
            throw new GridException("A non-parallel job cannot have multiple inputs!");
        status = STATUS_STARTED;
        aggregator.initialize(size, parallel);
        int index = 0;
        for (Iterator iterator = inputList.iterator(); iterator.hasNext();)
        {
            Serializable input = (Serializable) iterator.next();
            addInput(serviceClass, input, index);
            index++;
        }
    }

    private RequestState addInput(Class serviceClass, Serializable input, int index)
    {
        if (log.isDebugEnabled())
           log.debug("addInput() : " + serviceClass.getName() + ", " + input + ", " + index);
        RequestState state;
        try
        {
            JobRequest request = new JobRequest(
                    clientSession.nextRequestId(),
                    serviceClass.getName(),
                    input
            );
            state = new RequestState(this, request, index);
            synchronized (waitingFor)
            {
                waitingFor.add(state.getRequestId());
            }
            clientSession.putRequest(state);        // Enqueue the request.
        }
        catch (IOException e)
        {
            throw new GridException("Unable to create job request due to : " + e, e);
        }
        catch (InterruptedException e)
        {
            throw new GridException("Unable to create job request due to : " + e, e);
        }
        return state;
    }

    public void onCompletion(RequestState requestState)
    {
        boolean finished;
        synchronized (waitingFor)
        {
            if (!waitingFor.remove(requestState.getRequestId()))
                throw new GridException("Job was not waiting for " + requestState.getRequestId());
            finished = waitingFor.size() == 0;
        }

        try
        {
            Serializable output = requestState.getOutput();
            aggregator.aggregate(output, requestState.getIndex());
        }
        catch (IOException e)
        {
            log.error(e,e);
            throw new GridException(e);
        }
        catch (ClassNotFoundException e)
        {
            log.error(e,e);
            throw new GridException(e);
        }
        finally
        {
            requestState.clearOutput();
        }

        if (finished)
        {
            log.info("Finished, calling aggregator...");
            status = STATUS_FINISHED;
            Object result = aggregator.finish();
            resultPromise.setResult(result);
        }
    }
}
