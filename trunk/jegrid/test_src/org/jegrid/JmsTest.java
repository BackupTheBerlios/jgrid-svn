package org.jegrid;

import junit.framework.TestCase;

import javax.naming.InitialContext;
import javax.naming.Context;
import javax.jms.*;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.util.Hashtable;

/**
 * TODO: Add class level javadoc.
 * <br>User: Joshua Davis
 * Date: Oct 8, 2006
 * Time: 5:02:36 PM
 */
public class JmsTest extends TestCase
{
    public void testConsumer() throws Exception
    {
        Hashtable env = new Hashtable();
//        env.put(Context.INITIAL_CONTEXT_FACTORY,"org.jnp.interfaces.NamingContextFactory");
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.NamingContextFactory");
        env.put(Context.PROVIDER_URL, "jnp://192.168.0.5:1099");
        env.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
        InitialContext ic = new InitialContext(env);

        QueueConnectionFactory qcf = (QueueConnectionFactory) ic.lookup("ConnectionFactory");
//        QueueConnectionFactory qcf = (QueueConnectionFactory) ic.lookup("java:JmsXA");
        QueueConnection conn = qcf.createQueueConnection();
        QueueSession session = conn.createQueueSession(true, QueueSession.AUTO_ACKNOWLEDGE);
        conn.start();
        Queue queue = (Queue) ic.lookup("queue/A");
        QueueReceiver receiver = session.createReceiver(queue);
        receiver.setMessageListener(new Listener());
        QueueSender sender = session.createSender(queue);
        // For this one needs jboss-transactionclient.jar
        UserTransaction tx = (UserTransaction) ic.lookup("java:/UserTransaction");
        tx.begin();
        for (int i = 0; i < 10; i++)
        {
            TextMessage tm = session.createTextMessage("MSG" + i);
            sender.send(tm);
        }
        System.out.println("commit...");
        tx.commit();
        Thread.sleep(5000);
    }

    private class Listener implements MessageListener
    {
        public void onMessage(Message message)
        {
            if (message instanceof TextMessage)
            {
                TextMessage textMessage = (TextMessage) message;
                try
                {
                    System.out.println(textMessage.getText());
                }
                catch (JMSException e)
                {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
    }
}
