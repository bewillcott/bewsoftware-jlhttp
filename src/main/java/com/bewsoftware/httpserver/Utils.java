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
import java.util.*;

import static com.bewsoftware.httpserver.HTTPServer.DAYS;
import static com.bewsoftware.httpserver.HTTPServer.GMT;
import static com.bewsoftware.httpserver.HTTPServer.MONTHS;
import static com.bewsoftware.httpserver.HTTPServer.isMac;
import static com.bewsoftware.httpserver.HTTPServer.isUnix;
import static com.bewsoftware.httpserver.HTTPServer.isWindows;

/**
 * Utils class contains helper methods from the original HTTPServer class.
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 2.5.3
 * @version 2.5.3
 */
public class Utils {

    /**
     * Returns an HTML-escaped version of the given string for safe display
     * within a web page. The characters '&amp;', '&gt;' and '&lt;' must always
     * be escaped, and single and double quotes must be escaped within
     * attribute values; this method escapes them always. This method can
     * be used for generating both HTML and XHTML valid content.
     *
     * @param s the string to escape
     *
     * @return the escaped string
     *
     * @see <a href="http://www.w3.org/International/questions/qa-escapes">The W3C FAQ</a>
     */
    public static String escapeHTML(String s) {
        int len = s.length();
        StringBuilder sb = new StringBuilder(len + 30);
        int start = 0;

        for (int i = 0; i < len; i++)
        {
            String ref = null;

            switch (s.charAt(i))
            {
                case '&':
                    ref = "&amp;";
                    break;
                case '>':
                    ref = "&gt;";
                    break;
                case '<':
                    ref = "&lt;";
                    break;
                case '"':
                    ref = "&quot;";
                    break;
                case '\'':
                    ref = "&#39;";
                    break;
            }

            if (ref != null)
            {
                sb.append(s.substring(start, i)).append(ref);
                start = i + 1;
            }
        }

        return start == 0 ? s : sb.append(s.substring(start)).toString();
    }

    /**
     * Formats the given time value as a string in RFC 1123 format.
     *
     * @param time the time in milliseconds since January 1, 1970, 00:00:00 GMT
     *
     * @return the given time value as a string in RFC 1123 format
     */
    public static String formatDate(long time) {
        // this implementation performs far better than SimpleDateFormat instances, and even
        // quite better than ThreadLocal SDFs - the server's CPU-bound benchmark gains over 20%!
        if (time < -62167392000000L || time > 253402300799999L)
        {
            throw new IllegalArgumentException("year out of range (0001-9999): " + time);
        }

        char[] s = "DAY, 00 MON 0000 00:00:00 GMT".toCharArray(); // copy the format template
        Calendar cal = new GregorianCalendar(GMT, Locale.getDefault());
        cal.setTimeInMillis(time);
        System.arraycopy(DAYS, 4 * (cal.get(Calendar.DAY_OF_WEEK) - 1), s, 0, 3);
        System.arraycopy(MONTHS, 4 * cal.get(Calendar.MONTH), s, 8, 3);

        int n = cal.get(Calendar.DATE);
        s[5] += n / 10;
        s[6] += n % 10;

        n = cal.get(Calendar.YEAR);
        s[12] += n / 1000;
        s[13] += n / 100 % 10;
        s[14] += n / 10 % 10;
        s[15] += n % 10;

        n = cal.get(Calendar.HOUR_OF_DAY);
        s[17] += n / 10;
        s[18] += n % 10;

        n = cal.get(Calendar.MINUTE);
        s[20] += n / 10;
        s[21] += n % 10;

        n = cal.get(Calendar.SECOND);
        s[23] += n / 10;
        s[24] += n % 10;

        return new String(s);
    }

    /**
     * Converts strings to bytes by casting the chars to bytes.
     * This is a fast way to encode a string as ISO-8859-1/US-ASCII bytes.
     * If multiple strings are provided, their bytes are concatenated.
     *
     * @param strings the strings to convert (containing only ISO-8859-1 chars)
     *
     * @return the byte array
     */
    public static byte[] getBytes(String... strings) {
        int n = 0;

        for (String s : strings)
        {
            n += s.length();
        }

        byte[] b = new byte[n];
        n = 0;

        for (String s : strings)
        {
            for (int i = 0, len = s.length(); i < len; i++)
            {
                b[n++] = (byte) s.charAt(i);
            }
        }

        return b;
    }

    /**
     * Returns a string constructed by joining the string representations of the
     * iterated objects (in order), with the delimiter inserted between them.
     *
     * @param delim the delimiter that is inserted between the joined strings
     * @param items the items whose string representations are joined
     * @param <T>   the item type
     *
     * @return the joined string
     */
    public static <T> String join(String delim, Iterable<T> items) {
        StringBuilder sb = new StringBuilder();

        for (Iterator<T> it = items.iterator(); it.hasNext();)
        {
            sb.append(it.next()).append(it.hasNext() ? delim : "");
        }

        return sb.toString();
    }

    /**
     * Matches the given ETag value against the given ETags. A match is found
     * if the given ETag is not null, and either the ETags contain a "*" value,
     * or one of them is identical to the given ETag. If strong comparison is
     * used, tags beginning with the weak ETag prefix "W/" never match.
     * See RFC2616#3.11, RFC2616#13.3.3.
     *
     * @param strong if true, strong comparison is used, otherwise weak
     *               comparison is used
     * @param etags  the ETags to match against
     * @param etag   the ETag to match
     *
     * @return true if the ETag is matched, false otherwise
     */
    public static boolean match(boolean strong, String[] etags, String etag) {
        if (etag == null || strong && etag.startsWith("W/"))
        {
            return false;
        }

        for (String e : etags)
        {
            if (e.equals("*") || (e.equals(etag) && !(strong && (e.startsWith("W/")))))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Open the default browser to the URL address.
     * <p>
     * Added by: Bradley Willcott (2020/12/08)
     *
     * @param url Address to open.
     *
     * @return process exit value: '0' is normal exit.
     *
     * @throws IOException          if any.
     * @throws InterruptedException if any.
     */
    public static int openURL(URL url) throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        int rtn = 0;

        try
        {
            if (isWindows())
            {
                rtn = rt.exec("rundll32 url.dll,FileProtocolHandler " + url).waitFor();
                System.out.println("Browser: " + url);
            } else if (isMac())
            {
                String[] cmd =
                {
                    "open", url.toString()
                };
                rtn = rt.exec(cmd).waitFor();
                System.out.println("Browser: " + url);
            } else if (isUnix())
            {
                String[] cmd =
                {
                    "xdg-open", url.toString()
                };
                rtn = rt.exec(cmd).waitFor();
                System.out.println("Browser: " + url);
            } else
            {
                try
                {
                    throw new IllegalStateException();
                } catch (IllegalStateException ex)
                {
                    System.err.println("desktop.not.supported");
                    throw ex;
                }
            }
        } catch (IOException | InterruptedException ex)
        {
            throw ex;
        }

        return rtn;
    }

    /**
     * Parses name-value pair parameters from the given "x-www-form-urlencoded"
     * MIME-type string. This is the encoding used both for parameters passed
     * as the query of an HTTP GET method, and as the content of HTML forms
     * submitted using the HTTP POST method (as long as they use the default
     * "application/x-www-form-urlencoded" encoding in their ENCTYPE attribute).
     * UTF-8 encoding is assumed.
     * <p>
     * The parameters are returned as a list of string arrays, each containing
     * the parameter name as the first element and its corresponding value
     * as the second element (or an empty string if there is no value).
     * <p>
     * The list retains the original order of the parameters.
     *
     * @param s an "application/x-www-form-urlencoded" string
     *
     * @return the parameter name-value pairs parsed from the given string,
     *         or an empty list if there are none
     */
    public static List<String[]> parseParamsList(String s) {
        if (s == null || s.length() == 0)
        {
            return Collections.emptyList();
        }

        List<String[]> params = new ArrayList<>(8);

        for (String pair : split(s, "&", -1))
        {
            int pos = pair.indexOf('=');
            String name = pos < 0 ? pair : pair.substring(0, pos);
            String val = pos < 0 ? "" : pair.substring(pos + 1);
            try
            {
                name = URLDecoder.decode(name.trim(), "UTF-8");
                val = URLDecoder.decode(val.trim(), "UTF-8");

                if (name.length() > 0)
                {
                    params.add(new String[]
                    {
                        name, val
                    });
                }
            } catch (UnsupportedEncodingException ignore)
            {
            } // never thrown
        }
        return params;
    }

    /**
     * Returns the absolute (zero-based) content range value specified
     * by the given range string. If multiple ranges are requested, a single
     * range containing all of them is returned.
     *
     * @param range  the string containing the range description
     * @param length the full length of the requested resource
     *
     * @return the requested range, or null if the range value is invalid
     */
    public static long[] parseRange(String range, long length) {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        try
        {
            for (String token : splitElements(range, false))
            {
                long start;
                long end;
                int dash = token.indexOf('-');

                if (dash == 0)
                { // suffix range
                    start = length - parseULong(token.substring(1), 10);
                    end = length - 1;
                } else if (dash == token.length() - 1)
                { // open range
                    start = parseULong(token.substring(0, dash), 10);
                    end = length - 1;
                } else
                { // explicit range
                    start = parseULong(token.substring(0, dash), 10);
                    end = parseULong(token.substring(dash + 1), 10);
                }

                if (end < start)
                {
                    throw new RuntimeException();
                }

                if (start < min)
                {
                    min = start;
                }

                if (end > max)
                {
                    max = end;
                }
            }

            if (max < 0) // no tokens
            {
                throw new RuntimeException();
            }

            if (max >= length && min < length)
            {
                max = length - 1;
            }

            return new long[]
            {
                min, max
            }; // start might be >= length!
        } catch (RuntimeException re)
        { // NFE, IOOBE or explicit RE
            return null; // RFC2616#14.35.1 - ignore header if invalid
        }
    }

    /**
     * Parses an unsigned long value. This method behaves the same as calling
     * {@link Long#parseLong(String, int)}, but considers the string invalid
     * if it starts with an ASCII minus sign ('-') or plus sign ('+').
     *
     * @param s     the String containing the long representation to be parsed
     * @param radix the radix to be used while parsing s
     *
     * @return the long represented by s in the specified radix
     *
     * @throws NumberFormatException if the string does not contain a parsable
     *                               long, or if it starts with an ASCII minus sign or plus sign
     */
    public static long parseULong(String s, int radix) throws NumberFormatException {
        long val = Long.parseLong(s, radix); // throws NumberFormatException

        if (s.charAt(0) == '-' || s.charAt(0) == '+')
        {
            throw new NumberFormatException("invalid digit: " + s.charAt(0));
        }
        return val;
    }

    /**
     * Splits the given string into its constituent non-empty trimmed elements,
     * which are delimited by any of the given delimiter characters.
     * This is a more direct and efficient implementation than using a regex
     * (e.g. String.split()), trimming the elements and removing empty ones.
     *
     * @param str        the string to split
     * @param delimiters the characters used as the delimiters between elements
     * @param limit      if positive, limits the returned array size (remaining of str in last element)
     *
     * @return the non-empty elements in the string, or an empty array
     */
    public static String[] split(String str, String delimiters, int limit) {
        if (str == null)
        {
            return new String[0];
        }
        Collection<String> elements = new ArrayList<>();
        int len = str.length();
        int start = 0;
        int end;
        while (start < len)
        {
            for (end = --limit == 0 ? len : start;
                 end < len && delimiters.indexOf(str.charAt(end)) < 0; end++);

            String element = str.substring(start, end).trim();

            if (element.length() > 0)
            {
                elements.add(element);
            }
            start = end + 1;
        }
        return elements.toArray(new String[elements.size()]);
    }

    /**
     * Splits the given element list string (comma-separated header value)
     * into its constituent non-empty trimmed elements.
     * (RFC2616#2.1: element lists are delimited by a comma and optional LWS,
     * and empty elements are ignored).
     *
     * @param list  the element list string
     * @param lower specifies whether the list elements should be lower-cased
     *
     * @return the non-empty elements in the list, or an empty array
     */
    public static String[] splitElements(String list, boolean lower) {
        return split(lower && list != null ? list.toLowerCase(Locale.US) : list, ",", -1);
    }

    /**
     * Converts a collection of pairs of objects (arrays of size two,
     * each representing a key and corresponding value) into a Map.
     * Duplicate keys are ignored (only the first occurrence of each key is considered).
     * The map retains the original collection's iteration order.
     *
     * @param pairs a collection of arrays, each containing a key and corresponding value
     * @param <K>   the key type
     * @param <V>   the value type
     *
     * @return a map containing the paired keys and values, or an empty map
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> toMap(Collection<? extends Object[]> pairs) {
        if (pairs == null || pairs.isEmpty())
        {
            return Collections.emptyMap();
        }

        Map<K, V> map = new LinkedHashMap<>(pairs.size());

        for (Object[] pair : pairs)
        {
            if (!map.containsKey((K) pair[0]))
            {
                map.put((K) pair[0], (V) pair[1]);
            }
        }
        return map;
    }

    /**
     * Returns a human-friendly string approximating the given data size,
     * e.g. "316", "1.8K", "324M", etc.
     *
     * @param size the size to display
     *
     * @return a human-friendly string approximating the given data size
     */
    public static String toSizeApproxString(long size) {
        final char[] units =
        {
            ' ', 'K', 'M', 'G', 'T', 'P', 'E'
        };

        int u;
        double s;

        for (u = 0, s = size; s >= 1000; u++, s /= 1024);

        return String.format(s < 10 ? "%.1f%c" : "%.0f%c", s, units[u]);
    }

    /**
     * Trims duplicate consecutive occurrences of the given character within the
     * given string, replacing them with a single instance of the character.
     *
     * @param s the string to trim
     * @param c the character to trim
     *
     * @return the given string with duplicate consecutive occurrences of c
     *         replaced by a single instance of c
     */
    public static String trimDuplicates(String s, char c) {
        int start = 0;

        while ((start = s.indexOf(c, start) + 1) > 0)
        {
            int end;

            for (end = start; end < s.length() && s.charAt(end) == c; end++);

            if (end > start)
            {
                s = s.substring(0, start) + s.substring(end);
            }
        }
        return s;
    }

    /**
     * Returns the given string with all occurrences of the given character
     * removed from its left side.
     *
     * @param s the string to trim
     * @param c the character to remove
     *
     * @return the trimmed string
     */
    public static String trimLeft(String s, char c) {
        int len = s.length();
        int start;

        for (start = 0; start < len && s.charAt(start) == c; start++);

        return start == 0 ? s : s.substring(start);
    }

    /**
     * Returns the given string with all occurrences of the given character
     * removed from its right side.
     *
     * @param s the string to trim
     * @param c the character to remove
     *
     * @return the trimmed string
     */
    public static String trimRight(String s, char c) {
        int len = s.length() - 1;
        int end;

        for (end = len; end >= 0 && s.charAt(end) == c; end--);

        return end == len ? s : s.substring(0, end + 1);
    }

    /**
     * Not intended to be instantiated.
     */
    private Utils() {
    }

}
