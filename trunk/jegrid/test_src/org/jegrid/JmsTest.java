package org.jegrid;

import junit.framework.TestCase;

import javax.naming.InitialContext;
import javax.naming.Context;
import java.util.Hashtable;

/**
 * TODO: Add class level javadoc.
 * <br>User: Joshua Davis
 * Date: Oct 8, 2006
 * Time: 5:02:36 PM
 */
public class JmsTest extends TestCase
{
    public void testInitialContext() throws Exception
    {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY,"org.jnp.interfaces.NamingContextFactory");
        env.put(Context.PROVIDER_URL,"jnp://192.168.0.5:1099");
        env.put(Context.URL_PKG_PREFIXES,"org.jboss.naming:org.jnp.interfaces");
        InitialContext ic = new InitialContext(env);
        ic.lookup("queue/A");
    }
}
