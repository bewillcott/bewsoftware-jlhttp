/*
 *  File Name:    ContextInfo.java
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
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bewsoftware.httpserver;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code ContextInfo} class holds a single context's information.
 * <p>
 * Refactored out to separate file: v2.6.3.
 *
 * @since 1.0
 * @version 2.6.3
 */
public class ContextInfo
{
    private final VirtualHost outer;

    protected final Map<String, ContextHandler> handlers = new ConcurrentHashMap<>(2);

    protected final String path;

    /**
     * Constructs a ContextInfo with the given context path.
     *
     * @param path  the context path (without trailing slash)
     * @param outer
     */
    public ContextInfo(final String path, final VirtualHost outer)
    {
        this.outer = outer;
        this.path = path;
    }

    /**
     * Adds (or replaces) a context handler for the given HTTP methods.
     *
     * @param handler the context handler
     * @param methods the HTTP methods supported by the handler (default is
     *                "GET")
     */
    @SuppressWarnings(value = "AssignmentToMethodParameter")
    public void addHandler(final ContextHandler handler, String... methods)
    {
        if (methods.length == 0)
        {
            methods = new String[]
            {
                "GET"
            };
        }
        for (String method : methods)
        {
            handlers.put(method, handler);
            outer.methods.add(method); // it's now supported by server
        }
    }

    /**
     * Returns the map of supported HTTP methods and their corresponding
     * handlers.
     *
     * @return the map of supported HTTP methods and their corresponding
     *         handlers
     */
    public Map<String, ContextHandler> getHandlers()
    {
        return Collections.unmodifiableMap(handlers);
    }

    /**
     * Returns the context path.
     *
     * @return the context path, or null if there is none
     */
    public String getPath()
    {
        return path;
    }
}
