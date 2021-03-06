/*******************************************************************************
 * $Id: HTTPResponse.java,v 1.1 2004/02/05 12:22:57 pgmjsd Exp $
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

import java.io.PrintStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * The web server uses this as a response.
 * <br>User: josh
 * Date: Jan 24, 2004
 * Time: 4:38:21 PM
 */
public class HTTPResponse {
    private int status;
    private InputStream inputStream;
    private PrintStream printStream;
    private ByteArrayOutputStream baos;

    public HTTPResponse() {
        baos = new ByteArrayOutputStream();
        printStream = new PrintStream(baos);
    }

    public PrintStream getPrintStream() {
        return printStream;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getContentLength() {
        return baos.size();
    }

    public InputStream getInputStream() {
        if (baos != null && inputStream == null) {
            byte[] bytes = baos.toByteArray();
            inputStream = new ByteArrayInputStream(bytes);
        }
        return inputStream;
    }
}
