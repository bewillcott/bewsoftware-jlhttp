/*
 *  Copyright © 2005-2019 Amichai Rothman
 *  Copyright © 2020 Bradley Willcott
 *
 *  This file is part of JLHTTP - the Java Lightweight HTTP Server.
 *
 *  JLHTTP is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  JLHTTP is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with JLHTTP.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  For additional info see http://www.freeutils.net/source/jlhttp/
 */
package com.bewsoftware.httpserver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.bewsoftware.httpserver.NetUtils.readHeaders;
import static com.bewsoftware.httpserver.Utils.getBytes;

/**
 * The {@code MultipartIterator} iterates over the parts of a multipart/form-data request.
 * <p>
 * For example, to support file upload from a web browser:
 * <ol>
 * <li>Create an HTML form which includes an input field of type "file", attributes
 * method="post" and enctype="multipart/form-data", and an action URL of your choice,
 * for example action="/upload". This form can be served normally like any other
 * resource, e.g. from an HTML file on disk.
 * <li>Add a context handler for the action path ("/upload" in this example), using either
 * the explicit {@link VirtualHost#addContext} method or the {@link Context} annotation.
 * <li>In the context handler implementation, construct a {@code MultipartIterator} from
 * the client {@code Request}.
 * <li>Iterate over the form {@link Part}s, processing each named field as appropriate -
 * for the file input field, read the uploaded file using the body input stream.
 * </ol>
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 1.0
 * @version 2.5.3
 */
public class MultipartIterator implements Iterator<MultipartIterator.Part> {

    protected final MultipartInputStream in;
    protected boolean next;

    /**
     * Creates a new MultipartIterator from the given request.
     *
     * @param req the multipart/form-data request
     *
     * @throws IOException              if an IO error occurs
     * @throws IllegalArgumentException if the given request's content type
     *                                  is not multipart/form-data, or is missing the boundary
     */
    public MultipartIterator(Request req) throws IOException {
        Map<String, String> ct = req.getHeaders().getParams("Content-Type");

        if (!ct.containsKey("multipart/form-data"))
        {
            throw new IllegalArgumentException("Content-Type is not multipart/form-data");
        }

        String boundary = ct.get("boundary"); // should be US-ASCII

        if (boundary == null)
        {
            throw new IllegalArgumentException("Content-Type is missing boundary");
        }

        in = new MultipartInputStream(req.getBody(), getBytes(boundary));
    }

    @Override
    public boolean hasNext() {
        try
        {
            return next || (next = in.nextPart());
        } catch (IOException ioe)
        {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public Part next() {
        if (!hasNext())
        {
            throw new NoSuchElementException();
        }

        next = false;
        Part p = new Part();

        try
        {
            p.headers = readHeaders(in);
        } catch (IOException ioe)
        {
            throw new RuntimeException(ioe);
        }

        Map<String, String> cd = p.headers.getParams("Content-Disposition");
        p.name = cd.get("name");
        p.filename = cd.get("filename");
        p.body = in;
        return p;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * The {@code Part} class encapsulates a single part of the multipart.
     */
    public static class Part {

        /**
         * The part's body (form field value).
         */
        public InputStream body;

        /**
         * The part's filename (original filename entered in file form field).
         */
        public String filename;

        /**
         * The part's Headers.
         */
        public Headers headers;
        /**
         * The part's name (form field name).
         */
        public String name;

        /**
         * *
         * Returns the part's body as a string. If the part
         * headers do not specify a charset, UTF-8 is used.
         *
         * @return the part's body as a string
         *
         * @throws IOException if an IO error occurs
         */
        public String getString() throws IOException {
            String charset = headers.getParams("Content-Type").get("charset");
            charset = charset == null ? "UTF-8" : charset;
            return FileUtils.readToken(body, -1, charset, 8192);
        }
    }
}
