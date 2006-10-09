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
    private ConnectionFactory connectionFactory;
    private Destination dest;
    private Connection connection;
    private Session session;
    private MessageConsumer consumer;
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
            connectionFactory = (ConnectionFactory)ic.lookup(config.getJmsConnectionFactoryName());
            dest = (Destination) ic.lookup(config.getJmsDestinationName());
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            consumer = session.createConsumer(dest);
            connection.start();
            while (true)
            {
                Message m = consumer.receive(config.getJmsReceiveTimeout());
                if (m != null && m instanceof ObjectMessage)
                {
                    ObjectMessage objectMessage = (ObjectMessage) m;
                    if (objectMessage instanceof TaskRequest)
                    {
                        TaskRequest taskRequest = (TaskRequest) objectMessage;
                        Task task = client.createTask(taskRequest.getTaskClassName());
                        List inputs = taskRequest.getInput();
                        for (Iterator iterator = inputs.iterator(); iterator.hasNext();)
                            task.addInput((Serializable) iterator.next());
                        Aggregator aggregator = instantiateAggregator(taskRequest.getAggregatorClassName());
                        task.run(aggregator,taskRequest.getMaxWorkers());
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.error(e,e);
        }
    }

    public Aggregator instantiateAggregator(String className)
            throws IllegalAccessException, InstantiationException, ClassNotFoundException
    {

        Class aClass = Thread.currentThread().getContextClassLoader().loadClass(className);
        return (Aggregator) aClass.newInstance();
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
