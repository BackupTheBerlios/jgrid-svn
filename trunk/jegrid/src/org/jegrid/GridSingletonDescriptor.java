package org.jegrid;

import java.util.Map;

/**
 * Describes a stateless signgleton that will be instantiated once and only once on the grid.
 * <br>User: Joshua Davis
 * Date: Nov 6, 2006
 * Time: 9:15:46 PM
 */
public class GridSingletonDescriptor
{
    private Object key;             // The key for the singleton, can be an interface.
    private Class impl;             // The singleton implementation class.
    private Map properties;         // Properties to be DI'd into the singleton.
    
    public GridSingletonDescriptor(Object key, Class impl)
    {
        this.key = key;
        this.impl = impl;
    }

    public GridSingletonDescriptor(Object key, Class impl,Map properties)
    {
        this.key = key;
        this.impl = impl;
        this.properties = properties;
    }

    public Object getKey()
    {
        return key;
    }

    public Class getImpl()
    {
        return impl;
    }

    public Map getProperties()
    {
        return properties;
    }
}
