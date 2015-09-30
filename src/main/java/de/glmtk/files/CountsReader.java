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
import java.util.List;

import de.glmtk.counts.Counts;
import de.glmtk.util.StringUtils;


public class CountsReader extends SequenceReader {
    public static final SequenceComparator<CountsReader> SEQUENCE_COMPARATOR =
        new SequenceComparator<>();

    private Counts counts;
    private boolean fromAbsolute;

    public CountsReader(Path file,
                        Charset charset) throws IOException {
        this(file, charset, 8192);
    }

    public CountsReader(Path file,
                        Charset charset,
                        int sz) throws IOException {
        super(file, charset, sz);
        counts = null;
        fromAbsolute = true;
    }

    @Override
    protected void parseLine() {
        super.parseLine();
        if (line == null) {
            counts = null;
            return;
        }

        try {
            List<String> split = StringUtils.split(line, '\t');
            if (split.size() == 2) {
                fromAbsolute = true;
                counts = new Counts(parseNumber(split.get(1)), 0L, 0L, 0L);
            } else if (split.size() == 5) {
                fromAbsolute = false;
                counts = new Counts(parseNumber(split.get(1)),
                    parseNumber(split.get(2)), parseNumber(split.get(3)),
                    parseNumber(split.get(4)));
            } else {
                throw new IllegalArgumentException(
                    "Expected line to have format '<sequence>(\\t<count>){1,4}'.");
            }
        } catch (IllegalArgumentException e) {
            throw newFileFormatException("counts", e.getMessage());
        }
    }

    public long getCount() {
        return counts.getOnePlusCount();
    }

    public Counts getCounts() {
        return counts;
    }

    public boolean isFromAbsolute() {
        return fromAbsolute;
    }
}
