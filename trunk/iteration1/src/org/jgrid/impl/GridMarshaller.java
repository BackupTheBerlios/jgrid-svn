package org.jgrid.impl;

import org.jgroups.blocks.RpcDispatcher;
import org.jgrid.util.SerializationUtil;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * RPC marshaller.
 * <br>User: Joshua Davis
 * <br>Date: Oct 25, 2005 Time: 7:08:37 AM
 */
public class GridMarshaller implements RpcDispatcher.Marshaller
{
    private static Logger log = Logger.getLogger(GridMarshaller.class);

    public byte[] objectToByteBuffer(Object obj) throws Exception
    {
        return SerializationUtil.objectToByteArray(obj);
    }

    public Object objectFromByteBuffer(byte[] buf) throws Exception
    {
        try {
            return SerializationUtil.byteArrayToObject(buf);
        } catch (Exception e) {
            log.error(e,e);
            throw e;
        }
    }
}
