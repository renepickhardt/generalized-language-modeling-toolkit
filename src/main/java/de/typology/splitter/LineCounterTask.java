package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LineCounterTask implements Runnable {

    protected InputStream inputStream;

    protected File outputDirectory;

    protected String patternLabel;

    protected String delimiter;

    protected boolean setCountToOne;

    private Logger logger = LogManager.getLogger(this.getClass().getName());

    public LineCounterTask(
            InputStream inputStream,
            File outputDirectory,
            String patternLabel,
            String delimiter,
            boolean setCountToOne) {
        this.inputStream = inputStream;
        this.outputDirectory = outputDirectory;
        this.patternLabel = patternLabel;
        this.delimiter = delimiter;
        this.setCountToOne = setCountToOne;
    }

    @Override
    public void run() {
        try {
            File outputDirectory =
                    new File(this.outputDirectory.getAbsolutePath() + "/"
                            + patternLabel);
            if (outputDirectory.exists()) {
                FileUtils.deleteDirectory(outputDirectory);
            }
            outputDirectory.mkdir();
            logger.info("count lines for: " + outputDirectory.getAbsolutePath());

            long onePlusCount = 0L;
            long oneCount = 0L;
            long twoCount = 0L;
            long threePlusCount = 0L;

            try (BufferedReader inputStreamReader =
                    new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = inputStreamReader.readLine()) != null) {
                    if (setCountToOne) {
                        onePlusCount++;
                    } else {
                        long count = Long.parseLong(line.split(delimiter)[1]);

                        onePlusCount += count;
                        if (count == 1L) {
                            oneCount += count;
                        }
                        if (count == 2L) {
                            twoCount += count;
                        }
                        if (count >= 3L) {
                            threePlusCount += count;
                        }
                    }
                }
            }

            try (BufferedWriter bufferedWriter =
                    new BufferedWriter(new FileWriter(
                            outputDirectory.getAbsolutePath() + "/" + "all"))) {
                bufferedWriter.write(onePlusCount + delimiter + oneCount
                        + delimiter + twoCount + delimiter + threePlusCount
                        + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
