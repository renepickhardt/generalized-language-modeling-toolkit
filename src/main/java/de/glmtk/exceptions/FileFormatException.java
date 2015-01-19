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

import java.nio.file.Path;
import java.util.Formatter;

import org.yaml.snakeyaml.error.Mark;

public class FileFormatException extends RuntimeException {

    private static final long serialVersionUID = 7551127644841955051L;

    private static String assembleMessage(Path file,
                                          String fileType,
                                          String message,
                                          Object... params) {
        if (fileType == null)
            fileType = "";
        else
            fileType += ' ';

        try (Formatter f = new Formatter()) {
            f.format(message, params);
            f.format("%n In %s file '%s'.", fileType, file);
            return f.toString();
        }
    }

    private static String assembleMessage(String line,
                                          Integer lineNo,
                                          Path file,
                                          String fileType,
                                          String message,
                                          Object... params) {
        if (fileType == null)
            fileType = "";
        else
            fileType += ' ';

        try (Formatter f = new Formatter()) {
            f.format(message, params);
            f.format("%nIn %s file '%s', line %d:%n", fileType, file, lineNo);
            f.format("    " + line);
            return f.toString();
        }
    }

    private static String assembleMessage(Path file,
                                          String fileType,
                                          Mark mark,
                                          String message,
                                          Object... params) {
        int line = mark.getLine() + 1;
        int column = mark.getColumn() + 1;
        if (fileType == null)
            fileType = "";
        else
            fileType += ' ';

        try (Formatter f = new Formatter()) {
            f.format(message, params);
            f.format("%nIn %s file '%s', line %d, column %d:%n", fileType,
                    file, line, column);
            f.format(mark.get_snippet());
            return f.toString();
        }
    }

    public FileFormatException(Path file,
                               String fileType,
                               String message,
                               Object... params) {
        assembleMessage(file, fileType, message, params);
    }

    public FileFormatException(String line,
                               Integer lineNo,
                               Path file,
                               String fileType,
                               String message,
                               Object... params) {
        super(assembleMessage(line, lineNo, file, fileType, message, params));
    }

    public FileFormatException(Path file,
                               String fileType,
                               Mark mark,
                               String message,
                               Object... params) {
        super(assembleMessage(file, fileType, mark, message, params));
    }
}
