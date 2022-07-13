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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.util.Collections;

import static com.bewsoftware.httpserver.NetUtils.serveFile;

/**
 * The {@code JarContextHandler} services a context by mapping it
 * to a 'jar' file on disk..
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 1.0
 * @version 2.5.3
 */
@SuppressWarnings("ProtectedField")
public class JarContextHandler implements ContextHandler, AutoCloseable
{
    /**
     * The Jar File System.
     */
    protected FileSystem jarFS;

    /**
     * URI to the 'jar' file.
     */
    protected final URI jarURI;

    /**
     * Jar file Root directory.
     */
    protected String rootDir;

    /**
     * Instantiate a {@code JarContextHandler}.
     *
     * @param jarURI Path to the 'jar' file.
     * @param dir    Directory in 'jar' file to publish.
     *
     * @throws IOException        if any.
     * @throws URISyntaxException if any.
     */
    public JarContextHandler(URI jarURI, String dir) throws IOException, URISyntaxException
    {
        this.jarURI = jarURI;
        rootDir = dir != null ? dir : "";
    }

    @Override
    public void close() throws IOException
    {
        if (jarFS != null)
        {
            jarFS.close();
        }
    }

    @Override
    public int serve(Request req, Response resp) throws IOException
    {
        try
        {
            if (jarFS == null)
            {
                jarFS = FileSystems.getFileSystem(jarURI);
            }

            if (!jarFS.isOpen())
            {
                throw new FileSystemNotFoundException();
            }
        } catch (FileSystemNotFoundException ignore)
        {
            jarFS = FileSystems.newFileSystem(jarURI, Collections.emptyMap());
        }

        return serveFile(jarFS, req.getContext().getPath(), req, resp);
    }

    @Override
    public String toString()
    {
        return "JarContextHandler{"
                + "\njarURI=" + jarURI + ", "
                + "\nrootDir=" + rootDir + '}';
    }
}
