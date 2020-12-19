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

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.bewsoftware.httpserver.FileUtils.createIndex;
import static com.bewsoftware.httpserver.HTTPServer.CRLF;
import static com.bewsoftware.httpserver.HTTPServer.getContentType;
import static com.bewsoftware.httpserver.Utils.formatDate;
import static com.bewsoftware.httpserver.Utils.getBytes;
import static com.bewsoftware.httpserver.Utils.match;
import static com.bewsoftware.httpserver.Utils.splitElements;
import static java.lang.String.join;

/**
 * NetUtils class contains helper methods from the original HTTPServer class.
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 2.5.3
 * @version 2.5.3
 */
public class NetUtils {

    /**
     * Returns the local host's auto-detected name.
     *
     * @return the local host name
     */
    public static String detectLocalHostName() {
        try
        {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException uhe)
        {
            return "localhost";
        }
    }

    /**
     * Calculates the appropriate response status for the given request and
     * its resource's last-modified time and ETag, based on the conditional
     * headers present in the request.
     *
     * @param req          the request
     * @param lastModified the resource's last modified time
     * @param etag         the resource's ETag
     *
     * @return the appropriate response status for the request
     */
    public static int getConditionalStatus(Request req, long lastModified, String etag) {
        Headers headers = req.getHeaders();
        // If-Match
        String header = headers.get("If-Match");

        if (header != null && !match(true, splitElements(header, false), etag))
        {
            return 412;
        }

        // If-Unmodified-Since
        Date date = headers.getDate("If-Unmodified-Since");
        if (date != null && lastModified > date.getTime())
        {
            return 412;
        }
        // If-Modified-Since
        int status = 200;
        boolean force = false;
        date = headers.getDate("If-Modified-Since");

        if (date != null && date.getTime() <= System.currentTimeMillis())
        {
            if (lastModified > date.getTime())
            {
                force = true;
            } else
            {
                status = 304;
            }
        }

        // If-None-Match
        header = headers.get("If-None-Match");

        if (header != null)
        {
            if (match(false, splitElements(header, false), etag)) // RFC7232#3.2: use weak matching
            {
                status = req.getMethod().equals("GET")
                         || req.getMethod().equals("HEAD") ? 304 : 412;
            } else
            {
                force = true;
            }
        }

        return force ? 200 : status;
    }

    /**
     * Handles a TRACE method request.
     *
     * @param req  the request
     * @param resp the response into which the content is written
     *
     * @throws IOException if an error occurs
     */
    public static void handleTrace(Request req, Response resp) throws IOException {
        resp.sendHeaders(200, -1, -1, null, "message/http", null);
        OutputStream out = resp.getBody();
        out.write(getBytes("TRACE ", req.getURI().toString(), " ", req.getVersion()));
        out.write(CRLF);
        req.getHeaders().writeTo(out);
        FileUtils.transfer(req.getBody(), out, -1);
    }

    /**
     * Reads headers from the given stream. Headers are read according to the
     * RFC, including folded headers, element lists, and multiple headers
     * (which are concatenated into a single element list header).
     * Leading and trailing whitespace is removed.
     *
     * @param in the stream from which the arrHeader are read
     *
     * @return the read arrHeader (possibly empty, if none exist)
     *
     * @throws IOException if an IO error occurs or the arrHeader are malformed
     *                     or there are more than 100 header lines
     */
    public static Headers readHeaders(InputStream in) throws IOException {
        Headers headers = new Headers();
        String line;
        String prevLine = "";
        int count = 0;

        while ((line = FileUtils.readLine(in)).length() > 0)
        {
            int start; // start of line data (after whitespace)

            for (start = 0; start < line.length()
                            && Character.isWhitespace(line.charAt(start)); start++);

            if (start > 0) // unfold header continuation line
            {
                line = prevLine + ' ' + line.substring(start);
            }

            int separator = line.indexOf(':');

            if (separator < 0)
            {
                throw new IOException("invalid header: \"" + line + "\"");
            }

            String name = line.substring(0, separator);
            String value = line.substring(separator + 1).trim(); // ignore LWS
            Header replaced = headers.replace(name, value);

            // concatenate repeated arrHeader (distinguishing repeated from folded)
            if (replaced != null && start == 0)
            {
                value = replaced.getValue() + ", " + value;
                line = name + ": " + value;
                headers.replace(name, value);
            }

            prevLine = line;

            if (++count > 100)
            {
                throw new IOException("too many header lines");
            }
        }
        return headers;
    }

    /**
     * Serves a context's contents from a file based resource.
     * <p>
     * The file is located by stripping the given context prefix from
     * the request's path, and appending the result to the given jarPath directory.
     * <p>
     * Missing, forbidden and otherwise invalid files return the appropriate
     * error response. Directories are served as an HTML index page if the
     * virtual host allows one, or a forbidden error otherwise. Files are
     * sent with their corresponding content types, and handle conditional
     * and partial retrievals according to the RFC.
     *
     * @param base    the jarPath directory to which the context is mapped
     * @param context the context which is mapped to the jarPath directory
     * @param req     the request
     * @param resp    the response into which the content is written
     *
     * @return the HTTP status code to return, or 0 if a response was sent
     *
     * @throws IOException if an error occurs
     */
    public static int serveFile(File base, String context,
                                Request req, Response resp) throws IOException {
        String relativePath = req.getPath().substring(context.length());
        File file = new File(base, relativePath).getCanonicalFile();

        if (!file.exists() || file.isHidden() || file.getName().startsWith("."))
        {
            return 404;
        } else if (!file.canRead() || !file.getPath().startsWith(base.getPath()))
        { // validate
            return 403;
        } else if (file.isDirectory())
        {
            if (relativePath.endsWith("/"))
            {
                if (!req.getVirtualHost().isAllowGeneratedIndex())
                {
                    return 403;
                }

                resp.send(200, createIndex(file, req.getPath()));
            } else
            { // redirect to the normalized directory URL ending with '/'
                resp.redirect(req.getBaseURL() + req.getPath() + "/", true);
            }
        } else if (relativePath.endsWith("/"))
        {
            return 404; // non-directory ending with slash (File constructor removed it)
        } else
        {
            serveFileContent(file, req, resp);
        }

        return 0;
    }

    /**
     * Serves a context's contents from a 'jar' file based resource.
     * <p>
     * The file is located by stripping the given context prefix from
     * the request's path, and appending the result to the given jarPath directory.
     * <p>
     * Missing, forbidden and otherwise invalid files return the appropriate
     * error response. Directories are served as an HTML index page if the
     * virtual host allows one, or a forbidden error otherwise. Files are
     * sent with their corresponding content types, and handle conditional
     * and partial retrievals according to the RFC.
     *
     * @param jarFS   The 'jar' file system.
     * @param context the context which is mapped to the jarPath directory
     * @param req     the request
     * @param resp    the response into which the content is written
     *
     * @return the HTTP status code to return, or 0 if a response was sent
     *
     * @throws IOException if an error occurs
     */
    public static int serveFile(FileSystem jarFS, String context,
                                Request req, Response resp) throws IOException {

        Path filePath = jarFS.getPath(req.getPath().substring(context.length()));

        if (!Files.exists(filePath) || Files.isHidden(filePath)
            || filePath.startsWith("."))
        {
            return 404;
        } else if (!Files.isReadable(filePath))
        { // validate
            return 403;
        } else if (Files.isDirectory(filePath))
        {
            if (!req.getVirtualHost().isAllowGeneratedIndex())
            {
                return 403;
            }

            resp.send(200, createIndex(filePath, req.getPath()));
        } else if (filePath.endsWith("/"))
        {
            return 404; // non-directory ending with slash (File constructor removed it)
        } else
        {
            serveFileContent(filePath, req, resp);
        }

        return 0;
    }

    /**
     * Serves the contents of a file, with its corresponding content type,
     * last modification time, etc. conditional and partial retrievals are
     * handled according to the RFC.
     *
     * @param file the existing and readable file whose contents are served
     * @param req  the request
     * @param resp the response into which the content is written
     *
     * @throws IOException if an error occurs
     */
    public static void serveFileContent(File file, Request req, Response resp) throws IOException {
        long len = file.length();
        long lastModified = file.lastModified();
        String etag = "W/\"" + lastModified + "\""; // a weak tag based on date
        int status = 200;
        // handle range or conditional request
        long[] range = req.getRange(len);

        if (range == null || len == 0)
        {
            status = getConditionalStatus(req, lastModified, etag);
        } else
        {
            String ifRange = req.getHeaders().get("If-Range");
            if (ifRange == null)
            {
                if (range[0] >= len)
                {
                    status = 416; // unsatisfiable range
                } else
                {
                    status = getConditionalStatus(req, lastModified, etag);
                }
            } else if (range[0] >= len)
            {
                // RFC2616#14.16, 10.4.17: invalid If-Range gets everything
                range = null;
            } else
            { // send either range or everything
                if (!ifRange.startsWith("\"") && !ifRange.startsWith("W/"))
                {
                    Date date = req.getHeaders().getDate("If-Range");
                    if (date != null && lastModified > date.getTime())
                    {
                        range = null; // modified - send everything
                    }
                } else if (!ifRange.equals(etag))
                {
                    range = null; // modified - send everything
                }
            }
        }
        // send the response
        Headers respHeaders = resp.getHeaders();
        switch (status)
        {
            case 304: // no other arrHeader or body allowed
                respHeaders.add("ETag", etag);
                respHeaders.add("Vary", "Accept-Encoding");
                respHeaders.add("Last-Modified", formatDate(lastModified));
                resp.sendHeaders(304);
                break;
            case 412:
                resp.sendHeaders(412);
                break;
            case 416:
                respHeaders.add("Content-Range", "bytes */" + len);
                resp.sendHeaders(416);
                break;
            case 200:
                // send OK response
                resp.sendHeaders(200, len, lastModified, etag,
                                 getContentType(file.getName(), "application/octet-stream"), range);
                // send body
                InputStream in = new FileInputStream(file);
                try
                {
                    resp.sendBody(in, len, range);
                } finally
                {
                    in.close();
                }
                break;
            default:
                resp.sendHeaders(500); // should never happen
                break;
        }
    }

    /**
     * Serves the contents of a file, with its corresponding content type,
     * last modification time, etc. conditional and partial retrievals are
     * handled according to the RFC.
     *
     * @param file the existing and readable file whose contents are served
     * @param req  the request
     * @param resp the response into which the content is written
     *
     * @throws IOException if an error occurs
     */
    public static void serveFileContent(Path file, Request req, Response resp) throws IOException {

        long len = Files.size(file);
        long lastModified = Files.getLastModifiedTime(file).toMillis();
        String etag = "W/\"" + lastModified + "\""; // a weak tag based on date
        int status = 200;
        // handle range or conditional request
        long[] range = req.getRange(len);

        if (range == null || len == 0)
        {
            status = getConditionalStatus(req, lastModified, etag);
        } else
        {
            String ifRange = req.getHeaders().get("If-Range");
            if (ifRange == null)
            {
                if (range[0] >= len)
                {
                    status = 416; // unsatisfiable range
                } else
                {
                    status = getConditionalStatus(req, lastModified, etag);
                }
            } else if (range[0] >= len)
            {
                // RFC2616#14.16, 10.4.17: invalid If-Range gets everything
                range = null;
            } else
            { // send either range or everything
                if (!ifRange.startsWith("\"") && !ifRange.startsWith("W/"))
                {
                    Date date = req.getHeaders().getDate("If-Range");
                    if (date != null && lastModified > date.getTime())
                    {
                        range = null; // modified - send everything
                    }
                } else if (!ifRange.equals(etag))
                {
                    range = null; // modified - send everything
                }
            }
        }
        // send the response
        Headers respHeaders = resp.getHeaders();
        switch (status)
        {
            case 304: // no other headers or body allowed
                respHeaders.add("ETag", etag);
                respHeaders.add("Vary", "Accept-Encoding");
                respHeaders.add("Last-Modified", formatDate(lastModified));
                resp.sendHeaders(304);
                break;

            case 412:
                resp.sendHeaders(412);
                break;

            case 416:
                respHeaders.add("Content-Range", "bytes */" + len);
                resp.sendHeaders(416);
                break;

            case 200:
                // send OK response
                resp.sendHeaders(200, len, lastModified, etag,
                                 getContentType(file.getFileName().toString(),
                                                "application/octet-stream"), range);
                // send body
                try ( InputStream in = Files.newInputStream(file))
                {
                    resp.sendBody(in, len, range);
                }
                break;

            default:
                resp.sendHeaders(500); // should never happen
                break;
        }
    }

    /**
     * Handles a transaction according to the request method.
     *
     * @param req  the transaction request
     * @param resp the transaction response (into which the response is written)
     *
     * @throws IOException if and error occurs
     */
    protected static void handleMethod(Request req, Response resp) throws IOException {
        String method = req.getMethod();
        Map<String, ContextHandler> handlers = req.getContext().getHandlers();

        // RFC 2616#5.1.1 - GET and HEAD must be supported
        if (method.equals("GET") || handlers.containsKey(method))
        {
            serve(req, resp); // method is handled by context handler (or 404)
        } else if (method.equals("HEAD"))
        { // default HEAD handler
            req.method = "GET"; // identical to a GET
            resp.setDiscardBody(true); // process normally but discard body
            serve(req, resp);
        } else if (method.equals("TRACE"))
        { // default TRACE handler
            handleTrace(req, resp);
        } else
        {
            Set<String> methods = new LinkedHashSet<>();
            methods.addAll(Arrays.asList("GET", "HEAD", "TRACE", "OPTIONS")); // built-in methods
            // "*" is a special server-wide (no-context) request supported by OPTIONS
            boolean isServerOptions = req.getPath().equals("*") && method.equals("OPTIONS");
            methods.addAll(isServerOptions ? req.getVirtualHost().getMethods() : handlers.keySet());
            resp.getHeaders().add("Allow", join(", ", methods));

            if (method.equals("OPTIONS"))
            { // default OPTIONS handler
                resp.getHeaders().add("Content-Length", "0"); // RFC2616#9.2
                resp.sendHeaders(200);
            } else if (req.getVirtualHost().getMethods().contains(method))
            {
                resp.sendHeaders(405); // supported by server, but not this context (nor built-in)
            } else
            {
                resp.sendError(501); // unsupported method
            }
        }
    }

    /**
     * Handles a single transaction on a connection.
     * <p>
     * Subclasses can override this method to perform filtering on the
     * request or response, apply wrappers to them, or further customize
     * the transaction processing in some other way.
     *
     * @param req  the transaction request
     * @param resp the transaction response (into which the response is written)
     *
     * @throws IOException if and error occurs
     */
    protected static void handleTransaction(Request req, Response resp) throws IOException {
        resp.setClientCapabilities(req);

        if (preprocessTransaction(req, resp))
        {
            handleMethod(req, resp);
        }
    }

    /**
     * Preprocesses a transaction, performing various validation checks
     * and required special header handling, possibly returning an
     * appropriate response.
     *
     * @param req  the request
     * @param resp the response
     *
     * @return whether further processing should be performed on the transaction
     *
     * @throws IOException if an error occurs
     */
    protected static boolean preprocessTransaction(Request req, Response resp) throws IOException {
        Headers reqHeaders = req.getHeaders();
        // validate request
        String version = req.getVersion();

        switch (version)
        {
            case "HTTP/1.1":
                if (!reqHeaders.contains("Host"))
                {
                    // RFC2616#14.23: missing Host header gets 400
                    resp.sendError(400, "Missing required Host header");
                    return false;
                }   // return a continue response before reading body
                String expect = reqHeaders.get("Expect");
                if (expect != null)
                {
                    if (expect.equalsIgnoreCase("100-continue"))
                    {
                        Response tempResp = new Response(resp.getOutputStream());
                        tempResp.sendHeaders(100);
                        resp.getOutputStream().flush();
                    } else
                    {
                        // RFC2616#14.20: if unknown expect, send 417
                        resp.sendError(417);
                        return false;
                    }
                }
                break;

            case "HTTP/1.0":
            case "HTTP/0.9":
                // RFC2616#14.10 - remove connection headers from older versions
                for (String token : splitElements(reqHeaders.get("Connection"), false))
                {
                    reqHeaders.remove(token);
                }

                break;

            default:
                resp.sendError(400, "Unknown version: " + version);
                return false;
        }
        return true;
    }

    /**
     * Serves the content for a request by invoking the context
     * handler for the requested context (path) and HTTP method.
     *
     * @param req  the request
     * @param resp the response into which the content is written
     *
     * @throws IOException if an error occurs
     */
    protected static void serve(Request req, Response resp) throws IOException {
        // get context handler to handle request
        ContextHandler handler = req.getContext().getHandlers().get(req.getMethod());

        if (handler == null)
        {
            resp.sendError(404);
            return;
        }

        // serve request
        int status = 404;
        // add directory index if necessary
        String path = req.getPath();

        if (path.endsWith("/"))
        {
            String index = req.getVirtualHost().getDirectoryIndex();

            if (index != null)
            {
                req.setPath(path + index);
                status = handler.serve(req, resp);
                req.setPath(path);
            }
        }

        if (status == 404)
        {
            status = handler.serve(req, resp);
        }

        if (status > 0)
        {
            resp.sendError(status);
        }
    }

    /**
     * Not meant to be instantiated.
     */
    private NetUtils() {
    }
}
