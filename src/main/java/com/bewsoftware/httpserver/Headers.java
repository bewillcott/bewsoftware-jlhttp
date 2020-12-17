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
import java.io.OutputStream;
import java.util.*;

import static com.bewsoftware.httpserver.HTTPServer.CRLF;
import static com.bewsoftware.httpserver.HTTPServer.parseDate;
import static com.bewsoftware.httpserver.Utils.getBytes;
import static com.bewsoftware.httpserver.Utils.split;
import static com.bewsoftware.httpserver.Utils.trimLeft;
import static com.bewsoftware.httpserver.Utils.trimRight;

/**
 * Headers class description.
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 1.0
 * @version 2.5.3
 */
/**
 * The {@code Headers} class encapsulates a collection of HTTP headers.
 * <p>
 * Header names are treated case-insensitively, although this class retains
 * their original case. Header insertion order is maintained as well.
 */
public class Headers implements Iterable<Header> {

    // due to the requirements of case-insensitive name comparisons,
    // retaining the original case, and retaining header insertion order,
    // and due to the fact that the number of arrHeader is generally
    // quite small (usually under 12 arrHeader), we use a simple array with
    // linear access times, which proves to be more efficient and
    // straightforward than the alternatives
    protected Header[] arrHeader = new Header[12];
    protected int count;

    /**
     * Adds a header with the given name and value to the end of this
     * collection of headers. Leading and trailing whitespace are trimmed.
     *
     * @param name  the header name (case insensitive)
     * @param value the header value
     */
    public void add(String name, String value) {
        Header header = new Header(name, value); // also validates

        // expand array if necessary
        if (count == arrHeader.length)
        {
            Header[] expanded = new Header[2 * count];
            System.arraycopy(arrHeader, 0, expanded, 0, count);
            arrHeader = expanded;
        }

        arrHeader[count++] = header; // inlining header would cause a bug!
    }

    /**
     * Adds all given arrHeader to the end of this collection of headers,
     * in their original order.
     *
     * @param headers the arrHeader to add
     */
    public void addAll(Headers headers) {
        for (Header header : headers)
        {
            add(header.getName(), header.getValue());
        }
    }

    /**
     * Returns whether there exists a header with the given name.
     *
     * @param name the header name (case insensitive)
     *
     * @return whether there exists a header with the given name
     */
    public boolean contains(String name) {
        return get(name) != null;
    }

    /**
     * Returns the value of the first header with the given name.
     *
     * @param name the header name (case insensitive)
     *
     * @return the header value, or null if none exists
     */
    public String get(String name) {
        for (int i = 0; i < count; i++)
        {
            if (arrHeader[i].getName().equalsIgnoreCase(name))
            {
                return arrHeader[i].getValue();
            }
        }
        return null;
    }

    /**
     * Returns the Date value of the header with the given name.
     *
     * @param name the header name (case insensitive)
     *
     * @return the header value as a Date, or null if none exists
     *         or if the value is not in any supported date format
     */
    public Date getDate(String name) {
        try
        {
            String header = get(name);
            return header == null ? null : parseDate(header);
        } catch (IllegalArgumentException iae)
        {
            return null;
        }
    }

    /**
     * Returns a header's parameters. Parameter order is maintained,
     * and the first key (in iteration order) is the header's value
     * without the parameters.
     *
     * @param name the header name (case insensitive)
     *
     * @return the header's parameter names and values
     */
    public Map<String, String> getParams(String name) {
        Map<String, String> params = new LinkedHashMap<>();

        for (String param : split(get(name), ";", -1))
        {
            String[] pair = split(param, "=", 2);
            String val = pair.length == 1 ? "" : trimLeft(trimRight(pair[1], '"'), '"');
            params.put(pair[0], val);
        }
        return params;
    }

    /**
     * Returns an iterator over the headers, in their insertion order.
     * If the headers collection is modified during iteration, the
     * iteration result is undefined. The remove operation is unsupported.
     *
     * @return an Iterator over the arrHeader
     */
    @Override
    public Iterator<Header> iterator() {
        // we use the built-in wrapper instead of a trivial custom implementation
        // since even a tiny anonymous class here compiles to a 1.5K class file
        return Arrays.asList(arrHeader).subList(0, count).iterator();
    }

    /**
     * Removes all arrHeader with the given name (if any exist).
     *
     * @param name the header name (case insensitive)
     */
    public void remove(String name) {
        int j = 0;

        for (int i = 0; i < count; i++)
        {
            if (!arrHeader[i].getName().equalsIgnoreCase(name))
            {
                arrHeader[j++] = arrHeader[i];
            }
        }

        while (count > j)
        {
            arrHeader[--count] = null;
        }
    }

    /**
     * Adds a header with the given name and value, replacing the first
     * existing header with the same name. If there is no existing header
     * with the same name, it is added as in {@link #add}.
     *
     * @param name  the header name (case insensitive)
     * @param value the header value
     *
     * @return the replaced header, or null if none existed
     */
    public Header replace(String name, String value) {
        for (int i = 0; i < count; i++)
        {
            if (arrHeader[i].getName().equalsIgnoreCase(name))
            {
                Header prev = arrHeader[i];
                arrHeader[i] = new Header(name, value);
                return prev;
            }
        }

        add(name, value);
        return null;
    }

    /**
     * Returns the number of added arrHeader.
     *
     * @return the number of added arrHeader
     */
    public int size() {
        return count;
    }

    /**
     * Writes the arrHeader to the given stream (including trailing CRLF).
     *
     * @param out the stream to write the arrHeader to
     *
     * @throws IOException if an error occurs
     */
    public void writeTo(OutputStream out) throws IOException {
        for (int i = 0; i < count; i++)
        {
            out.write(getBytes(arrHeader[i].getName(), ": ", arrHeader[i].getValue()));
            out.write(CRLF);
        }

        out.write(CRLF); // ends header block
    }

}
