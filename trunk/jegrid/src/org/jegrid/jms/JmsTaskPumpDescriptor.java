package org.jegrid.jms;

import org.jegrid.GridSingletonDescriptor;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: Add class level javadoc.
 * <br>User: Joshua Davis
 * Date: Nov 6, 2006
 * Time: 11:14:27 PM
 */
public class JmsTaskPumpDescriptor extends GridSingletonDescriptor
{

    public JmsTaskPumpDescriptor()
    {
        super(JmsTaskPump.class,JmsTaskPump.class,new HashMap());
        Map props = getProperties();
        props.put("jmsDestinationName","name");
    }
}
