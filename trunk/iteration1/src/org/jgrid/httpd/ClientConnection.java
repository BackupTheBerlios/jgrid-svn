package org.jgrid.httpd;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;

/**
 * Represents a connection accepted by an ServerSocketListener
 * <br>User: jdavis
 * Date: Dec 11, 2003
 * Time: 11:46:47 AM
 *
 * @author jdavis
 */
public abstract class ClientConnection {

    private static Logger log = Logger.getLogger(ClientConnection.class);

    /**
     * The default buffer size for input and output streams.
     */
    public final static int DEFAULT_BUFFER_SIZE = 1024;

    private SocketListener listener;
    private Socket socket;

    /**
     * The response stream, server->client.
     */
    private OutputStream out;

    /**
     * The request stream, client->server.
     */
    private InputStream in;

    /**
     * True if the input and output streams are being buffered.
     */
    private boolean buffered;

    public ClientConnection(SocketListener listener,
                            Socket socket) throws IOException {
        this.listener = listener;
        this.socket = socket;

        socket.setSoTimeout(listener.getConnectionTimeout());

        // Get the streams from the socket while we're still on the listener
        // thread.
        in = socket.getInputStream();
        out = socket.getOutputStream();
    }


    public void initialize(SocketListener listener) {
        this.listener = listener;
    }

    /**
     * Implementors will obtain threads and start running.
     */
    public abstract void start();

    /**
     * Notifies the server that this connection has been closed.
     */
    public void onClose() {
        listener.clientClosed(this);
    }

    public void close() {
        if (out != null) {
            try {
                out.close();
            }
            catch (IOException e) {
                log.error(e, e);
            }
            out = null;
        }
        if (in != null) {
            try {
                in.close();
            }
            catch (IOException e) {
                log.error(e, e);
            }
            in = null;
        }
        if (socket != null) {
            try {
                socket.close();
            }
            catch (IOException e) {
                log.error(e, e);
            }
            socket = null;
        }
        onClose();
    }


    protected Socket usurpSocket() {
        Socket rv = socket;
        flushOutputStream();
        if (rv == null)
            throw new IllegalStateException("There is no socket to usurp!");
        // Set the socket and the input streams to null, so they won't get closed.
        socket = null;
        in = null;
        out = null;
        // Close this connection.
        close();
        // Return the socket.
        return rv;
    }

    /**
     * Flushes the output stream, if it is buffered.
     */
    private void flushOutputStream() {
        // If buffered, flush the buffers.
        if (buffered) {
            if (out != null)
                try {
                    out.flush();
                }
                catch (IOException e) {
                    log.error(e, e);
                }
        }
    }

    protected InputStream getInputStream() {
        return in;
    }

    protected OutputStream getOutputStream() {
        return out;
    }

    /**
     * Create simple buffers on the input and output streams.  Invoke this from
     * either the constructor or the 'run()' method to add buffering (recommended).
     */
    protected void addStreamBuffers() {
        if (!buffered)  // Don't buffer twice.
        {
            in = new BufferedInputStream(in, DEFAULT_BUFFER_SIZE);
            out = new BufferedOutputStream(out, DEFAULT_BUFFER_SIZE);
            buffered = true;
        }
    }


    /**
     * Returns the incoming client socket.
     *
     * @return The socket.
     */
    protected Socket getSocket() {
        return socket;
    }

    /**
     * Returns the server listener that is managing this client connection.
     *
     * @return The server listener.
     */
    protected SocketListener getListener() {
        return listener;
    }

    /**
     * Stops and closes the client connection.
     */
    public void shutdown() {
        log.info("shutdown()");
    }
}
