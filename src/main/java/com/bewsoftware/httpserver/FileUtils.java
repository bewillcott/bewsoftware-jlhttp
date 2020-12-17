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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.bewsoftware.httpserver.Utils.toSizeApproxString;

/**
 * FileUtils class contains helper methods from the original HTTPServer class.
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 2.5.3
 * @version 2.5.3
 */
public class FileUtils {

    /**
     *
     *
     * @param path the path whose parent is returned (must start with '/')
     *
     * @return the parent of the given path (excluding trailing slash),
     *         or null if given path is the root path
     */
    public static String getParentPath(String path) {
        path = Utils.trimRight(path, '/'); // remove trailing slash
        int slash = path.lastIndexOf('/');
        return slash < 0 ? null : path.substring(0, slash);
    }

    /**
     *
     *
     * @param in the stream from which the line is read
     *
     * @return the read string, excluding the terminating LF character
     *         and (if exists) the CR character immediately preceding it
     *
     * @throws EOFException if the stream end is reached before an LF character is found
     * @throws IOException  if an IO error occurs, or the line is longer than 8192 bytes
     * @see #readToken(InputStream, int, String, int)
     */
    public static String readLine(InputStream in) throws IOException {
        return readToken(in, '\n', "ISO8859_1", 8192);
    }

    /**
     * If LF is specified as the delimiter, a CRLF pair is also treated as one.
     *
     * @param in        the stream from which the token is read
     * @param delim     the byte value which marks the end of the token,
     *                  or -1 if the token ends at the end of the stream
     * @param enc       a character-encoding name
     * @param maxLength the maximum length (in bytes) to read
     *
     * @return the read token, excluding the delimiter
     *
     * @throws UnsupportedEncodingException if the encoding is not supported
     * @throws EOFException                 if the stream end is reached before a delimiter is found
     * @throws IOException                  if an IO error occurs, or the maximum length
     *                                      is reached before the token end is reached
     */
    public static String readToken(InputStream in, int delim, String enc, int maxLength) throws IOException {
        // note: we avoid using a ByteArrayOutputStream here because it
        // suffers the overhead of synchronization for each byte written
        int b;
        int len = 0; // buffer length
        int count = 0; // number of read bytes
        byte[] buf = null; // optimization - lazy allocation only if necessary
        while ((b = in.read()) != -1 && b != delim)
        {
            if (count == len)
            {
                // expand buffer
                if (count == maxLength)
                {
                    throw new IOException("token too large (" + count + ")");
                }
                len = len > 0 ? 2 * len : 256; // start small, double each expansion
                len = maxLength < len ? maxLength : len;
                byte[] expanded = new byte[len];
                if (buf != null)
                {
                    System.arraycopy(buf, 0, expanded, 0, count);
                }
                buf = expanded;
            }
            buf[count++] = (byte) b;
        }
        if (b < 0 && delim != -1)
        {
            throw new EOFException("unexpected end of stream");
        }
        if (delim == '\n' && count > 0 && buf[count - 1] == '\r')
        {
            count--;
        }
        return count > 0 ? new String(buf, 0, count, enc) : "";
    }

    /**
     *
     *
     * @param in  the input stream to transfer from
     * @param out the output stream to transfer to (or null to discard output)
     * @param len the number of bytes to transfer. If negative, the entire
     *            contents of the input stream are transferred.
     *
     * @throws IOException if an IO error occurs or the input stream ends
     *                     before the requested number of bytes have been read
     */
    public static void transfer(InputStream in, OutputStream out, long len) throws IOException {
        if (len == 0 || out == null && len < 0 && in.read() < 0)
        {
            return; // small optimization - avoid buffer creation
        }
        byte[] buf = new byte[4096];
        while (len != 0)
        {
            int count = len < 0 || buf.length < len ? buf.length : (int) len;
            count = in.read(buf, 0, count);
            if (count < 0)
            {
                if (len > 0)
                {
                    throw new IOException("unexpected end of stream");
                }
                break;
            }
            if (out != null)
            {
                out.write(buf, 0, count);
            }
            len -= len > 0 ? count : 0;
        }
    }

    /**
     * <p>
     * <b>Sort order:</b>
     * <ol>
     * <li>Directories:</li>
     * <ol>
     * <li>All uppercase first letter</li>
     * <li>All lowercase first letter</li>
     * </ol>
     * <li>Files:</li>
     * <ol>
     * <li>All uppercase first letter</li>
     * <li>All lowercase first letter</li>
     * </ol>
     * </ol>
     *
     * @param files The list of files to sort.
     *
     * @return Sorted list.
     *
     * @since 2.5.1
     */
    private static File[] sortFiles(File[] files) {
        SortedSet<File> dirSet = new TreeSet<>((file1, file2) ->
        {
            int rtn = 0;
            if (file1.isDirectory())
            {
                if (file2.isDirectory())
                {
                    rtn = file1.compareTo(file2);
                } else
                {
                    rtn = -11;
                }
            } else
            {
                if (file2.isDirectory())
                {
                    rtn = 1;
                } else
                {
                    rtn = file1.compareTo(file2);
                }
            }
            return rtn;
        });
        dirSet.addAll(Arrays.asList(files));
        return dirSet.toArray(new File[dirSet.size()]);
    }

    /**
     * <p>
     * <b>Sort order:</b>
     * <ol>
     * <li>Directories:</li>
     * <ol>
     * <li>All uppercase first letter</li>
     * <li>All lowercase first letter</li>
     * </ol>
     * <li>Files:</li>
     * <ol>
     * <li>All uppercase first letter</li>
     * <li>All lowercase first letter</li>
     * </ol>
     * </ol>
     *
     * @param files The list of files to sort.
     *
     * @return Sorted list.
     *
     * @since 2.5.1
     */
    private static Path[] sortFiles(Path[] files) {
        SortedSet<Path> dirSet = new TreeSet<>((file1, file2) ->
        {
            int rtn = 0;
            if (Files.isDirectory(file1))
            {
                if (Files.isDirectory(file2))
                {
                    rtn = file1.compareTo(file2);
                } else
                {
                    rtn = -11;
                }
            } else
            {
                if (Files.isDirectory(file2))
                {
                    rtn = 1;
                } else
                {
                    rtn = file1.compareTo(file2);
                }
            }
            return rtn;
        });
        dirSet.addAll(Arrays.asList(files));
        return dirSet.toArray(new Path[dirSet.size()]);
    }

    /**
     * Not meant to be instantiated.
     */
    private FileUtils() {
    }

    /**
     * Serves the contents of a directory as an HTML file index.
     *
     * @param dir  the existing and readable directory whose contents are served
     * @param path the displayed jarPath path corresponding to jarPath
     *
     * @return an HTML string containing the file index for the directory
     */
    public static String createIndex(File dir, String path) {
        if (!path.endsWith("/"))
        {
            path += "/";
        }

        // calculate name column width
        int w = 21; // minimum width

        for (String name : dir.list())
        {
            if (name.length() > w)
            {
                w = name.length();
            }
        }

        w += 2; // with room for added slash and space
        // note: we use apache's format, for consistent user experience
        Formatter f = new Formatter(Locale.US);

        f.format("<!DOCTYPE html>%n"
                 + "<html><head><title>Index of %s</title></head>%n"
                 + "<body><h1>Index of %s</h1>%n"
                 + "<pre> Name%" + (w - 5) + "s Last modified      Size<hr>",
                 path, path, "");

        if (path.length() > 1) // add parent link if not root path
        {
            f.format(" <a href=\"%s/\">Parent Directory</a>%"
                     + (w + 5) + "s-%n", getParentPath(path), "");
        }

        for (File file : sortFiles(dir.listFiles()))
        {
            try
            {
                String name = file.getName() + (file.isDirectory() ? "/" : "");
                String size = file.isDirectory() ? "- " : toSizeApproxString(file.length());
                // properly url-encode the link
                String link = new URI(null, path + name, null).toASCIIString();

                if (!file.isHidden() && !name.startsWith("."))
                {
                    f.format(" <a href=\"%s\">%s</a>%-" + (w - name.length())
                             + "s&#8206;%td-%<tb-%<tY %<tR%6s%n",
                             link, name, "", file.lastModified(), size);
                }
            } catch (URISyntaxException ignore)
            {
            }
        }

        f.format("</pre></body></html>");
        return f.toString();
    }

    /**
     * Serves the contents of a directory as an HTML file index.
     * <p>
     * Added by: Bradley Willcott (2020/12/08)
     *
     * @param dir  the existing and readable directory whose contents are served
     * @param path the displayed jarPath path corresponding to jarPath
     *
     * @return an HTML string containing the file index for the directory
     *
     * @throws IOException if any.
     */
    public static String createIndex(Path dir, String path) throws IOException {
        if (!path.endsWith("/"))
        {
            path += "/";
        }

        Path[] files = Files.list(dir).toArray(Path[]::new);

        // calculate name column width
        int w = 21; // minimum width

        for (Path file : files)
        {
            String name = file.getFileName().toString();

            if (name.length() > w)
            {
                w = name.length();
            }
        }

        w += 2; // with room for added slash and space
        // note: we use apache's format, for consistent user experience
        Formatter f = new Formatter(Locale.US);

        f.format("<!DOCTYPE html>%n"
                 + "<html><head><title>Index of %s</title></head>%n"
                 + "<body><h1>Index of %s</h1>%n"
                 + "<pre> Name%" + (w - 5) + "s Last modified      Size<hr>",
                 path, path, "");

        if (path.length() > 1) // add parent link if not root path
        {
            f.format(" <a href=\"%s/\">Parent Directory</a>%"
                     + (w + 5) + "s-%n", getParentPath(path), "");
        }

        for (Path file : sortFiles(files))
        {
            try
            {
                String name = file.getFileName().toString() + (Files.isDirectory(file) ? "/" : "");
                String size = Files.isDirectory(file) ? "- " : toSizeApproxString(Files.size(file));
                // properly url-encode the link
                String link = new URI(null, path + name, null).toASCIIString();

                if (!Files.isHidden(file) && !name.startsWith("."))
                {
                    f.format(" <a href=\"%s\">%s</a>%-" + (w - name.length())
                             + "s&#8206;%td-%<tb-%<tY %<tR%6s%n",
                             link, name, "", Files.getLastModifiedTime(file).toMillis(), size);
                }
            } catch (URISyntaxException ignore)
            {
            }
        }

        f.format("</pre></body></html>");
        return f.toString();
    }

}
