package org.jgrid.impl;

import org.jgrid.httpd.HTTPDaemon;
import org.jgrid.WebServer;

import java.io.IOException;

/**
 * The embedded web server for the grid node.
 * <br>User: Joshua Davis
 * Date: Feb 28, 2006
 * Time: 6:29:35 AM
 */
public class WebServerImpl extends HTTPDaemon implements WebServer
{
    public WebServerImpl() throws IOException
    {
        super();
    }
}
