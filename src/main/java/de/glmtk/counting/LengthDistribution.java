package de.glmtk.counting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import de.glmtk.Constants;
import de.glmtk.util.StringUtils;

public class LengthDistribution {

    private List<Double> lengthDistribution;

    public LengthDistribution(
            Path file,
            boolean fromTraining) throws IOException {
        if (fromTraining) {
            readFromTraining(file);
        } else {
            readFromStore(file);
        }
    }

    private void readFromTraining(Path trainingFile) throws IOException {
        lengthDistribution = new ArrayList<Double>();

        int sum = 0;
        try (BufferedReader reader =
                Files.newBufferedReader(trainingFile, Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                // TODO: This is a slow method to get sentence length, better would be to count number of spaces.
                int length = StringUtils.splitAtChar(line, ' ').size();

                ++sum;
                if (lengthDistribution.size() <= length) {
                    for (int i = lengthDistribution.size(); i != length + 1; ++i) {
                        lengthDistribution.add(0.0);
                    }
                    lengthDistribution.set(length, 1.0);
                } else {
                    lengthDistribution.set(length,
                            lengthDistribution.get(length) + 1.0);
                }
            }
        }

        for (int i = 0; i != lengthDistribution.size(); ++i) {
            lengthDistribution.set(i, lengthDistribution.get(i) / sum);
        }
    }

    private void readFromStore(Path storeFile) throws IOException {
        lengthDistribution = new ArrayList<Double>();

        try (BufferedReader reader =
                Files.newBufferedReader(storeFile, Constants.CHARSET)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                ++lineNo;
                List<String> split = StringUtils.splitAtChar(line, '\t');

                if (split.size() != 2) {
                    throw new IllegalStateException(
                            String.format(
                                    "Length distribution file '%s' has illegal format in line %d: '%s'.",
                                    storeFile, lineNo, line));
                }

                int length = Integer.parseInt(split.get(0));
                double frequency = Double.parseDouble(split.get(1));

                for (int i = lengthDistribution.size(); i != length + 1; ++i) {
                    lengthDistribution.add(0.0);
                }
                lengthDistribution.set(length, frequency);
            }
        }
    }

    public void writeToStore(Path storeFile) throws IOException {
        try (BufferedWriter writer =
                Files.newBufferedWriter(storeFile, Constants.CHARSET)) {
            int length = 0;
            for (double frequency : lengthDistribution) {
                writer.write(Integer.toString(length));
                writer.write('\t');
                writer.write(Double.toString(frequency));
                writer.write('\n');
                ++length;
            }
        }
    }

    public double getLengthFrequency(int length) {
        if (length < 1) {
            throw new IllegalArgumentException(
                    String.format(
                            "Illegal sentences length: '%d'. Must be positive integer.",
                            length));
        }
        return lengthDistribution.get(length);
    }

    public int getMaxLength() {
        return lengthDistribution.size() - 1;
    }

}
