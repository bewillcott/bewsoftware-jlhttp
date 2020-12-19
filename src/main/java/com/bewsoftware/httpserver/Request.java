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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static com.bewsoftware.httpserver.NetUtils.detectLocalHostName;
import static com.bewsoftware.httpserver.NetUtils.readHeaders;
import static com.bewsoftware.httpserver.Utils.parseParamsList;
import static com.bewsoftware.httpserver.Utils.parseRange;
import static com.bewsoftware.httpserver.Utils.parseULong;
import static com.bewsoftware.httpserver.Utils.split;
import static com.bewsoftware.httpserver.Utils.splitElements;
import static com.bewsoftware.httpserver.Utils.toMap;
import static com.bewsoftware.httpserver.Utils.trimDuplicates;

/**
 * The {@code Request} class encapsulates a single HTTP request.
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 1.0
 * @version 2.5.3
 */
public final class Request {

    protected URL baseURL; // cached value
    protected InputStream body;
    protected VirtualHost.ContextInfo context; // cached value
    protected Headers headers;
    protected VirtualHost host; // cached value
    protected String method;
    protected Map<String, String> params; // cached value
    protected HTTPServer server;
    protected URI uri;
    protected String version;

    /**
     * Constructs a Request from the data in the given input stream.
     *
     * @param in     the input stream from which the request is read
     * @param server The active server.
     *
     * @throws IOException if an error occurs
     */
    public Request(final InputStream in, final HTTPServer server) throws IOException {
        this.server = server;

        readRequestLine(in);
        headers = readHeaders(in);
        // RFC2616#3.6 - if "chunked" is used, it must be the last one
        // RFC2616#4.4 - if non-identity Transfer-Encoding is present,
        // it must either include "chunked" or close the connection after
        // the body, and in any case ignore Content-Length.
        // if there is no such Transfer-Encoding, use Content-Length
        // if neither header exists, there is no body
        String header = headers.get("Transfer-Encoding");

        if (header != null && !header.toLowerCase(Locale.US).equals("identity"))
        {
            if (Arrays.asList(splitElements(header, true)).contains("chunked"))
            {
                body = new ChunkedInputStream(in, headers);
            } else
            {
                body = in; // body ends when connection closes
            }
        } else
        {
            header = headers.get("Content-Length");
            long len = header == null ? 0 : parseULong(header, 10);
            body = new LimitedInputStream(in, len, false);
        }
    }

    /**
     * Returns the base URL (scheme, host and port) of the request resource.
     * The host name is taken from the request URI or the Host header or a
     * default host (see RFC2616#5.2).
     *
     * @return the jarPath URL of the requested resource, or null if it
     *         is malformed
     */
    public URL getBaseURL() {
        if (baseURL != null)
        {
            return baseURL;
        }

        // normalize host header
        String strHost = uri.getHost();

        if (strHost == null)
        {
            strHost = headers.get("Host");

            if (strHost == null) // missing in HTTP/1.0
            {
                strHost = detectLocalHostName();
            }
        }

        int pos = strHost.indexOf(':');
        strHost = pos < 0 ? strHost : strHost.substring(0, pos);

        try
        {
            return baseURL = new URL(server.secure ? "https" : "http", strHost, server.port, "");
        } catch (MalformedURLException ex)
        {
            return null;
        }
    }

    /**
     * Returns the input stream containing the request body.
     *
     * @return the input stream containing the request body
     */
    public InputStream getBody() {
        return body;
    }

    /**
     * Returns the info of the context handling this request.
     *
     * @return the info of the context handling this request, or an empty context
     */
    public VirtualHost.ContextInfo getContext() {
        return context != null ? context : (context = getVirtualHost().getContext(getPath()));
    }

    /**
     * Returns the request arrHeader.
     *
     * @return the request arrHeader
     */
    public Headers getHeaders() {
        return headers;
    }

    /**
     * Returns the request method.
     *
     * @return the request method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the request parameters, which are parsed both from the query
     * part of the request URI, and from the request body if its content
     * type is "application/x-www-form-urlencoded" (i.e. a submitted form).
     * UTF-8 encoding is assumed in both cases.
     * <p>
     * For multivalued parameters (i.e. multiple parameters with the same
     * name), only the first one is considered. For access to all values,
     * use {@link #getParamsList()} instead.
     * <p>
     * The map iteration retains the original order of the parameters.
     *
     * @return the request parameters name-value pairs,
     *         or an empty map if there are none
     *
     * @throws IOException if an error occurs
     * @see #getParamsList()
     */
    public Map<String, String> getParams() throws IOException {
        if (params == null)
        {
            params = toMap(getParamsList());
        }

        return params;
    }

    /**
     * Returns the request parameters, which are parsed both from the query
     * part of the request URI, and from the request body if its content
     * type is "application/x-www-form-urlencoded" (i.e. a submitted form).
     * UTF-8 encoding is assumed in both cases.
     * <p>
     * The parameters are returned as a list of string arrays, each containing
     * the parameter name as the first element and its corresponding value
     * as the second element (or an empty string if there is no value).
     * <p>
     * The list retains the original order of the parameters.
     *
     * @return the request parameters name-value pairs,
     *         or an empty list if there are none
     *
     * @throws IOException if an error occurs
     * @see Utils#parseParamsList(String)
     */
    public List<String[]> getParamsList() throws IOException {
        List<String[]> queryParams = parseParamsList(uri.getRawQuery());
        List<String[]> bodyParams = Collections.emptyList();
        String ct = headers.get("Content-Type");

        if (ct != null && ct.toLowerCase(Locale.US).startsWith("application/x-www-form-urlencoded"))
        {
            bodyParams = parseParamsList(FileUtils.readToken(body, -1, "UTF-8", 2097152)); // 2MB limit
        }

        if (bodyParams.isEmpty())
        {
            return queryParams;
        }

        if (queryParams.isEmpty())
        {
            return bodyParams;
        }

        queryParams.addAll(bodyParams);
        return queryParams;
    }

    /**
     * Returns the path component of the request URI, after
     * URL decoding has been applied (using the UTF-8 charset).
     *
     * @return the decoded path component of the request URI
     */
    public String getPath() {
        return uri.getPath();
    }

    /**
     * Sets the path component of the request URI. This can be useful
     * in URL rewriting, etc.
     *
     * @param path the path to set
     *
     * @throws IllegalArgumentException if the given path is malformed
     */
    public void setPath(String path) {
        try
        {
            uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
                          trimDuplicates(path, '/'), uri.getQuery(), uri.getFragment());
            context = null; // clear cached context so it will be recalculated
        } catch (URISyntaxException use)
        {
            throw new IllegalArgumentException("error setting path", use);
        }
    }

    /**
     * Returns the absolute (zero-based) content range value read
     * from the Range header. If multiple ranges are requested, a single
     * range containing all of them is returned.
     *
     * @param length the full length of the requested resource
     *
     * @return the requested range, or null if the Range header
     *         is missing or invalid
     */
    public long[] getRange(long length) {
        String header = headers.get("Range");
        return header == null || !header.startsWith("bytes=")
               ? null : parseRange(header.substring(6), length);
    }

    /**
     * Returns the request URI.
     *
     * @return the request URI
     */
    public URI getURI() {
        return uri;
    }

    /**
     * Returns the request version string.
     *
     * @return the request version string
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the virtual host corresponding to the requested host name,
     * or the default host if none exists.
     *
     * @return the virtual host corresponding to the requested host name,
     *         or the default virtual host
     */
    public VirtualHost getVirtualHost() {
        return host != null ? host
               : (host = server.getVirtualHost(getBaseURL().getHost())) != null ? host
                 : (host = server.getVirtualHost(null));
    }

    @Override
    public String toString() {
        return "Request{"
               + "\nbaseURL=" + baseURL + ", "
               + "\ncontext=" + context + ", "
               + "\nheaders=" + headers + ", "
               + "\nhost=" + host + ", "
               + "\nmethod=" + method + ", "
               + "\nparams=" + params + ", "
               + "\nserver=" + server + ", "
               + "\nuri=" + uri + ", "
               + "\nversion=" + version
               + "\n}";
    }

    /**
     * Reads the request line, parsing the method, URI and version string.
     *
     * @param in the input stream from which the request line is read
     *
     * @throws IOException if an error occurs or the request line is invalid
     */
    protected void readRequestLine(InputStream in) throws IOException {
        // RFC2616#4.1: should accept empty lines before request line
        // RFC2616#19.3: tolerate additional whitespace between tokens
        String line;

        try
        {
            do
            {
                line = FileUtils.readLine(in);
            } while (line.length() == 0);
        } catch (IOException ioe)
        { // if EOF, timeout etc.
            throw new IOException("missing request line"); // signal that the request did not begin
        }

        String[] tokens = split(line, " ", -1);

        if (tokens.length != 3)
        {
            throw new IOException("invalid request line: \"" + line + "\"");
        }

        try
        {
            method = tokens[0];
            // must remove '//' prefix which constructor parses as host name
            uri = new URI(trimDuplicates(tokens[1], '/'));
            version = tokens[2]; // RFC2616#2.1: allow implied LWS; RFC7230#3.1.1: disallow it
        } catch (URISyntaxException use)
        {
            throw new IOException("invalid URI: " + use.getMessage());
        }
    }
}
