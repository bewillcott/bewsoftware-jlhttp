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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@code Context} annotation decorates methods which are mapped
 * to a context (path) within the server, and provide its contents.
 * <p>
 * The annotated methods must have the same signature and contract
 * as {@link ContextHandler#serve}, but can have arbitrary names.
 *
 * @see VirtualHost#addContexts(Object)
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 1.0
 * @version 2.5.3
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Context {

    /**
     * The HTTP methods supported by this context handler (default is "GET").
     *
     * @return the HTTP methods supported by this context handler
     */
    String[] methods() default "GET";

    /**
     * The context (path) that this field maps to (must begin with '/').
     *
     * @return the context (path) that this field maps to
     */
    String value();
}
