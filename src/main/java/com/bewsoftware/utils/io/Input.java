/*
 *  File Name:    Input.java
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

import java.io.Closeable;
import java.util.Scanner;

/**
 * Input interface description.
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 1.0.7
 * @version 1.0.7
 */
public interface Input extends Closeable, Exceptions {

    /**
     * Provides a configured {@link Scanner} object.
     *
     * @return new Scanner
     */
    public Scanner newScanner();

    /**
     * Reads a single line of text from the console.
     *
     * @return A string containing the line read from the console, not
     *         including any line-termination characters, or null if an end of
     *         stream has been reached.
     */
    public String readLine();

    /**
     * Provides a formatted prompt, then reads a single line of text from the
     * console.
     *
     * @param fmt  A format string as described in Format string syntax.
     * @param args Arguments referenced by the format specifiers in the format
     *             string. If there are more arguments than format specifiers,
     *             the extra arguments are ignored.
     *
     * @return A string containing the line read from the console, not including
     *         any line-termination characters, or null if an end of stream has
     *         been reached.
     */
    public String readLine(final String fmt, final Object... args);

    /**
     * Reads a password or passphrase from the console with echoing disabled.
     *
     * @return A character array containing the password or passphrase read from
     *         the console, not including any line-termination characters, or
     *         null if an end of stream has been reached.
     */
    public char[] readPassword();

    /**
     * Provides a formatted prompt, then reads a password or passphrase from
     * the console with echoing disabled.
     *
     * @param fmt  A format string as described in Format string syntax for the
     *             prompt text.
     * @param args Arguments referenced by the format specifiers in the format
     *             string. If there are more arguments than format specifiers,
     *             the extra arguments are ignored.
     *
     * @return A character array containing the password or passphrase read from
     *         the console, not including any line-termination characters, or
     *         null if an end of stream has been reached.
     */
    public char[] readPassword(final String fmt, final Object... args);
}
