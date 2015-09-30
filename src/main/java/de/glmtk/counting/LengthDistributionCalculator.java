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

package de.glmtk.counting;

import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import de.glmtk.Constants;
import de.glmtk.common.Config;
import de.glmtk.common.Status;
import de.glmtk.logging.Logger;
import de.glmtk.output.ProgressBar;
import de.glmtk.util.CollectionUtils;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StringUtils;


public class LengthDistributionCalculator {
    private static final Logger LOGGER =
        Logger.get(LengthDistributionCalculator.class);

    private Config config;

    private ProgressBar progressBar;
    private Path inputFile;
    private Path outputFile;
    private int readerMemory;

    public LengthDistributionCalculator(Config config) {
        this.config = config;
    }

    public void calculate(Status status,
                          Path inputFile,
                          Path outputFile,
                          ProgressBar progressBar) throws IOException {
        if (status.isLengthDistribution()) {
            LOGGER.debug(
                "Status reports length distribution already calculated, returning.");
            return;
        }

        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.progressBar = progressBar;
        this.progressBar.total(Files.size(inputFile));
        calculateMemory();

        List<Double> frequencies = calculateLengthDistribution();
        writeLengthDistributionToFile(frequencies);

        status.setLengthDistribution();
    }

    private void calculateMemory() {
        readerMemory = config.getMemoryReader();
        LOGGER.debug("readerMemory = %s", humanReadableByteCount(readerMemory));
    }

    private List<Double> calculateLengthDistribution() throws IOException {
        List<Double> frequencies = new ArrayList<>();

        int sum = 0;

        // count absolute frequencies
        try (BufferedReader reader = NioUtils.newBufferedReader(inputFile,
            Constants.CHARSET, readerMemory)) {
            String line;
            while ((line = reader.readLine()) != null) {
                progressBar.increase(line.getBytes(Constants.CHARSET).length);

                // This is a slow method to get sequence length, better would be
                // to count number of spaces.
                int length = StringUtils.split(line, ' ').size();

                ++sum;
                CollectionUtils.fill(frequencies, length, 0.0);
                frequencies.set(length, frequencies.get(length) + 1);
            }
        }

        // convert to relative frequencies
        for (int i = 0; i != frequencies.size(); ++i) {
            frequencies.set(i, frequencies.get(i) / sum);
        }

        return frequencies;
    }

    private void writeLengthDistributionToFile(List<Double> frequencies)
            throws IOException {
        try (BufferedWriter writer =
            Files.newBufferedWriter(outputFile, Constants.CHARSET)) {
            for (int i = 0; i != frequencies.size(); ++i) {
                writer.append(Integer.toString(i)).append('\t')
                    .append(Double.toString(frequencies.get(i))).append('\n');
            }
        }
    }
}
