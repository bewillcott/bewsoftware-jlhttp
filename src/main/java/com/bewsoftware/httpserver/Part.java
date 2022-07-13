/*
 *  File Name:    Part.java
 *  Project Name: bewsoftware-jlhttp
 *
 *  Copyright (c) 2022 Bradley Willcott
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.bewsoftware.httpserver;

import java.io.IOException;
import java.io.InputStream;

/**
 * The {@code Part} class encapsulates a single part of the multipart.
 * <p>
 * Refactored out to separate file: v2.6.3.
 *
 * @since 1.0
 * @version 2.6.3
 */
@SuppressWarnings(value = "PublicField")
public class Part
{
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

    public Part()
    {
    }

    /**
     * *
     * Returns the part's body as a string. If the part
     * headers do not specify a charset, UTF-8 is used.
     *
     * @return the part's body as a string
     *
     * @throws IOException if an IO error occurs
     */
    public String getString() throws IOException
    {
        String charset = headers.getParams("Content-Type").get("charset");
        charset = charset == null ? "UTF-8" : charset;
        return FileUtils.readToken(body, -1, charset, 8192);
    }
}
