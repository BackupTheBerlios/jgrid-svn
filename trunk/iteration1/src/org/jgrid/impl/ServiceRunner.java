package org.jgrid.impl;

import org.apache.log4j.Logger;
import org.jgrid.Service;
import org.jgroups.Address;
import org.jgroups.Message;

import java.io.Serializable;
import java.io.IOException;

/**
 * Executes jobs (service class + input).
 * <br>User: Joshua Davis
 * <br>Date: Oct 22, 2005 Time: 3:34:43 PM
 */
public class ServiceRunner implements Runnable
{
    private static Logger log = Logger.getLogger(ServiceRunner.class);

    private ServerImpl server;
    private JobRequest req;
    private Address replyTo;

    public ServiceRunner(ServerImpl server, JobRequest req)
    {
        this.server = server;
        this.req = req;
    }

    public void setRequestMessage(Message message)
    {
        replyTo = message.getSrc();
    }

    public void run()
    {
        Thread currentThread = Thread.currentThread();
        ClassLoader originalClassLoader = currentThread.getContextClassLoader();
        String serviceClassName = req.getServiceClassName();
        JobResponse response = new JobResponse(req.getRequestId(),serviceClassName);
        try
        {
            log.info("run() : ENTER");
            // Get the class loader for the service class, and set the thread's current class loader.
            ClassLoader classLoader = server.getClassLoader(serviceClassName);
            currentThread.setContextClassLoader(classLoader);
            // Create an instance of the service, and run it.
            Class serviceClass = classLoader.loadClass(serviceClassName);
            Service service = (Service) serviceClass.newInstance();
            // Execute the service.
            log.info("run() : Executing...");
            response.setStartTime(System.currentTimeMillis());
            Serializable output = service.execute(req.getInput());
            response.setEndTime(System.currentTimeMillis());
            response.setOutput(output);
            log.info("run() : Executed.");
        }
        catch (Exception e)
        {
            log.error(e,e);
            response.setEndTime(System.currentTimeMillis());
            response.setThrowable(e);
        }
        finally
        {
            // No matter what, set the class loader back to it's original value.
            currentThread.setContextClassLoader(originalClassLoader);
            try
            {
                server.sendResponse(response,replyTo);
            }
            catch (IOException e)
            {
                log.error("Unable to send response due to: " + e, e);
            }
            log.info("run() : LEAVE");
        }
    }


    public String toString()
    {
        return "ServiceRunner{" +
                "server=" + server +
                ", req=" + req +
                ", replyTo=" + replyTo +
                '}';
    }
}
