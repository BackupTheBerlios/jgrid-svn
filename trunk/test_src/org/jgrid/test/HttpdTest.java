package org.jgrid.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;
import org.jgrid.httpd.WebServer;
import org.jgrid.util.Copier;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Tests the built-in web server.
 * <br>User: Joshua Davis
 * Date: Jan 20, 2006
 * Time: 7:34:39 AM
 */
public class HttpdTest extends TestCase {
    private static Logger log = Logger.getLogger(HttpdTest.class);

    public HttpdTest(String n) {
        super(n);
    }

    public void testWebServer() throws Exception {
        WebServer server = new WebServer(8666);
        Thread serverThread = new Thread(server, "WebServer");
        try {
            serverThread.start();
            HttpURLConnection con = (HttpURLConnection) new URL("http://localhost:8666").openConnection();
            con.connect();
            log.info("response code: " + con.getResponseCode());
            InputStream in = con.getInputStream();
            byte[] bytes = Copier.toByteArray(in);
            log.info("bytes.length = " + bytes.length);
            con.disconnect();
            log.info("disconnected");
        } finally {
            server.shutdown();
            serverThread.join();
        }
    }

    public static Test suite() {
        return new TestSuite(HttpdTest.class);
    }
}
