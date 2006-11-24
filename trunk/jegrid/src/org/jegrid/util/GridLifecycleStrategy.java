package org.jegrid.util;

import org.jegrid.LifecycleAware;
import org.picocontainer.Disposable;
import org.picocontainer.Startable;
import org.picocontainer.defaults.LifecycleStrategy;

/**
 * A picocontainer lifecycle strategy for the grid.
 * <br> User: jdavis
 * Date: Nov 23, 2006
 * Time: 11:43:03 AM
 */
class GridLifecycleStrategy implements LifecycleStrategy
{
    public void start(Object component)
    {
        if (component != null)
        {
            if (component instanceof Startable)
            {
                Startable s = (Startable) component;
                s.start();
            }
            if (component instanceof LifecycleAware)
            {
                LifecycleAware la = (LifecycleAware) component;
                la.initialize();
            }
        }
    }

    public void stop(Object component)
    {
        if (component != null)
        {
            if (component instanceof Startable)
            {
                Startable s = (Startable) component;
                s.stop();
            }
            if (component instanceof LifecycleAware)
            {
                LifecycleAware la = (LifecycleAware) component;
                la.terminate();
            }
        }
    }

    public void dispose(Object component)
    {
        if (component != null)
        {
            if (component instanceof Disposable)
            {
                Disposable d = (Disposable) component;
                d.dispose();
            }
        }
    }

    public boolean hasLifecycle(Class type)
    {
        return
                Startable.class.isAssignableFrom(type) ||
                Disposable.class.isAssignableFrom(type) ||
                LifecycleAware.class.isAssignableFrom(type);
    }
}
