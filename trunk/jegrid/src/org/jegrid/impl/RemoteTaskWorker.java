package org.jegrid.impl;

import org.apache.log4j.Logger;
import org.jegrid.Client;
import org.jegrid.Task;
import org.jegrid.TaskId;
import org.jegrid.TaskRequest;

/**
 * InputProcessingWorker that runs the entire task as if it was both client and server.
 * <br>User: Joshua Davis
 * Date: Oct 21, 2006
 * Time: 8:58:34 PM
 */
public class RemoteTaskWorker extends Worker
{
    private static Logger log = Logger.getLogger(RemoteTaskWorker.class);

    private TaskRequest request;
    private Task task;
    private TaskId taskId;
    private ServerImpl server;

    public RemoteTaskWorker(ServerImpl server, Client client, TaskRequest request, TaskId taskId)
    {
        super(server.getGrid());
        this.server = server;
        this.taskId = taskId;
        this.request = request;
        task = client.createTask(request.getTaskKey());
    }

    public void run()
    {
        try
        {
            task.run(request, true);
        }
        catch (Exception e)
        {
            log.error(e,e);
        }
        finally
        {
            server.done(taskId);
        }
    }
}
