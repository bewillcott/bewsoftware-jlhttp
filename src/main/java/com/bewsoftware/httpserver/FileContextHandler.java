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

import java.io.File;
import java.io.IOException;

import static com.bewsoftware.httpserver.NetUtils.serveFile;

/**
 * The {@code FileContextHandler} services a context by mapping it
 * to a file or folder (recursively) on disk.
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 1.0
 * @version 2.5.3
 */
public class FileContextHandler implements ContextHandler {

    protected final File base;

    public FileContextHandler(File dir) throws IOException {
        this.base = dir.getCanonicalFile();
    }

    @Override
    public int serve(Request req, Response resp) throws IOException {
        return serveFile(base, req.getContext().getPath(), req, resp);
    }
}
