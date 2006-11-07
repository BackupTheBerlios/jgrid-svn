package org.jegrid.jms;

import org.jegrid.GridSingletonDescriptor;
import org.jegrid.util.Util;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

/**
 * Grid singleton descriptor for the JMS task pump.
 * <br>User: Joshua Davis
 * Date: Nov 6, 2006
 * Time: 11:14:27 PM
 */
public class JmsTaskPumpDescriptor extends GridSingletonDescriptor
{

    public JmsTaskPumpDescriptor(Hashtable initalContextEnvironment, String connectionFactoryJndiName, String queueJndiName)
    {
        super(JmsTaskPump.class, JmsTaskPump.class, new HashMap());
        // Add the properties to the descriptor.
        Map props = getProperties();
        Properties contextProperties = new Properties();
        contextProperties.putAll(initalContextEnvironment);
        props.put("contextEnvironment", Util.encodeProperties(contextProperties));
        props.put("jmsConnectionFactoryName", connectionFactoryJndiName);
        props.put("jmsDestinationName", queueJndiName);
    }
}
