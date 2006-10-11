package org.jegrid.jms;

import org.apache.log4j.Logger;
import org.jegrid.*;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

/**
 * Receives tasks from a queue and submits them on the grid.
 * <br>User: Joshua Davis
 * Date: Oct 8, 2006
 * Time: 3:49:38 PM
 */
public class JmsTaskPump implements Runnable
{
    private static Logger log = Logger.getLogger(JmsTaskPump.class);

    private GridConfiguration config;
    private InitialContext ic;
    private QueueConnectionFactory connectionFactory;
    private Queue queue;
    private QueueConnection connection;
    private QueueSession session;
    private QueueReceiver consumer;
    private Client client;

    public JmsTaskPump(GridConfiguration config, Client client)
    {
        this.config = config;
        this.client = client;
    }

    public void run()
    {
        try
        {
            ic = getInitialContext();
            queue = (Queue) ic.lookup(config.getJmsDestinationName());
            connectionFactory = (QueueConnectionFactory) ic.lookup(config.getJmsConnectionFactoryName());
            connection = connectionFactory.createQueueConnection();
            session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            consumer = session.createReceiver(queue);
            connection.start();
            Task task = client.createTask();
            while (true)
            {
                task.acquire();
                try
                {
                    Message m = consumer.receive(config.getJmsReceiveTimeout());
                    if (m != null && m instanceof ObjectMessage)
                    {
                        ObjectMessage objectMessage = (ObjectMessage) m;
                        Object o = objectMessage.getObject();
                        if (o instanceof TaskRequest)
                        {
                            TaskRequest taskRequest = (TaskRequest) o;
                            task.run(taskRequest);
                        }
                    }
                }
                finally
                {
                    task.release();
                }
            }
        }
        catch (Exception e)
        {
            log.error(e, e);
        }
    }


    private InitialContext getInitialContext()
            throws NamingException
    {
        Hashtable contextEnvironment = config.getInitialContextEnvironment();
        InitialContext initialContext = (contextEnvironment == null) ?
                new InitialContext() :
                new InitialContext(contextEnvironment);
        return initialContext;
    }
}
