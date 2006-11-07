package org.jegrid.util;

import org.jegrid.GridException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Assorted utility methods.
 * <br>User: jdavis
 * Date: Nov 7, 2006
 * Time: 9:18:27 AM
 */
public class Util
{
    /**
     * Sleep for the specified number of milliseconds.
     * @param millis milliseconds to sleep
     */
    public static void sleep(long millis)
    {
        //noinspection EmptyCatchBlock
        try
        {
            Thread.sleep(millis);
        }
        catch (Exception ignore)
        {
        }
    }

    /**
     * Encodes a java.util.Properties as a string.
     * @param properties the properties
     * @return the string
     */
    public static String encodeProperties(Properties properties)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try
        {
            properties.store(baos,null);
        }
        catch (IOException e)
        {
            throw new GridException(e);
        }
        return baos.toString();
    }

    /**
     * Decodes a java.util.Properties from a string
     * @param propertiesString the encoded properties
     * @return the properties
     */
    public static Properties decodeProperties(String propertiesString)
    {
        ByteArrayInputStream in = new ByteArrayInputStream(propertiesString.getBytes());
        Properties props = new Properties();
        try
        {
            props.load(in);
        }
        catch (IOException e)
        {
            throw new GridException(e);
        }
        return props;
    }
}
