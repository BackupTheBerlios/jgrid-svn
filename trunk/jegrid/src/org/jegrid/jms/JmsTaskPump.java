package org.jegrid.jms;


import org.apache.log4j.Logger;
import org.jegrid.Client;
import org.jegrid.GridConfiguration;
import org.jegrid.Task;
import org.jegrid.TaskRequest;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * Receives tasks from a queue and submits them on the grid.
 * <br>User: Joshua Davis
 * Date: Oct 8, 2006
 * Time: 3:49:38 PM
 */
public class JmsTaskPump implements Runnable
{
    private static Logger log = Logger.getLogger(JmsTaskPump.class);

    private Hashtable contextEnvironment;
    private String jmsDestinationName;
    private String jmsConnectionFactoryName;

    private InitialContext ic;
    private QueueConnectionFactory connectionFactory;
    private Queue queue;
    private QueueConnection connection;
    private QueueSession session;
    private QueueReceiver consumer;
    private Client client;

    public JmsTaskPump(Client client)
    {
    }

    public void setContextEnvironment(Hashtable contextEnvironment)
    {
        this.contextEnvironment = contextEnvironment;
    }

    public void setJmsDestinationName(String jmsDestinationName)
    {
        this.jmsDestinationName = jmsDestinationName;
    }

    public void setJmsConnectionFactoryName(String jmsConnectionFactoryName)
    {
        this.jmsConnectionFactoryName = jmsConnectionFactoryName;
    }

    public void run()
    {
        try
        {
            ic = getInitialContext();
            queue = (Queue) ic.lookup(jmsDestinationName);
            connectionFactory = (QueueConnectionFactory) ic.lookup(jmsConnectionFactoryName);
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
                    Message m = consumer.receive();
                    if (m != null && m instanceof ObjectMessage)
                    {
                        ObjectMessage objectMessage = (ObjectMessage) m;
                        Object o = objectMessage.getObject();
                        if (o instanceof TaskRequest)
                        {
                            TaskRequest taskRequest = (TaskRequest) o;
                            task.run(taskRequest, false);
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
        InitialContext initialContext = (contextEnvironment == null) ?
                new InitialContext() :
                new InitialContext(contextEnvironment);
        return initialContext;
    }
}
