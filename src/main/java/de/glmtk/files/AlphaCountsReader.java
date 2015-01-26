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

import de.glmtk.counts.AlphaCounts;
import de.glmtk.util.StringUtils;

public class AlphaCountsReader extends SequenceReader {
    private AlphaCounts alphaCounts;

    public AlphaCountsReader(Path file,
                             Charset charset) throws IOException {
        this(file, charset, 8192);
    }

    public AlphaCountsReader(Path file,
                             Charset charset,
                             int sz) throws IOException {
        super(file, charset, sz);
        alphaCounts = null;
    }

    @Override
    protected void parseLine() {
        super.parseLine();
        if (line == null) {
            alphaCounts = null;
            return;
        }

        try {
            List<String> split = StringUtils.splitAtChar(line, '\t');
            if (split.size() == 1)
                throw new IllegalArgumentException(
                        "Expected line to have format '<sequence>\\t<alpha>+'.");

            sequence = split.get(0);
            alphaCounts = new AlphaCounts();
            for (int i = 1; i != split.size(); ++i)
                alphaCounts.append(parseFloatingPoint(split.get(i)));
        } catch (IllegalArgumentException e) {
            throw newFileFormatException("alpha counts", e.getMessage());
        }
    }

    public AlphaCounts getAlphaCounts() {
        return alphaCounts;
    }
}
