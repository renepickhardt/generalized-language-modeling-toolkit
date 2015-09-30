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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import de.glmtk.counts.Counts;


public class CountsWriter extends AbstractFileWriter {
    public CountsWriter(Path file,
                        Charset charset) throws IOException {
        this(file, charset, 8192);
    }

    public CountsWriter(Path file,
                        Charset charset,
                        int sz) throws IOException {
        super(file, charset, sz);
    }

    public void append(String sequence,
                       long count) throws IOException {
        writer.append(sequence).append('\t').append(Long.toString(count))
            .append('\n');
    }

    public void append(String sequence,
                       Counts counts) throws IOException {
        writer.append(sequence);
        writer.append('\t').append(Long.toString(counts.getOnePlusCount()));
        writer.append('\t').append(Long.toString(counts.getOneCount()));
        writer.append('\t').append(Long.toString(counts.getTwoCount()));
        writer.append('\t').append(Long.toString(counts.getThreePlusCount()));
        writer.append('\n');
    }
}
