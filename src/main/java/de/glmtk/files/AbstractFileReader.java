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

package de.glmtk.files;

import java.io.Closeable;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Path;

import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;
import de.glmtk.exceptions.FileFormatException;
import de.glmtk.util.NioUtils;


public abstract class AbstractFileReader implements Closeable, AutoCloseable {
    protected static long parseNumber(String value)
            throws NumberFormatException {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(
                String.format("Unable to parse '%s' as a number.", value));
        }
    }

    protected static double parseFloatingPoint(String value)
            throws IllegalArgumentException {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(String.format(
                "Unable to parse '%s' as a floating point number.", value));
        }
    }

    protected static Pattern parsePattern(String value)
            throws IllegalArgumentException {
        try {
            return Patterns.get(value);
        } catch (RuntimeException e) {
            throw new RuntimeException(
                String.format("Unable to parse '%s' as a pattern.", value));
        }
    }

    protected Path file;
    protected String line;
    protected LineNumberReader reader;

    public AbstractFileReader(Path file,
                              Charset charset) throws IOException {
        this(file, charset, 8192);
    }

    public AbstractFileReader(Path file,
                              Charset charset,
                              int sz) throws IOException {
        this.file = file;
        line = "undefined"; // so isEof() is not true if readLine() hasn't been
                            // called yet.
        reader = NioUtils.newLineNumberReader(file, charset, sz);
    }

    public String readLine() throws IOException {
        line = reader.readLine();
        parseLine();
        return line;
    }

    protected abstract void parseLine();

    public boolean isEof() {
        return line == null;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    public Path getFile() {
        return file;
    }

    public int getLineNo() {
        return reader.getLineNumber();
    }

    public String getLine() {
        return line;
    }

    protected FileFormatException newFileFormatException(String fileType,
                                                         String message,
                                                         Object... params) {
        throw new FileFormatException(line, getLineNo(), file, fileType,
            message, params);
    }
}
