package org.jegrid.jms;


import org.apache.log4j.Logger;
import org.jegrid.Client;
import org.jegrid.LifecycleAware;
import org.jegrid.TaskRequest;
import org.jegrid.util.Util;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * Receives TaskRequest objects from a JMS queue and submits them on the grid.
 * <br>User: Joshua Davis
 * Date: Oct 8, 2006
 * Time: 3:49:38 PM
 */
public class JmsTaskPump implements Runnable, LifecycleAware
{
    private static Logger log = Logger.getLogger(JmsTaskPump.class);

    private static final long RECONNECT_WAIT = 10000;

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
    private static final long RECEIVE_TIMEOUT = 30000;

    public JmsTaskPump(Client client)
    {
        log.info("<ctor>");
        this.client = client;
    }

    /**
     * Set the context environment as a java.util.Properties encoded string.
     *
     * @param contextEnvironmentString the initial context environment
     */
    public void setContextEnvironment(String contextEnvironmentString)
    {
        log.info("setContextEnvironment()\n" + contextEnvironmentString);
        this.contextEnvironment = Util.decodeProperties(contextEnvironmentString);
    }

    public void setJmsDestinationName(String jmsDestinationName)
    {
        this.jmsDestinationName = jmsDestinationName;
    }

    public void setJmsConnectionFactoryName(String jmsConnectionFactoryName)
    {
        this.jmsConnectionFactoryName = jmsConnectionFactoryName;
    }


    public void initialize()
    {
        log.info("initialize()");
        Thread t = new Thread(this,"JmsTaskPump");
        t.setDaemon(true);
        t.start();
    }

    public void terminate()
    {
    }

    public void run()
    {
        log.info("run() : ENTER");
        try
        {
            while (true)
            {
                // Wait for servers first, so we don't drop (ignore) JMS messages.
                client.waitForServers(1);
                try
                {
                    connect();
                    log.info("Waiting for JMS message...");
                    Message m = consumer.receive(RECEIVE_TIMEOUT);
                    if (m != null)
                    {
                        log.info("Received " + m);
                        if (m instanceof ObjectMessage)
                        {
                            ObjectMessage objectMessage = (ObjectMessage) m;
                            Object o = objectMessage.getObject();
                            if (o instanceof TaskRequest)
                            {
                                TaskRequest taskRequest = (TaskRequest) o;
                                client.background(taskRequest);
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    log.error(e, e);
                    disconnect();
                    // Go to sleep....
                    Util.sleep(RECONNECT_WAIT);
                }
            }
        }
        catch (Exception e)
        {
            log.error(e, e);
        }
        log.info("run() : LEAVE");
    }

    private void disconnect()
    {
        if (consumer != null)
        {
            try
            {
                consumer.close();
            }
            catch (JMSException e)
            {
                log.warn(e);
            }
            consumer = null;
        }
        if (session != null)
        {
            try
            {
                session.close();
            }
            catch (JMSException e)
            {
                log.warn(e);
            }
            session = null;
        }
        if (connection != null)
        {
            try
            {
                connection.close();
            }
            catch (JMSException e)
            {
                log.warn(e);
            }
            connection = null;
        }
        connectionFactory = null;
        queue = null;
        if (ic != null)
        {
            try
            {
                ic.close();
            }
            catch (NamingException e)
            {
                log.warn(e);
            }
            ic = null;
        }
        log.info("Disconnected from " + jmsDestinationName);
    }

    private void connect()
            throws NamingException, JMSException
    {
        if (ic != null)
            return;
        ic = getInitialContext();
        queue = (Queue) ic.lookup(jmsDestinationName);
        connectionFactory = (QueueConnectionFactory) ic.lookup(jmsConnectionFactoryName);
        connection = connectionFactory.createQueueConnection();
        session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        consumer = session.createReceiver(queue);
        connection.start();
        log.info("Connected to " + jmsDestinationName);
    }


    private InitialContext getInitialContext()
            throws NamingException
    {
        return (contextEnvironment == null) ?
                new InitialContext() :
                new InitialContext(contextEnvironment);
    }
}
