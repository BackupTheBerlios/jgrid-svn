package org.jegrid.util;

import org.apache.log4j.Logger;
import org.jegrid.GridSingletonDescriptor;
import org.jegrid.LifecycleAware;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.picocontainer.defaults.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Wrapper around PicoContainer.
 * <br>User: Joshua Davis
 * <br>Date: Oct 9, 2005 Time: 8:56:44 AM
 */
public class MicroContainer
{
    private static Logger log = Logger.getLogger(MicroContainer.class);

    private MutablePicoContainer pico;

    public MicroContainer()
    {
        pico = new DefaultPicoContainer();
    }

    public MicroContainer(MicroContainer parent)
    {
        pico = new DefaultPicoContainer(parent.pico);
    }

    public Object getComponentInstance(Object key)
    {
        return pico.getComponentInstance(key);
    }

    public Class loadImplementation(String className) throws ClassNotFoundException
    {
        return Thread.currentThread().getContextClassLoader().loadClass(className);
    }

    public void registerSingleton(Object key, Class implementation)
    {
        registerSingleton(key,implementation,null);
    }

    public void registerSingleton(Object key, Class implementation, Map properties)
    {
        // No properties? Just do the normal registration.
        if (properties == null || properties.size() == 0)
        {
            // Once an implementation has been created, the same one should be returned
            // every time.  This is what CachingComponentAdapter does (see javadocs for
            // ConstructorInjectionComponentAdapter.
            // log.debug("Registering " + key + "->" + implementation.getName());        
            pico.registerComponent(
                    new CachingComponentAdapter(
                            new ConstructorInjectionComponentAdapter(
                                    key, implementation)));
        }
        else
        {
            // Otherwise, add the Bean adapter to inject all the properties
            // into the object using setters after it is constructed.
            log.info("Registering " + key + "->" + implementation.getName() + " with bean properties.");        
            ConstructorInjectionComponentAdapter componentAdapter =
                    new ConstructorInjectionComponentAdapter(
                            key, implementation);
            BeanPropertyComponentAdapter beanAdaptor =
                    new BeanPropertyComponentAdapter(componentAdapter);
            beanAdaptor.setProperties(properties);
            pico.registerComponent(
                    new CachingComponentAdapter(
                            beanAdaptor));
        }
    }

    public void registerComponentInstance(Object object)
    {
        // Regsisters an instance where it's class is the key.
        pico.registerComponentInstance(object);
    }

    public void registerSingleton(Object key, String implementationName) throws ClassNotFoundException
    {
        Class implementationClass = loadImplementation(implementationName);
        registerSingleton(key, implementationClass);
    }

    public void registerEmptySingleton(Object key)
    {
        pico.registerComponent(new EmptyComponentAdapter(key));
    }

    public static void initializeComponent(Object component)
    {
        if (component instanceof LifecycleAware)
        {
            LifecycleAware lifecycleAware = (LifecycleAware) component;
            lifecycleAware.initialize();
        }
    }

    public static void destroyComponent(Object component)
    {
        if (component instanceof LifecycleAware)
        {
            LifecycleAware lifecycleAware = (LifecycleAware) component;
            lifecycleAware.terminate();
        }
    }

    public void initializeFromDescriptors(List list)
    {
        for (Iterator iterator = list.iterator(); iterator.hasNext();)
        {
            GridSingletonDescriptor descriptor = (GridSingletonDescriptor) iterator.next();
            registerSingleton(descriptor.getKey(),descriptor.getImpl(),descriptor.getProperties());
        }
        // Now create them all.
        for (Iterator iterator = list.iterator(); iterator.hasNext();)
        {
            GridSingletonDescriptor descriptor = (GridSingletonDescriptor) iterator.next();
            Object key = descriptor.getKey();
            log.info("Instantiating " + key + " ...");
            Object component = getComponentInstance(key);
            MicroContainer.initializeComponent(component);
        }
    }

    public void destroyFromDescriptors(List list)
    {
        // Destroy them all.
        for (Iterator iterator = list.iterator(); iterator.hasNext();)
        {
            GridSingletonDescriptor descriptor = (GridSingletonDescriptor) iterator.next();
            Object key = descriptor.getKey();
            log.info("Destroying " + key + " ...");
            Object component = getComponentInstance(key);
            destroyComponent(component);
        }
        pico.dispose();

    }
    private class EmptyComponentAdapter extends InstanceComponentAdapter
    {

        public EmptyComponentAdapter(Object componentKey) throws AssignabilityRegistrationException, NotConcreteRegistrationException
        {
            super(componentKey, "");
        }

        public Object getComponentInstance(PicoContainer container)
        {
            return null;
        }
    }
}
