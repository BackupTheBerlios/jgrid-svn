/*******************************************************************************
 * $Id: HTTPClientConnection.java,v 1.1 2004/02/05 12:22:57 pgmjsd Exp $
 * $Author: pgmjsd $
 * $Date: 2004/02/05 12:22:57 $
 *
 * Copyright 2002-2003  YAJUL Developers, Joshua Davis, Kent Vogel.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 ******************************************************************************/
package org.jgrid.httpd;

import org.apache.log4j.Logger;
import org.jgrid.util.Copier;

import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * A server side connection that handles incoming HTTPConstants requests.
 * User: josh
 * Date: Jan 17, 2004
 * Time: 11:31:38 AM
 */
public class HTTPClientConnection extends ClientConnection implements Runnable {
    public static final byte[] CONTENT_TYPE_HEADER = "Content-Type: ".getBytes();
    public static final byte[] CONTENT_TYPE_HTML = "text/html".getBytes();
    public static final byte[] CONTENT_TYPE_XML = "text/xml".getBytes();

    public static final byte[] CRLF = "\r\n".getBytes();
    public static final byte[] DOUBLE_CRLF = "\r\n\r\n".getBytes();

    public static final byte[] CONTENT_LENGTH_HEADER = "Content-Length: ".getBytes();
    public static final byte[] CONNECTION_KEEP = "Connection: Keep-Alive\r\n".getBytes();
    public static final byte[] CONNECTION_CLOSE = "Connection: close\r\n".getBytes();
    public static final byte[] STATUS_OK = " 200 OK\r\n".getBytes();
    public static final byte[] SERVER = "Server: JEGrid Mini HTTP Server 1.0\r\n".getBytes();
    public static final int HEADER_SIZE = 512;

    /**
     * A logger for this class. *
     */
    private static Logger log = Logger.getLogger(HTTPClientConnection.class);
    /**
     * A pointer back to the web server that is managing this connection *
     */
    private HTTPDaemon server;
    /**
     * True if this connection is in 'keep-alive' mode. *
     */
    private boolean keepalive = false;

    private static final byte[] BAD_REQUEST = " 400 Bad Request\r\n".getBytes();

    public HTTPClientConnection(HTTPDaemon server, Socket socket)
            throws IOException {
        super(server, socket);
        this.server = server;
        addStreamBuffers();                   // Use simple stream buffers.
    }

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {
        if (log.isDebugEnabled())
            log.debug("run() : ENTER");
        setKeepalive(false);
        boolean waiting = true;

        try {
            do  // Keep-alive loop...
            {
                waiting = true;                 // Waiting for a request...

                if (log.isDebugEnabled())
                    log.debug("run() : Reading request...");

                // Read the request.
                HTTPInputStream in = new HTTPInputStream(getInputStream());

                RequestHeader request = new RequestHeader();
                request.read(in); // Read the request headers.
                if (request.getStartLine() == null || request.getStartLine().length() == 0) {
                    if (log.isDebugEnabled())
                        log.debug("run() : No start line, client probably disconnected.");
                    break;
                }
                try {
                    processRequest(request);
                }
                catch (Exception ex) {
                    log.error("Exception while processing request:" + ex.getMessage(), ex);
                }
                if (isKeepalive())
                    log.debug("Keep-alive specified, re-using connection.");
            }
            while (isKeepalive());
        }
        catch (SocketException se) {
            // Socket exceptions are okay while waiting for
            // requests from the client.  If we weren't waiting for
            // a request, log the exception as an error.
            if (!waiting)
                log.error(se, se);
        }
        catch (Exception exception) {
            log.error(exception, exception);
        }
        finally {
            // Tell the superclass to close the socket.
            close();
            if (log.isDebugEnabled())
                log.debug("run() : LEAVE");
        }
    }

    protected HTTPResponse createResponse(RequestHeader request) {
        if (log.isDebugEnabled())
            log.debug("createResponse()");
        HTTPResponse response = new HTTPResponse();
        response.getPrintStream().print("<html><body><h1>Error!</h1><p>No handler for this request.</p><pre>" + request.getStartLine() + "</pre></body></html>");
        response.setStatus(200);
        return response;
    }

    protected void processRequest(RequestHeader request) throws IOException {
        if (log.isDebugEnabled())
            log.debug("processRequest() : " + request.toString());

        // In keep alive mode, if the client is HTTP v1.1 and the server
        // is permitting keep alive.  If there is no 'Connection:'
        // header, this will be the default.
        setKeepalive(server.getKeepAlive() && "HTTP/1.1".equals(request.getHttpVersion()));

        // Look for a keep-alive token in the connection header.  This
        // might disable keep-alive.
        setKeepalive(server.getKeepAlive() && request.isKeepAlive());
        HTTPResponse response = createResponse(request);
        writeHTTPResponse(response, request);
    }

    protected void writeHTTPResponse(HTTPResponse response, RequestHeader request) throws IOException {
        if (log.isDebugEnabled())
            log.debug("writeHTTPResponse()");
        int status = response.getStatus();
        byte[] contentType = CONTENT_TYPE_HTML;
        OutputStream output = getOutputStream();
        if (status == 400) {
            output.write(request.getHttpVersion().getBytes());
            output.write(BAD_REQUEST);
            output.write(SERVER);
            output.write(CRLF);
            output.write(("Method " + request.getMethod() + " not implemented").getBytes());
            output.flush();
            setKeepalive(false);
        } else if (status == 200) {
            output.write(request.getHttpVersion().getBytes());
            output.write(STATUS_OK);
            output.write(SERVER);
            if (isKeepalive())
                output.write(CONNECTION_KEEP);
            else
                output.write(CONNECTION_CLOSE);
            output.write(CONTENT_TYPE_HEADER);
            output.write(contentType);
            output.write(CRLF);
            output.write(CONTENT_LENGTH_HEADER);
            output.write(Integer.toString(response.getContentLength()).getBytes());
            output.write(DOUBLE_CRLF);
            if (log.isDebugEnabled())
                log.debug("writing content...");
            int bytes = Copier.copy(response.getInputStream(), output, Copier.DEFAULT_BUFFER_SIZE, Copier.UNLIMITED);
            if (log.isDebugEnabled())
                log.debug("" + bytes + " bytes written");
            output.flush();
            if (log.isDebugEnabled())
                log.debug("output stream flushed");
        } else {
            output.write(request.getHttpVersion().getBytes());
            output.write(" 500 Server Error".getBytes());
            output.write(SERVER);
            setKeepalive(false);
        }
    }

    public void close() {
        log.info("close()");
        super.close();
    }

    public boolean isKeepalive() {
        return keepalive;
    }

    public void setKeepalive(boolean keepalive) {
        this.keepalive = keepalive;
    }
}

