package org.jegrid.util;

import org.jegrid.GridException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

/**
 * Assorted utility methods.
 * <br>User: jdavis
 * Date: Nov 7, 2006
 * Time: 9:18:27 AM
 */
public class Util
{
    private static final String OS_NAME;
    private static final String PROGRAM_EXTENSION;
    private static final String PATH_SEP;

    private static int javaVersionNumber;

    static {

        // Borrowed from ANT JavaEnvUtils.java ...
        
        // Determine the Java version by looking at available classes
        // java.lang.Readable was introduced in JDK 1.5
        // java.lang.CharSequence was introduced in JDK 1.4
        // java.lang.StrictMath was introduced in JDK 1.3
        // java.lang.ThreadLocal was introduced in JDK 1.2
        // java.lang.Void was introduced in JDK 1.1
        // Count up version until a NoClassDefFoundError ends the try

        try {
            javaVersionNumber = 10;
            Class.forName("java.lang.Void");
            javaVersionNumber++;
            Class.forName("java.lang.ThreadLocal");
            javaVersionNumber++;
            Class.forName("java.lang.StrictMath");
            javaVersionNumber++;
            Class.forName("java.lang.CharSequence");
            javaVersionNumber++;
            Class.forName("java.lang.Readable");
            javaVersionNumber++;
        } catch (Throwable ignore) {
            // If one of the Class.forName() calls fails, we have determined
            // the version of the JVM.
        }
        PATH_SEP = System.getProperty("path.separator");
        OS_NAME = System.getProperty("os.name").toLowerCase(Locale.US);
        PROGRAM_EXTENSION =
                PATH_SEP.equals(";") && !(OS_NAME.indexOf("netware") > -1) ?
                ".exe" : "";
    }

    private Util()
    {
    }
    
    /**
     * @return Java version number (10 = 1.0, 14 = Java 1.4, 15 = Java 5
     */
    public static int getJavaVersion()
    {
        return javaVersionNumber;
    }

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

    public static String getJavaCommand()
    {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null || javaHome.length() == 0)
            throw new RuntimeException("System property 'java.home' not defined!");
        File jhome = new File(javaHome);
        if (!jhome.exists())
            throw new RuntimeException(javaHome + " does not exist!");
        if (!jhome.isDirectory())
            throw new RuntimeException(javaHome + " is not a directory!");
        File[] files = jhome.listFiles();
        File bindir = null;
        for (int i = 0; i < files.length; i++)
        {
            File file = files[i];
            if (!file.isDirectory())
                continue;
            if (file.getName().indexOf("bin") >= 0)
            {
                bindir = file;
                break;
            }
        }
        if (bindir == null)
            throw new RuntimeException("No bin directory found in " + javaHome + " !");
        File javaprogram = new File(bindir,"java" + PROGRAM_EXTENSION);
        if (!(javaprogram.exists() && javaprogram.isFile()))
            throw new RuntimeException(javaprogram + " does not exist!");
        return javaprogram.getAbsolutePath();        
    }
}
