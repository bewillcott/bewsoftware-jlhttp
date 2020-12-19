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
import java.net.*;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.swing.JOptionPane;

import static com.bewsoftware.httpserver.NetUtils.handleTransaction;
import static com.bewsoftware.httpserver.Utils.openURL;
import static com.bewsoftware.httpserver.Utils.split;
import static java.lang.System.exit;

/**
 * The {@code HTTPServer} class implements a light-weight HTTP server.
 * <p>
 * This server implements all functionality required by RFC 2616 ("Hypertext
 * Transfer Protocol -- HTTP/1.1"), as well as some of the optional
 * functionality (this is termed "conditionally compliant" in the RFC).
 * In fact, a couple of bugs in the RFC itself were discovered
 * (and fixed) during the development of this server.
 * <p>
 * <b>Feature Overview</b>
 * <ul>
 * <li>RFC compliant - correctness is not sacrificed for the sake of size</li>
 * <li>Virtual hosts - multiple domains and subdomains per server</li>
 * <li>File serving - built-in handler to serve files and folders from disk</li>
 * <li>Mime type mappings - configurable via API or a standard mime.types file</li>
 * <li>Directory index generation - enables browsing folder contents</li>
 * <li>Welcome files - configurable default filename (e.g. index.html)</li>
 * <li>All HTTP methods supported - GET/HEAD/OPTIONS/TRACE/POST/PUT/DELETE/custom</li>
 * <li>Conditional statuses - ETags and If-* header support</li>
 * <li>Chunked transfer encoding - for serving dynamically-generated data streams</li>
 * <li>Gzip/deflate compression - reduces bandwidth and download time</li>
 * <li>HTTPS - secures all server communications</li>
 * <li>Partial content - download continuation (a.k.a. byte range serving)</li>
 * <li>File upload - multipart/form-data handling as stream or iterator</li>
 * <li>Multiple context handlers - a different handler method per URL path</li>
 * <li>@Context annotations - auto-detection of context handler methods</li>
 * <li>Parameter parsing - from query string or x-www-form-urlencoded body</li>
 * <li>A single source file - super-easy to integrate into any application</li>
 * <li>Standalone - no dependencies other than the Java runtime</li>
 * <li>Small footprint - standard jar is ~50K, stripped jar is ~35K</li>
 * <li>Extensible design - easy to override, add or remove functionality</li>
 * <li>Reusable utility methods to simplify your custom code</li>
 * <li>Extensive documentation of API and implementation (&gt;40% of source lines)</li>
 * </ul>
 * <p>
 * <b>Use Cases</b>
 * <p>
 * Being a lightweight, standalone, easily embeddable and tiny-footprint
 * server, it is well-suited for
 * <ul>
 * <li>Resource-constrained environments such as embedded devices.
 * For really extreme constraints, you can easily remove unneeded
 * functionality to make it even smaller (and use the -Dstripped
 * maven build option to strip away debug info, license, etc.)</li>
 * <li>Unit and integration tests - fast setup/teardown times, small overhead
 * and simple context handler setup make it a great web server for testing
 * client components under various server response conditions.</li>
 * <li>Embedding a web console into any headless application for
 * administration, monitoring, or a full portable GUI.</li>
 * <li>A full-fledged standalone web server serving static files,
 * dynamically-generated content, REST APIs, pseudo-streaming, etc.</li>
 * <li>A good reference for learning how HTTP works under the hood.</li>
 * </ul>
 * <p>
 * <b>Implementation Notes</b>
 * <p>
 * The design and implementation of this server attempt to balance correctness,
 * compliance, readability, size, features, extensibility and performance,
 * and often prioritize them in this order, but some trade-offs must be made.
 * <p>
 * This server is multi-threaded in its support for multiple concurrent HTTP
 * connections, however most of its constituent classes are not thread-safe and
 * require external synchronization if accessed by multiple threads concurrently.
 * <p>
 * <b>Source Structure and Documentation</b>
 * <p>
 * This server is intentionally written as a single source file, in order to make
 * it as easy as possible to integrate into any existing project - by simply adding
 * this single file to the project sources. It does, however, aim to maintain a
 * structured and flexible design. There are no external package dependencies.
 * <p>
 * This file contains extensive documentation of its classes and methods, as
 * well as implementation details and references to specific RFC sections
 * which clarify the logic behind the code. It is recommended that anyone
 * attempting to modify the protocol-level functionality become acquainted with
 * the RFC, in order to make sure that protocol compliance is not broken.
 * <p>
 * <b>Getting Started</b>
 * <p>
 * For an example and a good starting point for learning how to use the API,
 * see the {@link #execute()} method, and follow
 * the code into the API from there. Alternatively, you can just browse through
 * the classes and utility methods and read their documentation and code.
 * <p>
 * &nbsp;</p>
 * <hr>
 * <b>Changes:</b>
 * <ul>
 * <li>(2020/12/08 - v2.5.2)
 * <ul>
 * <li>Made changes recommended by Netbeans 12.1.</li>
 * <li>Updated code to JDK 12.</li>
 * <li>Made specific to requirement of publishing static web pages from 'jar' file.</li>
 * <li>Added ability to auto open the system default browser to hosted location.</li>
 * <li>Changed package from: {@code net.freeutils.httpserver} to {@code com.bewsoftware.httpserver}.</li>
 * <li>Updated licence from GPLv2 to GPLv3.</li>
 * <li>To be embed inside 'jar' files containing the results of my MDj CLI program (.md -&lt; .html).</li>
 * </ul></li>
 * <li>(2020/12/18 - v2.5.3)
 * <ul>
 * <li>Made minor changes to improve embedding.</li>
 * <li>Due to the difficulty of working with such a large single file, I have split it up
 * into many smaller more manageable files.</li>
 * </ul></li>
 * <li>(2020/12/18 - v2.5.4)
 * <ul>
 * <li>Made minor bug fixes to improve reliability.</li>
 * </ul></li>
 * </ul>
 * Bradley Willcott
 * <hr>
 *
 * @author Amichai Rothman
 * @since 2008-07-24
 * @version 2.5.4
 */
public class HTTPServer {

    /**
     * A convenience array containing the carriage-return and line feed chars.
     */
    public static final byte[] CRLF =
    {
        0x0d, 0x0a
    };

    /**
     * The SimpleDateFormat-compatible formats of dates which must be supported.
     * Note that all generated date fields must be in the RFC 1123 format only,
     * while the others are supported by recipients for backwards-compatibility.
     */
    public static final String[] DATE_PATTERNS =
    {
        "EEE, dd MMM yyyy HH:mm:ss z", // RFC 822, updated by RFC 1123
        "EEEE, dd-MMM-yy HH:mm:ss z", // RFC 850, obsoleted by RFC 1036
        "EEE MMM d HH:mm:ss yyyy"      // ANSI C's asctime() format
    };

    /**
     * Program title.
     * <p>
     * Added by: Bradley Willcott (2020/12/08)
     */
    public static final String TITLE = "HTTP Server";

    /**
     * Http Server version.
     * <p>
     * Added by: Bradley Willcott (2020/12/08)
     */
    public static final String VERSION = "v2.5.4";

    /**
     * Date format string.
     */
    protected static final char[] DAYS = "Sun Mon Tue Wed Thu Fri Sat".toCharArray();

    /**
     * Default range of ports to try.
     * <p>
     * Added by: Bradley Willcott (2020/12/08)
     */
    protected static int[] DEFAULT_PORT_RANGE =
    {
        9000, 9010
    };

    /**
     * A GMT (UTC) timezone instance.
     */
    protected static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    /**
     * Date format string.
     */
    protected static final char[] MONTHS
                                  = "Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec"
                    .toCharArray();
    /**
     * The operating system we are running on.
     * <p>
     * Added by: Bradley Willcott (2020/12/08)
     */
    protected static volatile String OS = System.getProperty("os.name").toLowerCase();

    /**
     * The MIME types that can be compressed (prefix/suffix wildcards allowed).
     */
    protected static final String[] compressibleContentTypes =
    {
        "text/*", "*/javascript", "*icon", "*+xml", "*/json"
    };

    /**
     * A mapping of path suffixes (e.g. file extensions) to their
     * corresponding MIME types.
     */
    protected static final Map<String, String> contentTypes = new ConcurrentHashMap<>();

    /**
     * The HTTP status description strings.
     */
    protected static final String[] statuses = new String[600];

    static
    {
        // initialize status descriptions lookup table
        Arrays.fill(statuses, "Unknown Status");
        statuses[100] = "Continue";
        statuses[200] = "OK";
        statuses[204] = "No Content";
        statuses[206] = "Partial Content";
        statuses[301] = "Moved Permanently";
        statuses[302] = "Found";
        statuses[304] = "Not Modified";
        statuses[307] = "Temporary Redirect";
        statuses[400] = "Bad Request";
        statuses[401] = "Unauthorized";
        statuses[403] = "Forbidden";
        statuses[404] = "Not Found";
        statuses[405] = "Method Not Allowed";
        statuses[408] = "Request Timeout";
        statuses[412] = "Precondition Failed";
        statuses[413] = "Request Entity Too Large";
        statuses[414] = "Request-URI Too Large";
        statuses[416] = "Requested Range Not Satisfiable";
        statuses[417] = "Expectation Failed";
        statuses[500] = "Internal Server Error";
        statuses[501] = "Not Implemented";
        statuses[502] = "Bad Gateway";
        statuses[503] = "Service Unavailable";
        statuses[504] = "Gateway Time-out";
    }

    static
    {
        // add some default common content types
        // see http://www.iana.org/assignments/media-types/ for full list
        addContentType("application/font-woff", "woff");
        addContentType("application/font-woff2", "woff2");
        addContentType("application/java-archive", "jar");
        addContentType("application/javascript", "js");
        addContentType("application/json", "json");
        addContentType("application/octet-stream", "exe");
        addContentType("application/pdf", "pdf");
        addContentType("application/x-7z-compressed", "7z");
        addContentType("application/x-compressed", "tgz");
        addContentType("application/x-gzip", "gz");
        addContentType("application/x-tar", "tar");
        addContentType("application/xhtml+xml", "xhtml");
        addContentType("application/zip", "zip");
        addContentType("audio/mpeg", "mp3");
        addContentType("image/gif", "gif");
        addContentType("image/jpeg", "jpg", "jpeg");
        addContentType("image/png", "png");
        addContentType("image/svg+xml", "svg");
        addContentType("image/x-icon", "ico");
        addContentType("text/css", "css");
        addContentType("text/csv", "csv");
        addContentType("text/html; charset=utf-8", "htm", "html");
        addContentType("text/plain", "txt", "text", "log");
        addContentType("text/xml", "xml");
    }

    /**
     * Adds a Content-Type mapping for the given path suffixes.
     * If any of the path suffixes had a previous Content-Type associated
     * with it, it is replaced with the given one. Path suffixes are
     * considered case-insensitive, and contentType is converted to lowercase.
     *
     * @param contentType the content type (MIME type) to be associated with
     *                    the given path suffixes
     * @param suffixes    the path suffixes which will be associated with
     *                    the contentType, e.g. the file extensions of served files
     *                    (excluding the '.' character)
     */
    public static void addContentType(String contentType, String... suffixes) {
        for (String suffix : suffixes)
        {
            contentTypes.put(suffix.toLowerCase(Locale.US), contentType.toLowerCase(Locale.US));
        }
    }

    /**
     * Adds Content-Type mappings from a standard mime.types file.
     *
     * @param in a stream containing a mime.types file
     *
     * @throws IOException           if an error occurs
     * @throws FileNotFoundException if the file is not found or cannot be read
     */
    public static void addContentTypes(InputStream in) throws IOException {
        try (in)
        {
            String line;

            do
            {
                line = FileUtils.readLine(in).trim(); // throws EOFException when done

                if (line.length() > 0 && line.charAt(0) != '#')
                {
                    String[] tokens = split(line, " \t", -1);
                    for (int i = 1; i < tokens.length; i++)
                    {
                        addContentType(tokens[0], tokens[i]);
                    }
                }
            } while (line.length() >= 0);
        } catch (EOFException ignore)
        { // the end of file was reached - it's ok
        }
    }

    //=====================================================================================
    /**
     * Starts a stand-alone HTTP server, serving files from disk.
     */
    public static void execute() {
        HTTPServer server = null;

        try
        {
            server = new HTTPServer(DEFAULT_PORT_RANGE[0]);

            Path dir = null;

            // set up server
            File f = new File("/etc/mime.types");

            if (f.exists())
            {
                addContentTypes(new FileInputStream(f));
            } else
            {
                addContentTypes(HTTPServer.class.getResourceAsStream("/docs/jar/etc/mime.types"));
            }

            VirtualHost host = server.getVirtualHost(null); // default host
            host.setAllowGeneratedIndex(true); // with directory index pages
            host.addContext("/", new JarContextHandler(dir, "/manual"));
            host.addContext("/api/time", (Request req, Response resp) ->
                    {
                        long now = System.currentTimeMillis();
                        resp.getHeaders().add("Content-Type", "text/plain");
                        resp.send(200, String.format("Server time: %tF %<tT", now));
                        return 0;
                    });

            server.start();
            String msg = TITLE + " (" + VERSION + ") is listening on port " + server.port;
            System.out.println(msg);
            openURL(new URL("http", "localhost", server.port, "/"));

            // GUI dialog to show server running, with button to
            // shutdown server.
            //
            //Custom button text
            Object[] options =
            {
                "Stop Server"
            };
            JOptionPane.showOptionDialog(null, msg, TITLE + " (" + VERSION + ")",
                                         JOptionPane.OK_OPTION, JOptionPane.WARNING_MESSAGE,
                                         null, options, null);

            server.stop();
            msg = TITLE + " (" + VERSION + ") on port " + server.port + " has terminated.";
            System.out.println(msg);
            exit(0);
        } catch (IOException | NumberFormatException | URISyntaxException | InterruptedException e)
        {
            System.err.println("error: " + e);
        }
    } //=====================================================================================

    /**
     * Returns the content type for the given path, according to its suffix,
     * or the given default content type if none can be determined.
     *
     * @param path the path whose content type is requested
     * @param def  a default content type which is returned if none can be
     *             determined
     *
     * @return the content type for the given path, or the given default
     */
    public static String getContentType(String path, String def) {
        int dot = path.lastIndexOf('.');
        String type = dot < 0 ? def : contentTypes.get(path.substring(dot + 1).toLowerCase(Locale.US));
        return type != null ? type : def;
    }

    /**
     * Checks whether data of the given content type (MIME type) is compressible.
     *
     * @param contentType the content type
     *
     * @return true if the data is compressible, false if not
     */
    public static boolean isCompressible(String contentType) {
        int pos = contentType.indexOf(';'); // exclude params
        String ct = pos < 0 ? contentType : contentType.substring(0, pos);

        for (String s : compressibleContentTypes)
        {
            if (s.equals(ct) || s.charAt(0) == '*' && ct.endsWith(s.substring(1))
                || s.charAt(s.length() - 1) == '*' && ct.startsWith(s.substring(0, s.length() - 1)))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Are we running on a MacIntosh system?
     * <p>
     * Added by: Bradley Willcott (2020/12/08)
     *
     * @return result.
     */
    public static boolean isMac() {
        return OS.contains("mac");
    }

    /**
     * Are we running on a Unix/Linux type system?
     * <p>
     * Added by: Bradley Willcott (2020/12/08)
     *
     * @return result.
     */
    public static boolean isUnix() {
        return OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0;
    }

    /**
     * Are we running on a Windows system?
     * <p>
     * Added by: Bradley Willcott (2020/12/08)
     *
     * @return result.
     */
    public static boolean isWindows() {
        return OS.contains("win");
    }

    //=====================================================================================
    /**
     * Starts a stand-alone HTTP server, serving files from disk.
     *
     * @param args the command line arguments
     *
     * @throws URISyntaxException   if any.
     * @throws InterruptedException if any.
     */
    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        HTTPServer server = null;

        try
        {
            server = new HTTPServer(DEFAULT_PORT_RANGE[0]);

            Path dir = null;

            // set up server
            File f = new File("/etc/mime.types");

            if (f.exists())
            {
                addContentTypes(new FileInputStream(f));
            } else
            {
                addContentTypes(HTTPServer.class.getResourceAsStream("/etc/mime.types"));
            }

            VirtualHost host = server.getVirtualHost(null); // default host
            host.setAllowGeneratedIndex(true); // with directory index pages
            host.addContext("/", new JarContextHandler(dir, "/"));
            host.addContext("/api/time", (Request req, Response resp) ->
                    {
                        long now = System.currentTimeMillis();
                        resp.getHeaders().add("Content-Type", "text/plain");
                        resp.send(200, String.format("Server time: %tF %<tT", now));
                        return 0;
                    });

            server.start();
            String msg = TITLE + " (" + VERSION + ") is listening on port " + server.port;
            System.out.println(msg);
            openURL(new URL("http", "localhost", server.port, "/"));

            // GUI dialog to show server running, with button to
            // shutdown server.
            //
            //Custom button text
            Object[] options =
            {
                "Stop Server"
            };

            JOptionPane.showOptionDialog(null, msg, TITLE + " (" + VERSION + ")",
                                         JOptionPane.OK_OPTION, JOptionPane.WARNING_MESSAGE,
                                         null, options, null);

            server.stop();
            exit(0);
        } catch (IOException | NumberFormatException e)
        {
            System.err.println("error: " + e);
        }
    } //=====================================================================================

    /**
     * Parses a date string in one of the supported {@link #DATE_PATTERNS}.
     * <p>
     * Received date header values must be in one of the following formats:
     * Sun, 06 Nov 1994 08:49:37 GMT ; RFC 822, updated by RFC 1123
     * Sunday, 06-Nov-94 08:49:37 GMT ; RFC 850, obsoleted by RFC 1036
     * Sun Nov 6 08:49:37 1994 ; ANSI C's asctime() format
     *
     * @param time a string representation of a time value
     *
     * @return the parsed date value
     *
     * @throws IllegalArgumentException if the given string does not contain
     *                                  a valid date format in any of the supported formats
     */
    public static Date parseDate(String time) {
        for (String pattern : DATE_PATTERNS)
        {
            try
            {
                SimpleDateFormat df = new SimpleDateFormat(pattern, Locale.US);
                df.setLenient(false);
                df.setTimeZone(GMT);
                return df.parse(time);
            } catch (ParseException ignore)
            {
            }
        }

        throw new IllegalArgumentException("invalid date format: " + time);
    }

    protected volatile Executor executor;
    protected final Map<String, VirtualHost> hosts = new ConcurrentHashMap<>();
    protected volatile int port;
    protected volatile boolean secure;
    protected volatile ServerSocket serv;
    protected volatile ServerSocketFactory serverSocketFactory;
    protected volatile int socketTimeout = 1000;

    /**
     * Constructs an HTTPServer which can accept connections on the given port.
     * Note: the {@link #start()} method must be called to start accepting
     * connections.
     *
     * @param port the port on which this server will accept connections
     */
    public HTTPServer(int port) {
        setPort(port);
        addVirtualHost(new VirtualHost(null)); // add default virtual host
    }

    /**
     * Constructs an HTTPServer which can accept connections on the default HTTP port 80.
     * Note: the {@link #start()} method must be called to start accepting connections.
     */
    public HTTPServer() {
        this(80);
    }

    /**
     * Adds the given virtual host to the server.
     * If the host's name or aliases already exist, they are overwritten.
     *
     * @param host the virtual host to add
     */
    public final void addVirtualHost(VirtualHost host) {
        String name = host.getName();
        hosts.put(name == null ? "" : name, host);
    }

    /**
     * Sets the executor used in servicing HTTP connections.
     * If null, a default executor is used. The caller is responsible
     * for shutting down the provided executor when necessary.
     *
     * @param executor the executor to use
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    /**
     * Sets the port on which this server will accept connections.
     *
     * @param port the port on which this server will accept connections
     */
    public final void setPort(int port) {
        this.port = port;
    }

    /**
     * Sets the factory used to create the server socket.
     * If null or not set, the default {@link ServerSocketFactory#getDefault()} is used.
     * For secure sockets (HTTPS), use an SSLServerSocketFactory instance.
     * The port should usually also be changed for HTTPS, e.g. port 443 instead of 80.
     * <p>
     * If using the default SSLServerSocketFactory returned by
     * {@link SSLServerSocketFactory#getDefault()}, the appropriate system properties
     * must be set to configure the default JSSE provider, such as
     * {@code javax.net.ssl.keyStore} and {@code javax.net.ssl.keyStorePassword}.
     *
     * @param factory the server socket factory to use
     */
    public void setServerSocketFactory(ServerSocketFactory factory) {
        this.serverSocketFactory = factory;
        this.secure = factory instanceof SSLServerSocketFactory;
    }

    /**
     * Sets the socket timeout for established connections.
     *
     * @param timeout the socket timeout in milliseconds
     */
    public void setSocketTimeout(int timeout) {
        this.socketTimeout = timeout;
    }

    /**
     * Returns the virtual host with the given name.
     *
     * @param name the name of the virtual host to return,
     *             or null for the default virtual host
     *
     * @return the virtual host with the given name, or null if it doesn't exist
     */
    public VirtualHost getVirtualHost(String name) {
        return hosts.get(name == null ? "" : name);
    }

    /**
     * Returns all virtual hosts.
     *
     * @return all virtual hosts (as an unmodifiable set)
     */
    public Set<VirtualHost> getVirtualHosts() {
        return Collections.unmodifiableSet(new HashSet<>(hosts.values()));
    }

    /**
     * Starts this server. If it is already started, does nothing.
     * Note: Once the server is started, configuration-altering methods
     * of the server and its virtual hosts must not be used. To modify the
     * configuration, the server must first be stopped.
     *
     * @throws IOException if the server cannot begin accepting connections
     */
    public synchronized void start() throws IOException {
        if (serv != null)
        {
            return;
        }

        if (serverSocketFactory == null) // assign default server socket factory if needed
        {
            serverSocketFactory = ServerSocketFactory.getDefault(); // plain sockets
        }

        serv = createServerSocket();

        if (executor == null) // assign default executor if needed
        {
            executor = Executors.newCachedThreadPool(); // consumes no resources when idle
        }        // register all host aliases (which may have been modified)

        getVirtualHosts()
                .forEach(host -> host.getAliases()
                .forEach(alias -> hosts.put(alias, host)));

        // start handling incoming connections
        new SocketHandlerThread(this).start();
    }

    /**
     * Stops this server. If it is already stopped, does nothing.
     * Note that if an {@link #setExecutor Executor} was set, it must be closed separately.
     */
    public synchronized void stop() {
        try
        {
            if (serv != null)
            {
                serv.close();
            }
        } catch (IOException ignore)
        {
            // NoOp
        }
        serv = null;

        hosts.values().forEach((VirtualHost host) ->
        {
            host.contexts.values().forEach((VirtualHost.ContextInfo context) ->
            {
                context.handlers.values().forEach((ContextHandler handler) ->
                {
                    try
                    {
                        ((AutoCloseable) handler).close();
                    } catch (Exception ignore)
                    {
                        // No Op.
                    }
                });
            });
        });
    }

    @Override
    public String toString() {
        return "HTTPServer{"
               + "\nexecutor=" + executor + ", "
               + "\nhosts=" + hosts + ", "
               + "\nport=" + port + ", "
               + "\nsecure=" + secure + ", "
               + "\nserv=" + serv + ", "
               + "\nserverSocketFactory=" + serverSocketFactory + ", "
               + "\nsocketTimeout=" + socketTimeout + '}';
    }

    /**
     * Creates the server socket used to accept connections, using the configured
     * {@link #setServerSocketFactory ServerSocketFactory} and {@link #setPort port}.
     * <p>
     * Cryptic errors seen here often mean the factory configuration details are wrong.
     * <dl>
     * <dt><b>Changes:</b></dt>
     * <dd>Added code to try a range of default ports, if the initial port is not
     * available.</dd>
     * <dd>Bradley Willcott (2020/12/08)</dd>
     * </dl>
     *
     * @return the created server socket
     *
     * @throws IOException if the socket cannot be created
     */
    protected ServerSocket createServerSocket() throws IOException {
        ServerSocket serverSocket = serverSocketFactory.createServerSocket();
        serverSocket.setReuseAddress(true);

        // New code (bw)
        try
        {
            serverSocket.bind(new InetSocketAddress(port));
        } catch (IOException ex)
        {
            // Try to bind to preset default range of ports.

            for (int lport = DEFAULT_PORT_RANGE[0]; lport <= DEFAULT_PORT_RANGE[1]; lport++)
            {
                if (lport != port)
                {
                    try
                    {
                        serverSocket.bind(new InetSocketAddress(lport));
                        port = lport;
                        break;
                    } catch (IOException ignore)
                    {
                        // NoOP - go around again ....
                    }
                }
            }

            if (!serverSocket.isBound())
            {
                throw ex;
            }
        } // to here.

        return serverSocket;
    }

    /**
     * Handles communications for a single connection over the given streams.
     * Multiple subsequent transactions are handled on the connection,
     * until the streams are closed, an error occurs, or the request
     * contains a "Connection: close" header which explicitly requests
     * the connection be closed after the transaction ends.
     *
     * @param in  the stream from which the incoming requests are read
     * @param out the stream into which the outgoing responses are written
     *
     * @throws IOException if an error occurs
     */
    protected void handleConnection(InputStream in, OutputStream out) throws IOException {
        in = new BufferedInputStream(in, 4096);
        out = new BufferedOutputStream(out, 4096);
        Request req;
        Response resp;

        do
        {
            // create request and response and handle transaction
            req = null;
            resp = new Response(out);
            try
            {
                req = new Request(in, this);
                handleTransaction(req, resp);
            } catch (Throwable t)
            { // unhandled errors (not normal error responses like 404)

                if (req == null)
                { // error reading request
                    if (t instanceof IOException && t.getMessage().contains("missing request line"))
                    {
                        break; // we're not in the middle of a transaction - so just disconnect
                    }

                    resp.getHeaders().add("Connection", "close"); // about to close connection

                    if (t instanceof InterruptedIOException) // e.g. SocketTimeoutException
                    {
                        resp.sendError(408, "Timeout waiting for client request");
                    } else
                    {
                        resp.sendError(400, "Invalid request: " + t.getMessage());
                    }
                } else if (!resp.headersSent())
                { // if headers were not already sent, we can send an error response
                    t.printStackTrace();
                    resp = new Response(out); // ignore whatever headers may have already been set
                    resp.getHeaders().add("Connection", "close"); // about to close connection
                    resp.sendError(500, "Error processing request: " + t);
                } // otherwise just abort the connection since we can't recover

                break; // proceed to close connection
            } finally
            {
                resp.close(); // close response and flush output
            }
            // consume any leftover body data so next request can be processed
            FileUtils.transfer(req.getBody(), null, -1);
            // RFC7230#6.6: persist connection unless client or server close explicitly (or legacy client)
        } while (!"close".equalsIgnoreCase(req.getHeaders().get("Connection"))
                 && !"close".equalsIgnoreCase(resp.getHeaders().get("Connection")) && req.getVersion().endsWith("1.1"));
    }

}
