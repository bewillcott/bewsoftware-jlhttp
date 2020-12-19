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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.bewsoftware.httpserver.Utils.trimRight;

/**
 * The {@code VirtualHost} class represents a virtual host in the server.
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 1.0
 * @version 2.5.3
 */
public class VirtualHost {

    protected final Set<String> aliases = new CopyOnWriteArraySet<>();
    protected volatile boolean allowGeneratedIndex;
    protected final ConcurrentMap<String, ContextInfo> contexts
                                                       = new ConcurrentHashMap<>();
    protected volatile String directoryIndex = "index.html";
    protected final ContextInfo emptyContext = new ContextInfo(null);
    protected final Set<String> methods = new CopyOnWriteArraySet<>();
    protected final String name;

    /**
     * Constructs a VirtualHost with the given name.
     *
     * @param name the host's name, or null if it is the default host
     */
    public VirtualHost(String name) {
        this.name = name;
        contexts.put("*", new ContextInfo(null)); // for "OPTIONS *"
    }

    /**
     * Adds an alias for this host.
     *
     * @param alias the alias
     */
    public void addAlias(String alias) {
        aliases.add(alias);
    }

    /**
     * Adds a context and its corresponding context handler to this server.
     * Paths are normalized by removing trailing slashes (except the root).
     *
     * @param path    the context's path (must start with '/')
     * @param handler the context handler for the given path
     * @param methods the HTTP methods supported by the context handler (default is "GET")
     *
     * @throws IllegalArgumentException if path is malformed
     */
    public void addContext(String path, ContextHandler handler, String... methods) {
        if (path == null || !path.startsWith("/") && !path.equals("*"))
        {
            throw new IllegalArgumentException("invalid path: " + path);
        }

        path = trimRight(path, '/'); // remove trailing slash
        ContextInfo info = new ContextInfo(path);
        ContextInfo existing = contexts.putIfAbsent(path, info);
        info = existing != null ? existing : info;
        info.addHandler(handler, methods);
    }

    /**
     * Adds contexts for all methods of the given object that
     * are annotated with the {@link Context} annotation.
     * <p>
     * <b>Changes:</b>
     * <ul>
     * <li>{@code obj} changed from {@link Object} to {@link AutoCloseable} to
     * facilitate the use of file systems other than the default. Such may
     * require closing before the server application is completely shutdown,
     * perhaps to have the opportunity to flush buffers to permanent storage.</li>
     * </ul>
     * Bradley Willcott (2020/12/19)
     *
     * @param o the object whose annotated methods are added
     *
     * @throws IllegalArgumentException if a Context-annotated
     *                                  method has an {@link Context invalid signature}
     */
    public void addContexts(AutoCloseable o) throws IllegalArgumentException {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass())
        {
            // add to contexts those with @Context annotation
            for (Method m : c.getDeclaredMethods())
            {
                Context context = m.getAnnotation(Context.class);
                if (context != null)
                {
                    m.setAccessible(true); // allow access to private method
                    ContextHandler handler = new MethodContextHandler(m, o);
                    addContext(context.value(), handler, context.methods());
                }
            }
        }
    }

    /**
     * Returns this host's aliases.
     *
     * @return the (unmodifiable) set of aliases (which may be empty)
     */
    public Set<String> getAliases() {
        return Collections.unmodifiableSet(aliases);
    }

    /**
     * Returns the context handler for the given path.
     * <p>
     * If a context is not found for the given path, the search is repeated for
     * its parent path, and so on until a jarPath context is found. If neither the
     * given path nor any of its parents has a context, an empty context is returned.
     *
     * @param path the context's path
     *
     * @return the context info for the given path, or an empty context if none exists
     */
    public ContextInfo getContext(String path) {
        // all context paths are without trailing slash
        for (path = trimRight(path, '/'); path != null; path = FileUtils.getParentPath(path))
        {
            ContextInfo info = contexts.get(path);

            if (info != null)
            {
                return info;
            }
        }

        return emptyContext;
    }

    /**
     * Gets this host's directory index file.
     *
     * @return the directory index file, or null
     */
    public String getDirectoryIndex() {
        return directoryIndex;
    }

    /**
     * Sets the directory index file. For every request whose URI ends with
     * a '/' (i.e. a directory), the index file is appended to the path,
     * and the resulting resource is served if it exists. If it does not
     * exist, an auto-generated index for the requested directory may be
     * served, depending on whether {@link #setAllowGeneratedIndex
     * a generated index is allowed}, otherwise an error is returned.
     * The default directory index file is "index.html".
     *
     * @param directoryIndex the directory index file, or null if no
     *                       index file should be used
     */
    public void setDirectoryIndex(String directoryIndex) {
        this.directoryIndex = directoryIndex;
    }

    /**
     * Returns all HTTP methods explicitly supported by at least one context
     * (this may or may not include the methods with required or built-in support).
     *
     * @return all HTTP methods explicitly supported by at least one context
     */
    public Set<String> getMethods() {
        return methods;
    }

    /**
     * Returns this host's name.
     *
     * @return this host's name, or null if it is the default host
     */
    public String getName() {
        return name;
    }

    /**
     * Returns whether auto-generated indices are allowed.
     *
     * @return whether auto-generated indices are allowed
     */
    public boolean isAllowGeneratedIndex() {
        return allowGeneratedIndex;
    }

    /**
     * Sets whether auto-generated indices are allowed. If false, and a
     * directory resource is requested, an error will be returned instead.
     *
     * @param allowed specifies whether generated indices are allowed
     */
    public void setAllowGeneratedIndex(boolean allowed) {
        this.allowGeneratedIndex = allowed;
    }

    @Override
    public String toString() {
        return "VirtualHost{"
               + "\naliases=" + aliases + ", "
               + "\nallowGeneratedIndex=" + allowGeneratedIndex + ", "
               + "\ncontexts=" + contexts + ", "
               + "\ndirectoryIndex=" + directoryIndex + ", "
               + "\nmethods=" + methods + ", "
               + "\nname=" + name + '}';
    }

    /**
     * The {@code ContextInfo} class holds a single context's information.
     */
    public class ContextInfo {

        protected final String path;
        protected final Map<String, ContextHandler> handlers
                                                    = new ConcurrentHashMap<>(2);

        /**
         * Constructs a ContextInfo with the given context path.
         *
         * @param path the context path (without trailing slash)
         */
        public ContextInfo(String path) {
            this.path = path;
        }

        /**
         * Returns the context path.
         *
         * @return the context path, or null if there is none
         */
        public String getPath() {
            return path;
        }

        /**
         * Returns the map of supported HTTP methods and their corresponding handlers.
         *
         * @return the map of supported HTTP methods and their corresponding handlers
         */
        public Map<String, ContextHandler> getHandlers() {
            return handlers;
        }

        /**
         * Adds (or replaces) a context handler for the given HTTP methods.
         *
         * @param handler the context handler
         * @param methods the HTTP methods supported by the handler (default is "GET")
         */
        public void addHandler(ContextHandler handler, String... methods) {
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
                VirtualHost.this.methods.add(method); // it's now supported by server
            }
        }
    }
}
