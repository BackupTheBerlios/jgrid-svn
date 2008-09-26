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
 * Receives TaskRequest objects from a JMS queue and submits them on the grid
 * as background jobs.
 * @author Joshua Davis
 * @author Greg Kerdemelidis - added the shutdown code.
 * Date: Oct 8, 2006
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

    private Thread thread;
    private volatile boolean running;

    public JmsTaskPump(Client client)
    {
        log.info("<ctor>");
        this.client = client;
        this.running = false;
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

    /**
     * Set the name of the JMS Destination we're going to connect to.
     * @param jmsDestinationName name of jms destination.
     */
    public void setJmsDestinationName(String jmsDestinationName)
    {
        this.jmsDestinationName = jmsDestinationName;
    }

    /**
     * Set the class we use for creating JMS connections.
     * @param jmsConnectionFactoryName class name.
     */
    public void setJmsConnectionFactoryName(String jmsConnectionFactoryName)
    {
        this.jmsConnectionFactoryName = jmsConnectionFactoryName;
    }

    /**
     * Start the singleton thread.
     */
    public void initialize()
    {
        log.info("initialize()");
        thread = new Thread(this, "JmsTaskPump:" + jmsDestinationName);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Shut down the background thread.
     */
    public void terminate()
    {
        if(thread==null || !this.running)
            return;
        log.info("terminate()");
        running = false;
        thread.interrupt();
    }

    /**
     * Background thread that consumes requests from JMS and sends the requests to the grid.
     */
    public void run()
    {
        this.running=true;
        log.info("run() : ENTER");
        while (running)
        {
            try
            {
                // Wait for servers first, so we don't drop (ignore) JMS messages.
                log.info("Waiting for available servers...");
                client.waitForServers(1, 1, Client.WAIT_FOREVER);
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
                            client.background(taskRequest, Client.WAIT_FOREVER);
                        }
                    }
                    else
                    {
                        log.error("Discarding message!  It was not a TaskRequest!");
                    }
                }
            }
            catch (Exception e)
            {
                log.error("Exception: " + e, e);
                log.warn("Disconnecting because of exception...");
                disconnect();
                // Go to sleep....
                Util.sleep(RECONNECT_WAIT);
            }
        } // while
        log.info("run() : LEAVE");
    }

    /**
     * Disconnect from the grid.
     */
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

    /**
     * Connect to the grid.
     *
     * @throws NamingException on JNDI lookup failure finding the JMS connection factory or the queue.
     * @throws JMSException on messaging fault
     */
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

    /**
     * Get the IC.
     * @return the InitialContext.
     * @throws NamingException on JNDI error.
     */
    private InitialContext getInitialContext()
            throws NamingException
    {
        return (contextEnvironment == null) ?
                new InitialContext() :
                new InitialContext(contextEnvironment);
    }
}
