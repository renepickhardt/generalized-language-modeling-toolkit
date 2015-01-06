package de.glmtk.counting;

import static de.glmtk.Config.CONFIG;
import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Constants;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Status;
import de.glmtk.util.CollectionUtils;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StringUtils;

public enum LengthDistributionCalculator {
    LENGTH_DISTRIBUTION_CALCULATOR;

    private static final Logger LOGGER = LogManager.getFormatterLogger(LengthDistributionCalculator.class);

    private Progress progress;
    private Path inputFile;
    private Path outputFile;
    private int readerMemory;

    public void calculate(Status status,
                          Path inputFile,
                          Path outputFile) throws IOException {
        OUTPUT.setPhase(Phase.LENGTH_DISTRIBUATION_CALCULATING);

        if (status.isLengthDistributionCalculated()) {
            LOGGER.debug("Status reports length distribution already calculated, returning.");
            return;
        }

        progress = new Progress(Files.size(inputFile));
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        calculateMemory();

        List<Double> frequencies = calculateLengthDistribution();
        writeLengthDistributionToFile(frequencies);

        status.setLengthDistributionCalculated();
    }

    private void calculateMemory() {
        readerMemory = CONFIG.getReaderMemory();
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
                progress.increase(line.getBytes(Constants.CHARSET).length);

                // This is a slow method to get sequence length, better would be to count number of spaces.
                int length = StringUtils.splitAtChar(line, ' ').size();

                ++sum;
                CollectionUtils.ensureListSize(frequencies, length, 0.0);
                frequencies.set(length, frequencies.get(length) + 1);
            }
        }

        // convert to relative frequencies
        for (int i = 0; i != frequencies.size(); ++i)
            frequencies.set(i, frequencies.get(i) / sum);

        return frequencies;
    }

    private void writeLengthDistributionToFile(List<Double> frequencies) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile,
                Constants.CHARSET)) {
            for (int i = 0; i != frequencies.size(); ++i)
                writer.append(Integer.toString(i)).append('\t').append(
                        Double.toString(frequencies.get(i))).append('\n');
        }
    }
}
