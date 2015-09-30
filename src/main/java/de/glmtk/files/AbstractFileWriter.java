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

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import de.glmtk.util.NioUtils;


public abstract class AbstractFileWriter implements Closeable, AutoCloseable {
    protected BufferedWriter writer;

    public AbstractFileWriter(Path file,
                              Charset charset) throws IOException {
        this(file, charset, 8192);
    }

    public AbstractFileWriter(Path file,
                              Charset charset,
                              int sz) throws IOException {
        writer = NioUtils.newBufferedWriter(file, charset, sz);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
