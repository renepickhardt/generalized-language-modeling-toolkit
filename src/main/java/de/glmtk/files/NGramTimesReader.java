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

import de.glmtk.common.Pattern;
import de.glmtk.counts.NGramTimes;
import de.glmtk.util.StringUtils;

public class NGramTimesReader extends AbstractFileReader {
    private Pattern pattern;
    private NGramTimes ngramTimes;

    public NGramTimesReader(Path file,
                            Charset charset) throws IOException {
        this(file, charset, 8192);
    }

    public NGramTimesReader(Path file,
                            Charset charset,
                            int sz) throws IOException {
        super(file, charset, sz);
    }

    @Override
    protected void parseLine() {
        if (line == null) {
            pattern = null;
            ngramTimes = null;
            return;
        }

        List<String> split = StringUtils.splitAtChar(line, '\t');
        if (split.size() != 5)
            throw newFileFormatException("ngram times",
                    "Expected line to have format '<pattern>(\\t<count>){4}'.");

        try {
            pattern = parsePattern(split.get(0));
            ngramTimes = new NGramTimes(parseNumber(split.get(1)),
                    parseNumber(split.get(2)), parseNumber(split.get(3)),
                    parseNumber(split.get(4)));
        } catch (IllegalArgumentException e) {
            throw newFileFormatException("ngram times", e.getMessage());
        }
    }

    public Pattern getPattern() {
        return pattern;
    }

    public NGramTimes getNGramTimes() {
        return ngramTimes;
    }
}
