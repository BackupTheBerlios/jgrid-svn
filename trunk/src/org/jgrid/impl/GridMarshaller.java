package org.jgrid.impl;

import org.jgroups.blocks.RpcDispatcher;
import org.jgrid.util.SerializationUtil;

/**
 * RPC marshaller.
 * <br>User: Joshua Davis
 * <br>Date: Oct 25, 2005 Time: 7:08:37 AM
 */
public class GridMarshaller implements RpcDispatcher.Marshaller
{
    public byte[] objectToByteBuffer(Object obj) throws Exception
    {
        return SerializationUtil.objectToByteArray(obj);
    }

    public Object objectFromByteBuffer(byte[] buf) throws Exception
    {
        return SerializationUtil.byteArrayToObject(buf);
    }
}
