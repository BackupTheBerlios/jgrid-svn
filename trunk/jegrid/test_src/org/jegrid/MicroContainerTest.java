package org.jegrid;

import junit.framework.TestCase;
import org.jegrid.util.MicroContainer;

/**
 * Test the microcontainer classes.
 * <br> User: jdavis
 * Date: Nov 23, 2006
 * Time: 11:29:31 AM
 */
public class MicroContainerTest extends TestCase
{
    private MicroContainer mc;

    protected void setUp() throws Exception
    {
        mc = new MicroContainer();
    }

    protected void tearDown() throws Exception
    {
        mc.dispose();
    }

    public void testSingleton()
    {
        mc.registerSingleton("fred", Thingie.class);
        mc.registerSingleton("bob", Bungler.class);
        // All components are registered, so we start the microcontainer, which will
        // apply the lifecycle stuff to any component.
        mc.start();
        Thingie x = (Thingie) mc.getComponentInstance("fred");
        Thingie y = (Thingie) mc.getComponentInstance("fred");
        assertSame(x,y);
        assertTrue(x.initialized);
        assertFalse(x.terminated);
        Bungler b = (Bungler) mc.getComponentInstance("bob");
        mc.stop();
        assertTrue(x.initialized);
        assertTrue(x.terminated);
    }

    public static class Thingie implements LifecycleAware
    {
        public boolean initialized;
        public boolean terminated;

        public void initialize()
        {
            initialized = true;
        }
        public void terminate()
        {
            terminated = true;
        }
    }

    public static class Bungler implements LifecycleAware
    {
        public boolean initialized;
        public boolean terminated;

        public void initialize()
        {
            initialized = true;
        }
        public void terminate()
        {
            terminated = true;
        }
    }
}
