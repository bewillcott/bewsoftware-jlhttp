/*
 *  File Name:    Exceptions.java
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

/**
 * Exceptions interface description.
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 1.0.7
 * @version 1.0.7
 */
public interface Exceptions {

    /**
     * Clears all exceptions in preparation for an operation that may cause such to
     * occur.
     */
    public void clearExceptions();

    /**
     * Checks to see if there is an Exception.
     *
     * @return {@code true} if so, {@code false} otherwise.
     */
    public boolean isException();

    /**
     * Retrieves the last Exception.
     *
     * @return the Exception
     */
    public Exception popException();
}
