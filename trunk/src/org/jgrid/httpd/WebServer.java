package org.jgrid.httpd;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.Socket;

/**
 * A web server, embedded in Client and Server nodes.  This allows node/node bulk data transfer using HTTP,
 * as well as class loading using URLClassLoader.
 * <br>User: Joshua Davis
 * Date: Jan 17, 2006
 * Time: 8:31:37 AM
 */
public class WebServer extends SocketListener {
    /**
     * A logger for this class. *
     */
    private static Logger log = Logger.getLogger(WebServer.class);

    /**
     * The default server socket timeout for HTTPConstants servers.
     */
    public static final int DEFAULT_HTTP_TIMEOUT = 30000;
    /**
     * The default maximum number of connections for an HTTPConstants server.
     */
    public static final int DEFAULT_HTTP_CONNECTIONS = 32;

    private boolean keepAlive = true;

    /**
     * Creates a Web server at the specified port number.
     */
    public WebServer(int port) throws IOException {
        super(port);
        setConnectionTimeout(DEFAULT_HTTP_TIMEOUT);
        setMaxConnections(DEFAULT_HTTP_CONNECTIONS);
    }

    /**
     * Accept the incoming connection and create a client connection object.
     *
     * @param incoming The incoming socket.
     * @return A new client connection object.
     * @throws java.io.IOException if something goes wrong.
     */
    protected ClientConnection acceptClient(Socket incoming) throws IOException {
        return new HTTPClientConnection(this, incoming);
    }

    /**
     * Returns true if the server should support keep-alive connections.
     */
    public boolean getKeepAlive() {
        return keepAlive;
    }

    /**
     * Enable / Disable HTTPConstants keep alive support for the server.
     */
    public void setKeepAlive(boolean flag) {
        keepAlive = true;
    }

    /**
     * Handle an unexpected exception.
     *
     * @param t The unexpected exception.
     */
    protected void unexpected(Throwable t) {
        log.error(t, t);
    }
}
