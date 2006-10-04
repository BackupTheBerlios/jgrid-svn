package org.jgrid.util;

import org.jgroups.util.ContextObjectInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Utility methods for serializing and deserializing objects.
 * <br>User: Joshua Davis
 * <br>Date: Oct 2, 2005 Time: 3:02:38 PM
 */
public class SerializationUtil
{
    private static final Object mutex = new Object();
    private static final ByteArrayOutputStream baos = new ByteArrayOutputStream(512);

    public static byte[] compressByteArray(byte[] input, int compressionLevel)
    {
        Deflater deflater = new Deflater();
        deflater.reset();
        deflater.setLevel(compressionLevel);

        deflater.setInput(input);
        deflater.finish();

        ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);

        // Compress the data
        byte[] buf = createBuffer(input);
        while (!deflater.finished())
        {
            int count = deflater.deflate(buf);
            bos.write(buf, 0, count);
        }
        try
        {
            bos.close();
        }
        catch (IOException e)
        {
        }
        // Get the compressed data
        return bos.toByteArray();

    }

    private static byte[] createBuffer(byte[] bytes)
    {
        int bufsz = Math.min(bytes.length, 1024);
        return new byte[bufsz];
    }

    public static byte[] decompressByteArray(byte[] compressedData)
    {
        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedData.length);
        byte[] buf = createBuffer(compressedData);
        while (!inflater.finished())
        {
            try
            {
                int count = inflater.inflate(buf);
                bos.write(buf, 0, count);
            }
            catch (DataFormatException e)
            {
            }
        }
        try
        {
            bos.close();
        }
        catch (IOException e)
        {
        }
        return bos.toByteArray();
    }

    public static byte[] objectToByteArray(Object obj) throws IOException
    {
        byte[] result = null;
        synchronized (baos)
        {
            baos.reset();
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(obj);
            out.close();
            result = baos.toByteArray();
            result = compressByteArray(result, Deflater.BEST_SPEED);
        }
        return result;
    }

    public static Object byteArrayToObject(byte[] bytes) throws IOException, ClassNotFoundException
    {
        synchronized (mutex)
        {
            if (bytes == null) return null;
            Object retval = null;
            bytes = decompressByteArray(bytes);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ContextObjectInputStream(bais); // changed Nov 29 2004 (bela)
            retval = ois.readObject();
            ois.close();
            if (retval == null)
                return null;
            return retval;
        }

    }
}
