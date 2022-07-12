/*
 *  File Name:    ConsoleIO.java
 *  Project Name History:
 *  - bewsoftware-utils
 *  - bewsoftware-jlhttp
 *
 *  Copyright (c) 2021-2022 Bradley Willcott
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
package com.bewsoftware.utils.io;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class implements a console with benefits.
 * <p>
 * Apart from writing to and reading from the console, you can also write to
 * a file.
 *
 * @implNote
 * Once the ConsolIO object is closed, any further calls to any of the methods:
 * append, appendln, print, println, flush, newScanner, readLine, or
 * readPassword,
 * will cause an {@link IOException}.
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 1.0.7
 * @version 1.2.0
 */
public final class ConsoleIO implements Display, Input
{

    private static final String CLOSED = "ConsoleIO is closed.";

    /**
     * Stores the singleton of each {@link Writer} object related to each
     * specific instance of {@link PrintWriter} associated with a
     * {@code filename}.
     */
    private static final List<WriterInstance> WRITERS = new ArrayList<>();

    private final boolean blank;

    private final Console console;

    private int debugLevel = 0;

    private int displayLevel = 0;

    private Exception exception;

    private PrintWriter file;

    private final String filename;

    private Formatter formatter;

    private final String linePrefix;

    private boolean open;

    private PrintWriter out;

    private StringBuilder sb;

    /**
     * Instantiates a "blank console".
     * <p>
     * All output is thrown away.
     * <p>
     * For use by factory method.
     */
    private ConsoleIO()
    {
        this.file = null;
        this.out = null;
        this.linePrefix = "";
        this.open = true;
        this.blank = true;
        this.filename = null;
        this.console = null;
    }

    /**
     * Instantiates a fully configured "console display".
     * <p>
     * All output goes to the system console.
     * <p>
     * For use by factory method.
     *
     * @param linePrefix text to prepend to each line
     */
    private ConsoleIO(final String linePrefix)
    {
        this.open = true;
        this.file = null;
        this.filename = null;

        console = System.console();

        if (console != null)
        {
            this.out = console.writer();
        } else
        {
            this.out = new PrintWriter(System.out);
        }

        this.linePrefix = linePrefix;
        clear();
        this.blank = false;
    }

    /**
     * Instantiates a type of display based on the parameters: {@code filename}
     * and {@code withConsole}.
     *
     * @implSpec
     * If {@code filename} is not {@code null} and not {@code isBlank()}, then
     * the file will be opened/created. If the file exists, it will be
     * truncated.
     * If successful, a copy of all text will be appended to this file.
     * <p>
     * If {@code withConsole} is {@code true}, then the System console will be
     * sent a copy of all text.
     * <p>
     * For use by factory method.
     *
     * @param linePrefix  text to prepend to each line
     * @param filename    the file to output to
     * @param withConsole whether or not to output to the console, if any
     */
    private ConsoleIO(final String linePrefix, final String filename, final boolean withConsole)
    {
        this.open = true;
        boolean lBlank;

        if (withConsole)
        {
            this.console = System.console();

            if (this.console != null)
            {
                this.out = this.console.writer();
            } else
            {
                this.out = new PrintWriter(System.out);
            }

            this.linePrefix = linePrefix;
            clear();
            lBlank = false;
        } else
        {
            this.console = null;
            this.out = null;
            this.linePrefix = "";
            lBlank = true;
        }

        if (filename != null && !filename.isBlank())
        {
            this.filename = filename;
            int idx = WRITERS.indexOf(filename);

            try
            {
                if (idx == -1)
                {
                    WRITERS.add(new WriterInstance(filename));
                    idx = WRITERS.indexOf(filename);
                }

                this.file = WRITERS.get(idx).addUsage();
                lBlank = false;
            } catch (IOException ex)
            {
                exception = ex;
            }
        } else
        {
            this.file = null;
            this.filename = null;
        }

        this.blank = lBlank;
    }

    /**
     * Provides a version of the ConsolIO that displays nothing.
     *
     * @return new blank Display
     */
    public static Display blankDisplay()
    {
        return new ConsoleIO();
    }

    /**
     * Provides a fully configured console display.
     *
     * @param linePrefix text to prepend to each line
     *
     * @return new Display
     */
    public static Display consoleDisplay(final String linePrefix)
    {
        return new ConsoleIO(linePrefix);
    }

    /**
     * Provides a version of the ConsoleIO that outputs to both the console and
     * the designated file.
     *
     * @param linePrefix text to prepend to each line
     * @param filename   the file to output to
     *
     * @return new Display
     */
    public static Display consoleFileDisplay(final String linePrefix,
            final String filename)
    {

        return new ConsoleIO(linePrefix, filename, true);
    }

    /**
     * Provides a version of the ConsoleIO that outputs only to the designated
     * file.
     *
     * @param linePrefix text to prepend to each line
     * @param filename   the file to output to
     *
     * @return new Display
     */
    public static Display fileDisplay(final String linePrefix,
            final String filename)
    {

        return new ConsoleIO(linePrefix, filename, false);
    }

    @Override
    public Display append(final String text)
    {
        if (open)
        {
            if (!blank && displayOK())
            {
                sb.append(text);
            }
        } else
        {
            exception = new IOException(CLOSED);
        }

        return this;
    }

    @Override
    public void clear()
    {
        if (open)
        {
            if (!blank)
            {
                sb = new StringBuilder();
                formatter = new Formatter(sb);
            }
        } else
        {
            sb = null;
            formatter = null;
        }
    }

    @Override
    public void clearExceptions()
    {
        exception = null;
    }

    @Override
    public void close() throws IOException
    {
        flush();

        if (out != null)
        {
            out.close();
            out = null;
        }

        if (file != null)
        {
            WRITERS.get(WRITERS.indexOf(filename)).removeUsage();
            file = null;
        }

        open = false;
        clear();
    }

    @Override
    public void debugLevel(int level)
    {
        debugLevel = level;
    }

    @Override
    public void flush()
    {
        if (open)
        {
            if (out != null || file != null)
            {
                if (linePrefix != null && linePrefix.length() > 0)
                {
                    List<String> lines = sb.toString().lines().collect(Collectors.toList());
                    clear();

                    for (int i = 0; i < lines.size(); i++)
                    {
                        sb.append(linePrefix).append(lines.get(i)).append(System.lineSeparator());
                    }
                }

                if (out != null)
                {
                    out.print(sb);
                    out.flush();
                }

                if (file != null)
                {
                    file.print(sb);
                    file.flush();
                }

                clear();
            }
        } else
        {
            exception = new IOException(CLOSED);
        }
    }

    /**
     * @param format This implementation uses
     *               {@link Formatter#format(java.lang.String, java.lang.Object...)
     *               Formatter.format(String format, Object... args)}
     *               as the work-horse.
     *
     * @see java.util.Formatter - Format String Syntax
     */
    @Override
    public Display format(String format, Object... args)
    {
        if (open && displayOK())
        {
            formatter.format(
                    Locale.getDefault(Locale.Category.FORMAT),
                    format, args
            );
        }

        return this;
    }

    @Override
    public boolean isException()
    {
        return exception != null;
    }

    @Override
    public Display level(int level)
    {
        displayLevel = level;
        return this;
    }

    @Override
    public Scanner newScanner()
    {
        Scanner rtn = null;

        if (open)
        {
            if (console != null)
            {
                rtn = new Scanner(console.reader());
            } else
            {
                rtn = new Scanner(System.in);
            }
        } else
        {
            exception = new IOException(CLOSED);
        }

        return rtn;
    }

    @Override
    public Exception popException()
    {
        Exception rtn = exception;
        clearExceptions();
        return rtn;
    }

    @Override
    public String readLine()
    {
        String rtn = null;

        if (open)
        {
            if (console != null)
            {
                rtn = console.readLine();
            } else
            {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(System.in));
                try
                {
                    rtn = reader.readLine();
                } catch (IOException ex)
                {
                    exception = ex;
                }
            }
        } else
        {
            exception = new IOException(CLOSED);
        }

        return rtn;
    }

    @Override
    public String readLine(String fmt, Object... args)
    {
        String rtn = null;

        if (open)
        {
            if (console != null)
            {
                rtn = console.readLine(fmt, args);
            } else
            {
                println(String.format(fmt, args));
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(System.in));
                try
                {
                    rtn = reader.readLine();
                } catch (IOException ex)
                {
                    exception = ex;
                }
            }
        } else
        {
            exception = new IOException(CLOSED);
        }

        return rtn;
    }

    @Override
    public char[] readPassword()
    {
        char[] rtn = null;

        if (open)
        {
            if (console != null)
            {
                rtn = console.readPassword();
            } else
            {
                String input = readLine();
                rtn = input != null ? input.toCharArray() : null;
            }
        } else
        {
            exception = new IOException(CLOSED);
        }

        return rtn;
    }

    @Override
    public char[] readPassword(String fmt, Object... args)
    {
        char[] rtn = null;

        if (open)
        {
            if (console != null)
            {
                rtn = console.readPassword(fmt, args);
            } else
            {
                String input = readLine(fmt, args);
                rtn = input != null ? input.toCharArray() : null;
            }
        } else
        {
            exception = new IOException(CLOSED);
        }

        return rtn;

    }

    private boolean displayOK()
    {
        return debugLevel >= displayLevel;
    }

    /**
     * Contains a single instance of the {@link PrintWriter} object associated
     * with the file: {@code filename}.
     *
     * @since 1.0.7
     * @version 1.0.7
     */
    private static class WriterInstance implements Closeable
    {

        /**
         * Number of references of this instance of {@link PrintWriter} that are
         * still active.
         */
        private int count;

        /**
         * The filename associated with the {@link #printWriter}.
         */
        private final String filename;

        /**
         * The single instance of this {@link PrintWriter}.
         */
        private PrintWriter printWriter;

        /**
         * Will only be instantiated by parent class.
         *
         * @param filename name of file to create/open
         *
         * @throws FileNotFoundException If the given string does not denote an
         *                               existing, writable regular file and a
         *                               new regular file of that name cannot be
         *                               created, or if some other error occurs
         *                               while opening or creating the file
         *
         */
        private WriterInstance(final String filename) throws FileNotFoundException
        {
            this.filename = filename;
            this.printWriter = new PrintWriter(filename);
            this.count = 1;
        }

        /**
         * Increments the usage counter and provides a reference to the single
         * instance of {@link PrintWriter} stored internally.
         *
         * @return reference to {@link PrintWriter}
         *
         * @throws IOException if any
         */
        public PrintWriter addUsage() throws IOException
        {
            if (isOpen())
            {
                count++;
                return printWriter;
            } else
            {
                throw new IOException("PrintWriter closed.");
            }
        }

        @Override
        public void close()
        {
            count = 0;
            printWriter.flush();
            printWriter.close();
            printWriter = null;
        }

        /**
         * Number of references of this instance of {@link PrintWriter} that are
         * still active.
         *
         * @return count
         */
        public int count()
        {
            return count;
        }

        @Override
        @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }

            if (obj == null)
            {
                return false;
            }

            if (getClass() != obj.getClass())
            {
                return this.filename.equals(obj);
            }

            final WriterInstance other = (WriterInstance) obj;
            return Objects.equals(this.filename, other.filename);
        }

        /**
         * The filename associated with the {@link #printWriter}.
         *
         * @return filename
         */
        public String filename()
        {
            return filename;
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(this.filename);
        }

        /**
         *
         * @return {@code true} if open, {@code false} otherwise.
         */
        public boolean isOpen()
        {
            return printWriter != null;
        }

        /**
         * Decrements the usage counter.
         *
         * @implSpec
         * When the counter reaches zero, then the console is closed.
         */
        public void removeUsage()
        {
            if (isOpen())
            {
                --count;

                if (count == 0)
                {
                    close();
                }
            }
        }
    }
}
