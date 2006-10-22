package org.jegrid.impl;

import org.apache.log4j.NDC;
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
    private TaskRequest request;
    private Task task;
    private TaskId taskId;

    public RemoteTaskWorker(ServerImpl server, Client client, TaskRequest request, TaskId taskId)
    {
        super(server);
        this.taskId = taskId;
        this.request = request;
        task = client.createTask();
    }

    public void run()
    {
        NDC.push(taskId.getClient().toString() + "-" + task.getTaskId());
        try
        {
            task.run(request, true);
        }
        finally
        {
            NDC.pop();
            server.done(taskId);
        }
    }
}
