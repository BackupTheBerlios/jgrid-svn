package org.jegrid.util;

import org.picocontainer.MutablePicoContainer;
import org.picocontainer.defaults.CachingComponentAdapter;
import org.picocontainer.defaults.ConstructorInjectionComponentAdapter;
import org.picocontainer.defaults.DefaultPicoContainer;

/**
 * Wrapper around PicoContainer.
 * <br>User: Joshua Davis
 * <br>Date: Oct 9, 2005 Time: 8:56:44 AM
 */
public class MicroContainer
{
    private MutablePicoContainer pico;
    private Initializer initializer;

    public MicroContainer()
    {
    }

    public MicroContainer(Initializer initializer)
    {
        this.initializer = initializer;
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
                            key,implementation) ) );
    }

    private MutablePicoContainer getPico()
    {
        if (pico == null)
        {
            pico = new DefaultPicoContainer();
            if (initializer != null)
                initializer.initialize(this);
        }
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
        registerSingleton(key,implementationClass);
    }

    public interface Initializer
    {
        void initialize(MicroContainer microContainer);
    }
}
