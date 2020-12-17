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

/**
 * The {@code Header} class encapsulates a single HTTP header.
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 1.0
 * @version 2.5.3
 */
public class Header {

    protected final String name;
    protected final String value;

    /**
     * Constructs a header with the given name and value.
     * Leading and trailing whitespace are trimmed.
     *
     * @param name  the header name
     * @param value the header value
     *
     * @throws NullPointerException     if name or value is null
     * @throws IllegalArgumentException if name is empty
     */
    public Header(String name, String value) {
        this.name = name.trim();
        this.value = value.trim();

        // RFC2616#14.23 - header can have an empty value (e.g. Host)
        if (this.name.length() == 0) // but name cannot be empty
        {
            throw new IllegalArgumentException("name cannot be empty");
        }
    }

    /**
     * Returns this header's name.
     *
     * @return this header's name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns this header's value.
     *
     * @return this header's value
     */
    public String getValue() {
        return value;
    }
}
