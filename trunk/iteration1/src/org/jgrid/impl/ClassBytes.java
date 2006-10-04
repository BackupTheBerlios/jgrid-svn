package org.jgrid.impl;

import org.jgrid.util.SerializationUtil;

import java.io.Serializable;
import java.util.zip.Deflater;

/**
 * Transfer object for the GridClassLoader.   Grid nodes return this as a response when they receive the
 * getClassBytes() message.
 * <br>User: Joshua Davis
 * Date: Jan 15, 2006
 * Time: 6:07:35 PM
 */
public class ClassBytes implements Serializable {
    private String name;
    private byte[] bytes;

    public ClassBytes(String name, byte[] bytes) {
        this.name = name;
        this.bytes = SerializationUtil.compressByteArray(bytes, Deflater.BEST_SPEED);
    }

    public String getName() {
        return name;
    }

    public byte[] getBytes() {
        return SerializationUtil.decompressByteArray(bytes);
    }

}
