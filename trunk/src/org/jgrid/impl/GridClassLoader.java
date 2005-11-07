package org.jgrid.impl;

/**
 * TODO: Add class level javadoc
 * <br>User: Joshua Davis
 * <br>Date: Oct 22, 2005 Time: 3:58:57 PM
 */
public class GridClassLoader extends ClassLoader
{
    private String serviceClassName;
    private GridBusImpl gridBus;

    public GridClassLoader(ClassLoader parent, String serviceClassName, GridBusImpl gridBus)
    {
        super(parent);
        this.serviceClassName = serviceClassName;
        this.gridBus = gridBus;
    }

    protected Class findClass(String name) throws ClassNotFoundException
    {
        throw new ClassNotFoundException("Cannot find '" + name + "' because this class is not implemented yet!");
    }
}
