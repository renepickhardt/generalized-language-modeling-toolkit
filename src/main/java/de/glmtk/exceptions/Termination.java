/*
 * Generalized Language Modeling Toolkit (GLMTK)
 * 
 * Copyright (C) 2014-2015 Lukas Schmelzeisen
 * 
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 * 
 * See the AUTHORS file for contributors.
 */

package de.glmtk.exceptions;

import static java.lang.String.format;

/**
 * Exception used to trigger safe program exit.
 */
public class Termination extends RuntimeException {
    private static final long serialVersionUID = -6305395239947614194L;

    public Termination() {
        super();
    }

    /**
     * @param message
     *            Will be output to {@code stderr} on program termination.
     */
    public Termination(String message) {
        super(message);
    }

    public Termination(String format,
                       Object... args) {
        super(format(format, args));
    }

    public Termination(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     *            Will be output to {@code stderr} on program termination.
     */
    public Termination(String message,
                       Throwable cause) {
        super(message, cause);
    }

}
