package org.jgrid.test;

import org.jgrid.Service;
import org.jgrid.ClientSession;
import org.jgrid.*;

import java.io.Serializable;

/**
 * Test client for grid servers.
 * <br>User: Joshua Davis
 * <br>Date: Oct 22, 2005 Time: 12:47:56 PM
 */
public class TestClient
{
    public static class MyService implements Service
    {
        public Serializable execute(Serializable input)
        {
            return "executed " + input.toString();
        }
    }

    public static void main(String[] args)
    {
        GridConfiguration config = new GridConfiguration();
        ClientSession session = config.getClientSession();
        Job job = session.createJob(MyService.class);
        job.execute("first job");
        Object result = job.takeResult(5000);
        System.out.println("result = " + result);
    }
}
