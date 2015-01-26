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
import java.util.Comparator;

import de.glmtk.util.ObjectUtils;

public class SequenceReader extends AbstractFileReader {
    public static class SequenceComparator<T extends SequenceReader> implements Comparator<T> {
        @Override
        public int compare(T lhs,
                           T rhs) {
            if (lhs == rhs)
                return 0;
            else if (lhs == null)
                return 1;
            else if (rhs == null)
                return -1;
            return ObjectUtils.compare(lhs.sequence, rhs.sequence);
        }
    }

    protected String sequence;

    public SequenceReader(Path file,
                          Charset charset) throws IOException {
        this(file, charset, 8192);
        sequence = null;
    }

    public SequenceReader(Path file,
                          Charset charset,
                          int sz) throws IOException {
        super(file, charset, sz);
    }

    @Override
    protected void parseLine() {
        if (line == null) {
            sequence = null;
            return;
        }

        int p = line.indexOf('\t');
        sequence = line.substring(0, p);
    }

    public String getSequence() {
        return sequence;
    }

    /**
     * Assumes ordering of sequences.
     */
    public void forwardToSequence(String target) throws Exception {
        while (sequence == null || !sequence.equals(target)) {
            if (isEof() || (sequence != null && sequence.compareTo(target) > 0))
                throw new Exception(String.format(
                        "Could not forward to sequence '%s' in '%s'.", target,
                        file));

            readLine();
        }
    }
}
