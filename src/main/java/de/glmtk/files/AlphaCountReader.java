/*
 * Generalized Language Modeling Toolkit (GLMTK)
 * 
 * Copyright (C) 2015 Lukas Schmelzeisen
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

import de.glmtk.counts.AlphaCount;
import de.glmtk.exceptions.FileFormatException;
import de.glmtk.util.StringUtils;

public class AlphaCountReader extends AbstractFileReader {
    private String sequence;
    private AlphaCount alphaCount;

    public AlphaCountReader(Path file,
                       Charset charset) throws IOException {
        this(file, charset, 8192);
    }

    public AlphaCountReader(Path file,
                       Charset charset,
                       int sz) throws IOException {
        super(file, charset, sz);
        sequence = null;
        alphaCount = null;
    }

    @Override
    protected void parseLine() {
        if (line == null) {
            sequence = null;
            alphaCount = null;
            return;
        }

        try {
            List<String> split = StringUtils.splitAtChar(line, '\t');
            if (split.size() != 3)
                throw new IllegalArgumentException(
                        "Expected line to have format '<sequence>\\t<normal>\\t<discounted>'.");

            sequence = split.get(0);
            alphaCount = new AlphaCount(parseFloatingPoint(split.get(1)),
                    parseFloatingPoint(split.get(2)));
        } catch (IllegalArgumentException e) {
            throw new FileFormatException(line, lineNo, file, "alpha counts",
                    e.getMessage());
        }
    }

    public String getSequence() {
        return sequence;
    }

    public AlphaCount getAlphaCounts() {
        return alphaCount;
    }
}
