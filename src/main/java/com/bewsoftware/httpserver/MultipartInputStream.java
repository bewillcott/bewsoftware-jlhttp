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
 *  along with JLHTTP.  If not, see <https://www.gnu.org/licenses/>.
 *
 *  For additional info see http://www.freeutils.net/source/jlhttp/
 */
package com.bewsoftware.httpserver;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.bewsoftware.httpserver.HTTPServer.CRLF;

/**
 * The {@code MultipartInputStream} decodes an InputStream whose data has
 * a "multipart/*" content type (see RFC 2046), providing the underlying
 * data of its various parts.
 * <p>
 * The {@code InputStream} methods (e.g. {@link #read}) relate only to
 * the current part, and the {@link #nextPart} method advances to the
 * beginning of the next part.
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 1.0
 * @version 2.8.0
 */
@SuppressWarnings("ProtectedField")
public class MultipartInputStream extends FilterInputStream
{

    protected final byte[] boundary; // including leading CRLF--

    protected final byte[] buf = new byte[4096];

    protected int end; // last index of input data read into buf

    protected int head; // index of current part's data in buf

    protected int len; // length of found boundary

    protected int state; // initial, started data, start boundary, EOS, last boundary, epilogue

    protected int tail; // index of current part's data in buf

    /**
     * Constructs a MultipartInputStream with the given underlying stream.
     *
     * @param in       the underlying multipart stream
     * @param boundary the multipart boundary
     *
     * @throws NullPointerException     if the given stream or boundary is null
     * @throws IllegalArgumentException if the given boundary's size is not
     *                                  between 1 and 70
     */
    protected MultipartInputStream(InputStream in, byte[] boundary)
    {
        super(in);
        int blen = boundary.length;

        if (blen == 0 || blen > 70)
        {
            throw new IllegalArgumentException("invalid boundary length");
        }

        this.boundary = new byte[blen + 4]; // CRLF--boundary
        System.arraycopy(CRLF, 0, this.boundary, 0, 2);
        this.boundary[2] = this.boundary[3] = '-';
        System.arraycopy(boundary, 0, this.boundary, 4, blen);
    }

    @Override
    public int available() throws IOException
    {
        return tail - head;
    }

    @Override
    public boolean markSupported()
    {
        return false;
    }

    /**
     * Advances the stream position to the beginning of the next part.
     * Data read before calling this method for the first time is the preamble,
     * and data read after this method returns false is the epilogue.
     *
     * @return true if successful, or false if there are no more parts
     *
     * @throws IOException if an error occurs
     */
    @SuppressWarnings("empty-statement")
    public boolean nextPart() throws IOException
    {
        while (skip(buf.length) != 0); // skip current part (until boundary)

        head = tail += len; // the next part starts right after boundary
        state |= 1; // started data (after first boundary)

        if (state >= 8)
        { // found last boundary
            state |= 0x10; // now beyond last boundary (epilogue)
            return false;
        }

        findBoundary(); // update indices

        return true;
    }

    @Override
    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    public int read() throws IOException
    {
        if (!fill())
        {
            return -1;
        }

        return buf[head++] & 0xFF;
    }

    @Override
    @SuppressWarnings("AssignmentToMethodParameter")
    public int read(byte[] b, int off, int len) throws IOException
    {
        if (!fill())
        {
            return -1;
        }

        len = Math.min(tail - head, len);
        System.arraycopy(buf, head, b, off, len); // throws IOOBE as necessary
        head += len;

        return len;
    }

    @Override
    @SuppressWarnings("AssignmentToMethodParameter")
    public long skip(final long len) throws IOException
    {
        if (len <= 0 || !fill())
        {
            return 0;
        }

        final long rtn = Math.min(tail - head, len);
        head += (int) rtn;

        return rtn;
    }

    /**
     * Fills the buffer with more data from the underlying stream.
     *
     * @return true if there is available data for the current part,
     *         or false if the current part's end has been reached
     *
     * @throws IOException if an error occurs or the input format is invalid
     */
    protected boolean fill() throws IOException
    {
        // check if we already have more available data
        if (head != tail) // remember that if we continue, head == tail below
        {
            return true;
        }

        // if there's no more room, shift extra unread data to beginning of buffer
        if (tail > buf.length - 256)
        { // max boundary + whitespace supported size
            System.arraycopy(buf, tail, buf, 0, end -= tail);
            head = tail = 0;
        }

        // read more data and look for boundary (or potential partial boundary)
        int read;

        do
        {
            read = super.read(buf, end, buf.length - end);
            if (read < 0)
            {
                state |= 4; // end of stream (EOS)
            } else
            {
                end += read;
            }

            findBoundary(); // updates tail and length to next potential boundary
            // if we found a partial boundary with no data before it, we must
            // continue reading to determine if there is more data or not
        } while (read > 0 && tail == head && len == 0);

        // update and validate state
        if (tail != 0) // anything but a boundary right at the beginning
        {
            state |= 1; // started data (preamble or after boundary)
        }

        if (state < 8 && len > 0)
        {
            state |= 2; // found start boundary
        }

        if ((state & 6) == 4 // EOS but no start boundary found
                || len == 0 && ((state & 0xFC) == 4 // EOS but no last and no more boundaries
                || read == 0 && tail == head)) // boundary longer than buffer
        {
            throw new IOException("missing boundary");
        }

        if (state >= 0x10) // in epilogue
        {
            tail = end; // ignore boundaries, return everything
        }

        return tail > head; // available data in current part
    }

    /**
     * Finds the first (potential) boundary within the buffer's remaining data.
     * Updates tail, length and state fields accordingly.
     *
     * @throws IOException if an error occurs or the input format is invalid
     */
    protected void findBoundary() throws IOException
    {
        // see RFC2046#5.1.1 for boundary syntax
        len = 0;
        int off = tail - ((state & 1) != 0 || buf[0] != '-' ? 0 : 2); // skip initial CRLF?

        for (int lEnd = this.end; tail < lEnd; tail++, off = tail)
        {
            int j = tail; // end of potential boundary

            // try to match boundary value (leading CRLF is optional at first boundary)
            while (j < lEnd && j - off < boundary.length && buf[j] == boundary[j - off])
            {
                j++;
            }

            // return potential partial boundary which is cut off at end of current data
            if (j + 1 >= lEnd) // at least two more chars needed for full boundary (CRLF or --)
            {
                return;
            }

            // if we found the boundary value, expand selection to include full line
            if (j - off == boundary.length)
            {
                // check if last boundary of entire multipart
                if (buf[j] == '-' && buf[j + 1] == '-')
                {
                    j += 2;
                    state |= 8; // found last boundary that ends multipart
                }

                // allow linear whitespace after boundary
                while (j < lEnd && (buf[j] == ' ' || buf[j] == '\t'))
                {
                    j++;
                }

                // check for CRLF (required, except in last boundary with no epilogue)
                if (j + 1 < lEnd && buf[j] == '\r' && buf[j + 1] == '\n') // found CRLF
                {
                    len = j - tail + 2; // including optional whitespace and CRLF
                } else if (j + 1 < lEnd || (state & 4) != 0 && j + 1 == lEnd) // should have found or never will
                {
                    throw new IOException("boundary must end with CRLF");
                } else if ((state & 4) != 0) // last boundary with no CRLF at end of data is valid
                {
                    len = j - tail;
                }

                return;
            }
        }
    }
}
