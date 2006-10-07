package org.jegrid.util;

import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.picocontainer.defaults.*;

/**
 * Wrapper around PicoContainer.
 * <br>User: Joshua Davis
 * <br>Date: Oct 9, 2005 Time: 8:56:44 AM
 */
public class MicroContainer
{
    private MutablePicoContainer pico;

    public MicroContainer()
    {
    }

    public Object getComponentInstance(Object key)
    {
        return getPico().getComponentInstance(key);
    }

    public Class loadImplementation(String className) throws ClassNotFoundException
    {
        return Thread.currentThread().getContextClassLoader().loadClass(className);
    }

    public void registerSingleton(Object key, Class implementation)
    {
        // Once an implementation has been created, the same one should be returned
        // every time.  This is what CachingComponentAdapter does (see javadocs for
        // ConstructorInjectionComponentAdapter.
        getPico().registerComponent(
                new CachingComponentAdapter(
                        new ConstructorInjectionComponentAdapter(
                                key, implementation)));
    }

    private MutablePicoContainer getPico()
    {
        if (pico == null)
            pico = new DefaultPicoContainer();
        return pico;
    }

    public void registerComponentInstance(Object object)
    {
        // Regsisters an instance where it's class is the key.
        getPico().registerComponentInstance(object);
    }

    public void registerSingleton(Object key, String implementationName) throws ClassNotFoundException
    {
        Class implementationClass = loadImplementation(implementationName);
        registerSingleton(key, implementationClass);
    }

    public void registerEmptySingleton(Object key)
    {
        getPico().registerComponent(new EmptyComponentAdapter(key));
    }

    public interface Initializer
    {
        void initialize(MicroContainer microContainer);
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
