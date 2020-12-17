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

/**
 * A {@code ContextHandler} serves the content of resources within a context.
 *
 * @see VirtualHost#addContext
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 1.0
 * @version 2.5.3
 */
public interface ContextHandler {

    /**
     * Serves the given request using the given response.
     *
     * @param req  the request to be served
     * @param resp the response to be filled
     *
     * @return an HTTP status code, which will be used in returning
     *         a default response appropriate for this status. If this
     *         method invocation already sent anything in the response
     *         (arrHeader or content), it must return 0, and no further
     *         processing will be done
     *
     * @throws IOException if an IO error occurs
     */
    int serve(Request req, Response resp) throws IOException;
}
