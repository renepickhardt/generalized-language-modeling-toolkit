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

package de.glmtk.scripts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.glmtk.Constants;
import de.glmtk.util.StringUtils;


/**
 * Takes a file and removes all lines have tokens not mentioned in a vocab file.
 */
public class FilterForVocab {
    public static void main(String[] args) throws IOException {
        // Path to take vocab file from.
        Path vocabFile = Paths.get("/home/lukas/langmodels/data/en0008t.vocab");
        // Path to filter for vocab.
        Path inputFile =
            Paths.get("/home/lukas/langmodels/data/en0008t.testing/5");
        // Path to write filtered output to.
        Path outputFile =
            Paths.get("/home/lukas/langmodels/data/en0008t.testing/5v");

        Set<String> vocab = loadVocab(vocabFile);

        try (BufferedReader reader =
            Files.newBufferedReader(inputFile, Constants.CHARSET);
             BufferedWriter writer =
                 Files.newBufferedWriter(outputFile, Constants.CHARSET)) {
            String line;
            readLoop: while ((line = reader.readLine()) != null) {
                List<String> tokens = StringUtils.split(line, ' ');
                for (String token : tokens) {
                    if (!vocab.contains(token)) {
                        continue readLoop;
                    }
                }
                writer.append(line).append('\n');
            }
        }
    }

    private static Set<String> loadVocab(Path vocabFile) throws IOException {
        Set<String> vocab = new HashSet<>();
        try (BufferedReader reader =
            Files.newBufferedReader(vocabFile, Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                vocab.add(line);
            }
        }
        return vocab;
    }
}
