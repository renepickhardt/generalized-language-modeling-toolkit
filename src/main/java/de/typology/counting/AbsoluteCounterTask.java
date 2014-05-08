package de.typology.counting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AbsoluteCounterTask implements Runnable {

    private static Logger logger = LogManager.getLogger();

    private static int numTasks;

    private static int numCompleteTasks;

    private Path inputFile;

    private Path outputFile;

    private String delimiter;

    private int bufferSize;

    private boolean deleteTempFiles;

    public static void setNumTasks(int numTasks) {
        AbsoluteCounterTask.numTasks = numTasks;
    }

    public AbsoluteCounterTask(
            Path inputFile,
            Path outputFile,
            String delimiter,
            int bufferSize,
            boolean deleteTempFiles) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.delimiter = delimiter;
        this.bufferSize = bufferSize;
        this.deleteTempFiles = deleteTempFiles;
    }

    @Override
    public void run() {
        try {
            Map<String, Integer> sequenceCounts =
                    new HashMap<String, Integer>();

            try (InputStream input = Files.newInputStream(inputFile);
                    BufferedReader reader =
                            new BufferedReader(new InputStreamReader(input),
                                    bufferSize)) {
                String sequence;
                while ((sequence = reader.readLine()) != null) {
                    Integer count = sequenceCounts.get(sequence);
                    sequenceCounts.put(sequence, count == null ? 1 : count + 1);
                }
            }

            try (OutputStream output = Files.newOutputStream(outputFile);
                    BufferedWriter writer =
                            new BufferedWriter(new OutputStreamWriter(output),
                                    bufferSize)) {
                for (Map.Entry<String, Integer> sequenceCount : sequenceCounts
                        .entrySet()) {
                    String sequence = sequenceCount.getKey();
                    Integer counter = sequenceCount.getValue();
                    writer.write(sequence + delimiter + counter + "\n");
                }
            }

            if (deleteTempFiles) {
                Files.delete(inputFile);
            }

            ++numCompleteTasks;
            logger.info(String.format("%.2f", 100.f * numCompleteTasks
                    / numTasks)
                    + "% finished absolute counts for: "
                    + inputFile.getParent().getFileName()
                    + "/"
                    + inputFile.getFileName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
