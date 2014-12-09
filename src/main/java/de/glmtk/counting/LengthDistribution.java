package de.glmtk.counting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import de.glmtk.utils.StringUtils;

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
                Files.newBufferedReader(trainingFile, Charset.defaultCharset())) {
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
                Files.newBufferedReader(storeFile, Charset.defaultCharset())) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                ++lineNo;
                List<String> split = StringUtils.splitAtChar(line, '\t');

                try {
                    if (split.size() != 2) {
                        throw new IllegalStateException();
                    }

                    int length = Integer.valueOf(split.get(0));
                    double frequency = Double.valueOf(split.get(1));

                    for (int i = lengthDistribution.size(); i != length + 1; ++i) {
                        lengthDistribution.add(0.0);
                    }
                    lengthDistribution.set(length, frequency);
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Length distribution file '" + storeFile
                            + "' has illegal format in line " + lineNo
                            + ": '" + line + "'.");
                }
            }
        }
    }

    public void writeToStore(Path storeFile) throws IOException {
        try (BufferedWriter writer =
                Files.newBufferedWriter(storeFile, Charset.defaultCharset())) {
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
        return lengthDistribution.get(length);
    }

    public int getMaxLength() {
        return lengthDistribution.size() - 1;
    }

}
