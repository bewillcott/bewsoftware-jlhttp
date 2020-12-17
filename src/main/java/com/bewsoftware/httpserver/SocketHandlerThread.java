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
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.SSLSocket;

/**
 * The {@code SocketHandlerThread} handles accepted sockets.
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 1.0
 * @version 2.5.3
 */
class SocketHandlerThread extends Thread {

    protected HTTPServer server;

    public SocketHandlerThread(HTTPServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        setName(getClass().getSimpleName() + "-" + server.port);
        try
        {
            ServerSocket serverSocket = server.serv; // keep local to avoid NPE when stopped

            while (serverSocket != null && !serverSocket.isClosed())
            {
                final Socket sock = serverSocket.accept();

                server.executor.execute(() ->
                {
                    try
                    {
                        try
                        {
                            sock.setSoTimeout(server.socketTimeout);
                            sock.setTcpNoDelay(true); // we buffer anyway, so improve latency
                            server.handleConnection(sock.getInputStream(), sock.getOutputStream());
                        } finally
                        {
                            try
                            {
                                // RFC7230#6.6 - close socket gracefully
                                // (except SSL socket which doesn't support half-closing)
                                if (!(sock instanceof SSLSocket))
                                {
                                    sock.shutdownOutput(); // half-close socket (only output)
                                    FileUtils.transfer(sock.getInputStream(), null, -1); // consume input
                                }
                            } finally
                            {
                                sock.close(); // and finally close socket fully
                            }
                        }
                    } catch (IOException ignore)
                    {
                        // NoOp
                    }
                });
            }
        } catch (IOException ignore)
        {
            // NoOp
        }
    }
}
