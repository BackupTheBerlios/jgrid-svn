package org.jgrid.httpd;

import org.apache.log4j.Logger;

import java.net.ServerSocket;
import java.net.SocketException;
import java.net.Socket;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Basic server side TCP socket listener.
 * <br>User: Joshua Davis
 * Date: Jan 17, 2006
 * Time: 8:35:48 AM
 */
public abstract class SocketListener implements Runnable {
    private static Logger log = Logger.getLogger(SocketListener.class);

    /**
     * The default for the maximum number of connections.
     */
    public final static int DEFAULT_MAX_CONNECTIONS = 16;

    public static final int WAIT_TIMEOUT = 5000;

    /**
     * The default conneciton timeout.
     */
    public final static int DEFAULT_CONNECTION_TIMEOUT = 900000;

    /**
     * The port the server will listen on. *
     */
    private int port;

    /**
     * The server socket that the proxy will listen on. *
     */
    private ServerSocket socket;

    /**
     * The thread that is being used for the listener. *
     */
    private Thread thread;


    /**
     * Active connections to clients. *
     */
    private final ArrayList clientConnections = new ArrayList();

    /**
     * The maximum number of connections the server will accept. *
     */
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;

    /**
     * True if a shutdown has been requested. *
     */
    private boolean shutdownRequested = false;

    /**
     * The IP address filter. *
     */
    private IPAddressFilter addressFilter = new IPAddressFilter();
    /**
     * True if IP address filtering is enabled (a.k.a. paranoid mode). *
     */
    private boolean addressFilteringEnabled = false;
    /**
     * True if clients should be automatically disconnected when
     * the maximum number of client connections is reached.  False
     * will cause the server to wait until  there is an available connection
     * (a.k.a. 'rude mode').*
     */
    private boolean rejectIfUnavailable = false;
    /**
     * The socket timeout for client connections. *
     */
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;


    // -- Server statistics --
    private int connectionsAccepted = 0;
    private int connectionsRejected = 0;

    public SocketListener(int port) throws IOException {
        initialize(new ServerSocket(port), port);
    }

    public void initialize(ServerSocket socket, int port) {
        this.socket = socket;
        this.port = port;
    }
    
    public SocketListener() {
    }

    /**
     * Returns the port number this server is listening on.
     *
     * @return The port number this server is listening on.
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the maximum number of connections allowed by this server.
     *
     * @return the maximum number of connections allowed by this server.
     */
    public int getMaxConnections() {
        synchronized (this) {
            return maxConnections;
        }
    }

    /**
     * Sets the maximum number of active client connections the server
     * will handle.
     *
     * @param maxConnections The maximum number of active client connections.
     */
    public void setMaxConnections(int maxConnections) {
        synchronized (this) {
            this.maxConnections = maxConnections;
        }
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    public void run() {
        log.info("Listening on port " + this.port + "...");
        if (thread != null)
            throw new Error("Wrong thread!");
        while (!shutdownRequested) {
            thread = Thread.currentThread();
            try {
                boolean reject = false;
                synchronized (clientConnections) {
                    while ((clientConnections.size() > maxConnections)
                            && (!shutdownRequested)) {
                        // If 'rude mode' is enabled, kick the client.
                        // Don't bother looping either.
                        if (isRejectIfUnavailable()) {
                            reject = true;
                            break;
                        }
                        // Otherwise, wait...
                        else {
                            log.info("Client connection limit reached, waiting...");
                            try {
                                clientConnections.wait(WAIT_TIMEOUT);
                            }
                            catch (InterruptedException e) {
                                log.error(e, e);
                            }
                        }
                    } // while
                } // synchronized

                if (log.isDebugEnabled())
                    log.debug("run() : Listening...");
                // Accept a socket connection...
                Socket incoming = socket.accept();

                if (log.isDebugEnabled())
                    log.debug("run() : Socket accepted.");

                // If we're in 'rude mode', reject it.
                if (reject) {
                    incoming.close();
                    synchronized (this) {
                        connectionsRejected++;
                    }
                    log.error("Socket connection from " + incoming.getInetAddress().getAddress() + " rejected!");
                    continue;   // Continue accepting other connections.
                }

                synchronized (this) {
                    if (addressFilteringEnabled)       // If paranoid, check the IP address...
                    {
                        if (log.isDebugEnabled())
                            log.debug("Checking socket IP address...");
                        boolean allowed = addressFilter.checkSocket(incoming);
                        if (!allowed)   // If this is a 'bad' IP, then...
                        {               // kick this client, log the fact, and continue.
                            incoming.close();
                            connectionsRejected++;
                            log.error("Socket connection from " + incoming.getInetAddress().getAddress() + " denied!");
                            continue;   // Continue accepting other connections.
                        }
                    }
                }
                // Accept this client.
                doAccept(incoming);
            }
            catch (SocketException e) {
/* 2004-02-25 [jsd] In JDK1.3, Socket doesn't have the isClosed() method.
                if (socket.isClosed() && shutdownRequested)
                    log.info("Listener on port " + port + ", server socket closed.");
                else
                    unexpected(e);
*/
                if (shutdownRequested)
                    log.info("Listener on port " + port + ", server socket closed.");
                else
                    log.error(e, e);
                break;

            }
            catch (IOException e) {
                log.error(e, e);
                break;
            }
        }
        shutdownRequested = false;
        thread = null;
        log.info("Stopped (port " + this.port + ")");
    }

    /**
     * Returns the number of connections accepted so far.
     *
     * @return The number of connections accepted by the server.
     */
    public int getConnectionsAccepted() {
        synchronized (this) {
            return connectionsAccepted;
        }
    }

    /**
     * Returns the number of connections rejected so far.
     *
     * @return The number of connections rejected by the server.
     */
    public int getConnectionsRejected() {
        synchronized (this) {
            return connectionsRejected;
        }
    }

    /**
     * Stops the server, and any active clients.
     */
    public void shutdown() {
        synchronized (clientConnections) {
            for (Iterator iterator = clientConnections.iterator(); iterator.hasNext();) {
                ClientConnection clientConnection = (ClientConnection) iterator.next();
                try {
                    clientConnection.shutdown();
                }
                catch (Throwable e) {
                    log.error(e, e);
                }
            }
        }
        clearAllClients();
        shutdownRequested = true;
        try {
            socket.close();
        }
        catch (IOException ioex) {
            log.error(ioex, ioex);
        }
    }

    /**
     * Add an IP address to the list of allowed or denied addresses.  This also
     * activates 'paranoid mode'.
     *
     * @param pattern The IP address pattern.
     * @param accept  True to add an allowed addres, false to add a denied
     *                address.
     * @see IPAddressPattern
     */
    public void addAddressPattern(IPAddressPattern pattern, boolean accept) {
        synchronized (this) {
            if (addressFilter == null)
                addressFilter = new IPAddressFilter();
            addressFilter.add(pattern, accept);
        }
    }

    /**
     * Returns true if IP address filtering is enabled.
     *
     * @return true if IP address filtering is enabled.
     */
    public boolean isAddressFilteringEnabled() {
        synchronized (this) {
            return addressFilteringEnabled;
        }
    }

    /**
     * Enables or disables IP address filtering (a.k.a. paranoid mode).
     *
     * @param addressFilteringEnabled True to enable IP address filtering, false to disable it.
     */
    public void setAddressFilteringEnabled(boolean addressFilteringEnabled) {
        synchronized (this) {
            this.addressFilteringEnabled = addressFilteringEnabled;
        }
    }

    /**
     * Returns true if client connections should be closed if the maximum
     * number of clients has been reached.
     *
     * @return true if client connections should be closed if the maximum
     *         number of clients has been reached.
     */
    public boolean isRejectIfUnavailable() {
        synchronized (this) {
            return rejectIfUnavailable;
        }
    }

    /**
     * Enables or disables 'rude' treatment of clients when the maximum number
     * of clients has been reached.  If enabled, incoming client connections
     * will be closed when the maximum number of clients has been reached.
     *
     * @param rejectIfUnavailable
     */
    public void setRejectIfUnavailable(boolean rejectIfUnavailable) {
        synchronized (this) {
            this.rejectIfUnavailable = rejectIfUnavailable;
        }
    }

    /**
     * Clients are required to call this method when they shut down.
     *
     * @param client The client that has stopped.
     */
    public void clientClosed(ClientConnection client) {
        removeClient(client);
    }

    /**
     * Returns the client socket connection timeout.
     *
     * @return The client socket connection timeout.
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int timeout) {
        this.connectionTimeout = timeout;
    }

    /**
     * Accept the incoming connection and create a client connection object.
     *
     * @param incoming The incoming socket.
     * @return A new client connection object.
     * @throws IOException if something goes wrong.
     */
    protected abstract ClientConnection acceptClient(Socket incoming) throws IOException;

    private void doAccept(Socket incoming) throws IOException {
        ClientConnection client = acceptClient(incoming);
        if (client == null) {
            incoming.close();
            log.info("Client rejected: acceptClient() returned null.");
            return;
        }
        synchronized (this) {
            connectionsAccepted++;
        }

        // Add the client to the list before initializing.
        addClient(client);

        try {
            // NOTE: The client *must not* start any threads until this method is called!
            client.initialize(this);
            client.start(); // GO!
            if (log.isDebugEnabled())
                log.debug("doAccept() : Client connection handler started.");
        }
        catch (Throwable t) {
            log.error(t, t);

            try {
                removeClient(client);
                client.close();
            }
            catch (Throwable e) {
                log.error(e, e);
            }
        }
    }

    private void clearAllClients() {
        synchronized (clientConnections) {
            clientConnections.clear();
            clientConnections.notifyAll();
        }
    }

    private boolean removeClient(ClientConnection client) {
        synchronized (clientConnections) {
            boolean found = clientConnections.remove(client);
            if (found)
                log.info("Client connection " + client + " removed.");
            clientConnections.notifyAll();
            return found;
        }
    }

    private void addClient(ClientConnection client) {
        synchronized (clientConnections) {
            clientConnections.add(client);
            log.info("Client connection " + client + " added.");
            clientConnections.notifyAll();
        }
    }
}
