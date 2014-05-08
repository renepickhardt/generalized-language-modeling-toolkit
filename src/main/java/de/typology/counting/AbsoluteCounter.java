package de.typology.counting;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Counts absolute counts of sequences for a number of patterns.
 */
public class AbsoluteCounter {

    public static float MEMORY_FACTOR = 0.2f;

    private static Logger logger = LogManager.getLogger();

    private Path inputDir;

    private Path outputDir;

    private String delimiter;

    private int numberOfCores;

    private boolean deleteTempFiles;

    public AbsoluteCounter(
            Path inputDir,
            Path outputDir,
            String delimiter,
            int numberOfCores,
            boolean deleteTempFiles) throws IOException {
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.delimiter = delimiter;
        this.numberOfCores = numberOfCores;
        this.deleteTempFiles = deleteTempFiles;
    }

    public void count() throws IOException, InterruptedException {
        logger.info("Counting absolute counts of sequences.");

        Files.createDirectory(outputDir);

        int bufferSize =
                (int) (MEMORY_FACTOR * (Runtime.getRuntime().maxMemory() / numberOfCores));

        List<Runnable> tasks = new LinkedList<Runnable>();
        try (DirectoryStream<Path> patternDirs =
                Files.newDirectoryStream(inputDir)) {
            for (Path patternDir : patternDirs) {
                Path patternOutputDir =
                        outputDir.resolve(patternDir.getFileName());
                Files.createDirectory(patternOutputDir);

                try (DirectoryStream<Path> patternFiles =
                        Files.newDirectoryStream(patternDir)) {
                    for (Path patternFile : patternFiles) {
                        Path patternOutputFile =
                                patternOutputDir.resolve(patternFile
                                        .getFileName());
                        tasks.add(new AbsoluteCounterTask(patternFile,
                                patternOutputFile, delimiter, bufferSize,
                                deleteTempFiles));
                    }
                }
            }
        }

        AbsoluteCounterTask.setNumTasks(tasks.size());

        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfCores);

        for (Runnable task : tasks) {
            executorService.execute(task);
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

        logger.info("100.00%");

        if (deleteTempFiles) {
            try (DirectoryStream<Path> patternDirs =
                    Files.newDirectoryStream(inputDir)) {
                for (Path patternDir : patternDirs) {
                    Files.delete(patternDir);
                }
            }
            Files.delete(inputDir);
        }
    }

}
