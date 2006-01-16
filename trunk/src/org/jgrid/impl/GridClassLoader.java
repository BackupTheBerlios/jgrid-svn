package org.jgrid.impl;

import org.apache.log4j.Logger;
import org.jgroups.blocks.GroupRequest;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;

/**
 * TODO: Add class level javadoc
 * <br>User: Joshua Davis
 * <br>Date: Oct 22, 2005 Time: 3:58:57 PM
 */
public class GridClassLoader extends ClassLoader
{
    private static Logger log = Logger.getLogger(GridClassLoader.class);
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
        log.info("findClass(" + name + ")");
        byte[] bytes = getClassBytes(name);
        log.info("" + bytes.length + " bytes received, defining class...");
        return defineClass(name,bytes,0,bytes.length);
    }

    private byte[] getClassBytes(String name) throws ClassNotFoundException {
        GridRpcDispatcher dispatcher = gridBus.getDispatcher();
        PeersImpl peers = (PeersImpl) gridBus.getPeers();
        int mode = GroupRequest.GET_ALL;
        long timeout = 1000;
        log.info("getClassBytes(" + name + ") : Invoking...");
        RspList list = dispatcher.callRemoteMethods(peers.everyoneButMe(),"_getClassBytes",
                new Object[] { name }, new Class[] { name.getClass() },
                mode,
                timeout);
        log.info("getClassBytes(" + name + ") : " + list.size() + " responses.");
        if (list.size() == 0)
        {
            log.warn("Zero responses to getClassBytes()");
            throw new ClassNotFoundException("No responses: Grid " + gridBus.getConfig().getGridName() + " does not know about class " + name);
        }
        for (int i = 0; i < list.size(); i ++)
        {
            Rsp rsp = (Rsp)list.elementAt(i);
            Object val = rsp.getValue();
            if (val != null && val instanceof ClassBytes) {
                ClassBytes classBytes = (ClassBytes) val;
                byte[] bytes = classBytes.getBytes();
                log.info("Class " + classBytes.getName() + " : " + bytes.length + " bytes returned from " + rsp.getSender());
                return bytes;
            }
        }
        log.warn("No valid responses to getClassBytes()");
        throw new ClassNotFoundException("No valid responses: Grid " + gridBus.getConfig().getGridName() + " does not know about class " + name);
    }
}
