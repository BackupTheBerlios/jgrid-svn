package org.jegrid.util;

import java.io.*;
import java.util.ArrayList;

/**
 * Provides stream copying capability in a Runnable class.  This can be used to
 * redirect streams from a spawned JVM, or to 'pump' a one side of
 * PipedInputStream / PipedOutputStream pair.<br>
 * Also provides a static method that copies an entire input stream into
 * an output stream.
 * @author Joshua Davis
 */
public class StreamCopier implements Runnable
{
    /** The default buffer size. **/
    public static final int DEFAULT_BUFFER_SIZE = 256;
    /** The input stream. **/
    private InputStream in;
    /** The output stream. **/
    private OutputStream out;
    /** The buffer size to use while copying. **/
    private int bufsz = StreamCopier.DEFAULT_BUFFER_SIZE;
    /** If an exception was thrown in the run() method, this will be set. **/
    private IOException exception;
    /** True, if the copying is complete. **/
    private boolean complete = false;

    private static final int DEFAULT_BYTE_ARRAY_BUFSZ = 128;
    private static final int UNLIMITED = -1;

    /**
     * Copies the input stream into the output stream in a thread safe and
     * efficient manner.
     * @param in The input stream.
     * @param out The output stream.  If this is null, the input will be
     * discarded, similar to piping to /dev/null on UN*X.
     * @param bufsz The size of the buffer to use.
     * @return int The number of bytes copied.
     * @throws java.io.IOException When the stream could not be copied.
     * @throws InterruptedException if the copy was interrupted
     */
    public static int copy(InputStream in, OutputStream out, int bufsz)
            throws IOException, InterruptedException
    {
        // From Java I/O, page 43
        // Do not allow other threads to read from the input or write to the
        // output while the copying is taking place.
        synchronized (in)
        {
            if (out != null)
            {
                synchronized (out)
                {
                    return StreamCopier.unsyncCopy(in, out, bufsz);
                } // synchronized (out)
            }
            else
            {
                return StreamCopier.unsyncCopy(in, out, bufsz);
            }
        } // synchronized (in)
    }

    /**
     * Copies the input stream into the output stream in an efficient manner.
     * This version does not synchronize on the streams, so it is not safe
     * to use when the streams are being accessed by multiple threads.
     * @param in The input stream.
     * @param out The output stream.  If this is null, the input will be
     * discarded, similar to piping to /dev/null on UN*X.
     * @param bufsz The size of the buffer to use.
     * @return int The number of bytes copied.
     * @throws java.io.IOException When the stream could not be copied.
     * @throws InterruptedException if the copy was interrupted
     */
    public static int unsyncCopy(InputStream in, OutputStream out,
                                       int bufsz) throws IOException, InterruptedException
    {
        return StreamCopier.unsyncCopy(in, out, bufsz, StreamCopier.UNLIMITED);
    }

    /**
     * Copies the input stream into the output stream in an efficient manner.
     * This version does not synchronize on the streams, so it is not safe
     * to use when the streams are being accessed by multiple threads.
     * @param in The input stream.
     * @param out The output stream.  If this is null, the input will be
     * discarded, similar to piping to /dev/null on UN*X.
     * @param bufsz The size of the buffer to use.
     * @param limit The number of bytes to copy, or UNLIMITED (-1) to copy
     * until the end of the input stream.
     * @return int The number of bytes copied.
     * @throws java.io.IOException When the stream could not be copied.
     * @throws InterruptedException if the copy was interrupted
     */
    public static int unsyncCopy(InputStream in, OutputStream out, int bufsz, int limit) throws IOException, InterruptedException
    {
        return Copier.copy(in,out,bufsz,limit);
    }

    /**
     * Copies the input stream (reader) into the output stream (writer) in an efficient manner.
     * This version does not synchronize on the streams, so it is not safe
     * to use when the streams are being accessed by multiple threads.
     * @param in The input reader
     * @param out The output writer.  If this is null, the input will be
     * discarded, similar to piping to /dev/null on UN*X.
     * @param bufsz The size of the buffer to use.
     * @param limit The number of bytes to copy, or UNLIMITED (-1) to copy
     * until the end of the input stream.
     * @return int The number of bytes copied.
     * @throws java.io.IOException When the stream could not be copied.
     **/
    public static int unsyncCopy(Reader in, Writer out, int bufsz, int limit) throws IOException
    {
        return Copier.copy(in,out,bufsz,limit);
    }

    /**
     * Copies the input stream into the output stream in a thread safe and
     * efficient manner.
     * @param in The input stream.
     * @param out The output stream.  If this is null, the input will be
     * discarded, similar to piping to /dev/null on UN*X.
     * @return int The number of bytes copied.
     * @throws java.io.IOException When the stream could not be copied.
     * @throws InterruptedException if the copy was interrupted
     */
    public static int copy(InputStream in, OutputStream out)
            throws IOException, InterruptedException
    {
        return StreamCopier.copy(in, out, StreamCopier.DEFAULT_BUFFER_SIZE);
    }


    /**
     * Reads the entire input stream into an array list of byte arrays, each
     * byte array being a maximum of 'blocksz' bytes long.
     * @param blocksz The block size.  Byte arrays in the list will not
     * be longer than this.
     * @param in The input stream.
     * @return ArrayList - An array list of byte arrays.
     * @throws java.io.IOException When something happens while reading the stream.
     */
    public static ArrayList readBlocks(InputStream in, int blocksz)
            throws IOException
    {
        ArrayList list = new ArrayList();
        byte[] chunk;
        byte[] buf = new byte[blocksz];
        int bytesRead;
        while (true)
        {
            bytesRead = in.read(buf);
            if (bytesRead == -1)
                break;
            // Add a new chunk to the list.
            chunk = new byte[bytesRead];
            System.arraycopy(buf,0,chunk,0,bytesRead);
            list.add(chunk);
        } // while
        return list;
    }

    /**
     * Reads the entire input stream into a byte array.
     * @param in The input stream.
     * @throws java.io.IOException When something happens while reading the stream.
     * @throws InterruptedException if the copy was interrupted
     * @return the byte array
     */
    public static byte[] readByteArray(InputStream in)
            throws IOException, InterruptedException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamCopier.unsyncCopy(in,baos,StreamCopier.DEFAULT_BYTE_ARRAY_BUFSZ);
        return baos.toByteArray();
    }

    /** Reads the entire input stream into a byte array with a limit.
     * @param in The input reader
     * @param limit The number of bytes to read.
     * @return An array of bytes read from the input.
     * @throws java.io.IOException When something happens while reading the stream.
     * @throws InterruptedException if the copy was interrupted
     */
    public static byte[] readByteArray(InputStream in,int limit)
            throws IOException, InterruptedException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamCopier.unsyncCopy(in,baos,StreamCopier.DEFAULT_BUFFER_SIZE,limit);
        return baos.toByteArray();
    }

    /** Reads the entire input stream into a byte array with a limit.
     * @param in The input reader
     * @param limit The number of bytes to read.
     * @exception java.io.IOException Thrown if there was an error while copying.
     * @return An array of bytes read from the input.
     */
    public static byte[] readByteArray(Reader in,int limit)
        throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamCopier.unsyncCopy(in,new OutputStreamWriter(baos),StreamCopier.DEFAULT_BUFFER_SIZE,limit);
        return baos.toByteArray();
    }

    /**
     * Reads the entire input stream into a byte array.
     * @param in The input reader
     * @exception java.io.IOException Thrown if there was an error while copying.
     * @return An array of bytes read from the input.
     */
    public static byte[] readByteArray(Reader in)
        throws IOException
    {
        return StreamCopier.readByteArray(in,-1);
    }

    /**
     * Reads the specified file into a byte array.
     * @param file The file to read.
     * @throws java.io.IOException When something happens while reading the stream.
     * @throws InterruptedException if the copy was interrupted
     * @return the byte array
     */
    public static byte[] readByteArray(File file)
            throws IOException, InterruptedException
    {
        return StreamCopier.readByteArray(new BufferedInputStream(
                new FileInputStream(file),StreamCopier.DEFAULT_BUFFER_SIZE));
    }

    /**
     * Reads the specified file into a byte array.
     * @param fileName The file name to read.
     * @throws java.io.IOException When something happens while reading the stream.
     * @throws InterruptedException if the copy was interrupted
     * @return the byte array
     */
    public static byte[] readFileIntoByteArray(String fileName)
            throws IOException, InterruptedException
    {
        return StreamCopier.readByteArray(new File(fileName));
    }

    /**
     * Serializes the object into an array of bytes.
     * @param o The object to serialize.
     * @return An array of bytes that contiains the serialized object.
     * @throws java.io.IOException if something goes wrong.
     */
    public static byte[] serializeObject(Object o) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.flush();
        return baos.toByteArray();
    }

    /**
     * Reads a serialized object from the array of bytes.
     * @param bytes The array of bytes.
     * @return The unserialized object.
     * @throws java.io.IOException if there was a problem reading the input.
     * @throws ClassNotFoundException if the class of the object in the input was not found.
     */
    public static Object unserializeObject(byte[] bytes) throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return ois.readObject();
    }

    /**
     * Creates a new stream copier, that will copy the input stream into the
     * output stream when the run() method is caled.
     * @param    in     The input stream to read from.
     * @param out The output stream.  If this is null, the input will be
     * discarded, similar to piping to /dev/null on UN*X.
     */
    public StreamCopier(InputStream in, OutputStream out)
    {
        this.in = in;
        this.out = out;
    }

    /**
     * This method will copy the input into the output until there is no more
     * input.  Since this method is typically run by a thread, exceptions
     * are not thrown from it.  Instead, the exception can be read using
     * the getException() method.
     *
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see     Thread#run()
     */
    public void run()
    {
        try
        {
            // Copy, using the a buffer.
            StreamCopier.unsyncCopy(in, out, bufsz);
            // Flush the output.
            if (out != null)
                out.flush();
            // Completed state.
            synchronized (this)
            {
                complete = true;
            }
        }
        catch (IOException e)
        {
            // Remember the exception, just in case anyone cares.
            synchronized (this)
            {
                exception = e;
            }
        }
        catch (InterruptedException e)
        {
            // Ignore, just stop.
        }
        catch (Throwable e)
        {
            // Ignore.
        }
    }

    /**
     * Returns the exception thrown in the run() method, if any.
     * @return IOException  - The exception thrown during the run() method,
     * or null if there were no errors.
     */
    public IOException getException()
    {
        synchronized (this)
        {
            return exception;
        }
    }

    /**
     * Returns true if the copying is complete.
     * @return boolean - true if the copying is complete.  Returns false if
     * the copying is in progress, not started, or encountered an error.
     */
    public boolean isComplete()
    {
        synchronized (this)
        {
            return complete;
        }
    }
}
