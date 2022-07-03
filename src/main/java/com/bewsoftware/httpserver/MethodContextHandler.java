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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The {@code MethodContextHandler} services a context
 * by invoking a handler method on a specified object.
 * <p>
 * The method must have the same signature and contract as
 * {@link ContextHandler#serve}, but can have an arbitrary name.
 *
 * @see VirtualHost#addContexts(AutoCloseable)
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 1.0
 * @version 2.5.3
 */
public class MethodContextHandler implements ContextHandler, AutoCloseable
{

    protected final Method m;

    protected final Object obj;

    /**
     * Create instance of {@code MethodContextHandler}.
     * <p>
     * <b>Changes:</b>
     * <ul>
     * <li>{@code obj} changed from {@link Object} to {@link AutoCloseable} to
     * facilitate the use of file systems other than the default. Such may
     * require closing before the server application is completely shutdown,
     * perhaps to have the opportunity to flush buffers to permanent
     * storage.</li>
     * </ul>
     * Bradley Willcott (2020/12/19)
     *
     * @param m   Method with the same signature and contract as
     *            {@link ContextHandler#serve}.
     * @param obj The obj with the method.
     *
     * @throws IllegalArgumentException if any.
     */
    public MethodContextHandler(Method m, AutoCloseable obj) throws IllegalArgumentException
    {
        this.m = m;
        this.obj = obj;
        Class<?>[] params = m.getParameterTypes();

        if (params.length != 2
                || !Request.class.isAssignableFrom(params[0])
                || !Response.class.isAssignableFrom(params[1])
                || !int.class.isAssignableFrom(m.getReturnType()))
        {
            throw new IllegalArgumentException("invalid method signature: " + m);
        }
    }

    @Override
    public void close()
    {
        try
        {
            ((AutoCloseable) obj).close();
        } catch (Exception ex)
        {
            // Ignore
        }
    }

    @Override
    public int serve(Request req, Response resp) throws IOException
    {
        try
        {
            return (Integer) m.invoke(obj, req, resp);
        } catch (InvocationTargetException ite)
        {
            throw new IOException("error: " + ite.getCause().getMessage());
        } catch (IllegalAccessException | IllegalArgumentException e)
        {
            throw new IOException("error: " + e);
        }
    }
}
