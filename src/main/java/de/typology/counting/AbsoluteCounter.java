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

    private boolean sortCounts;

    public AbsoluteCounter(
            Path inputDir,
            Path outputDir,
            String delimiter,
            int numberOfCores,
            boolean deleteTempFiles,
            boolean sortCounts) throws IOException {
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.delimiter = delimiter;
        this.numberOfCores = numberOfCores;
        this.deleteTempFiles = deleteTempFiles;
        this.sortCounts = sortCounts;
    }

    public void count() throws IOException, InterruptedException {
        logger.info("Counting absolute counts of sequences.");

        Files.createDirectory(outputDir);

        int bufferSize =
                (int) (MEMORY_FACTOR * (Runtime.getRuntime().maxMemory() / numberOfCores));

        // generate tasks
        List<AbsoluteCounterTask> tasks = new LinkedList<AbsoluteCounterTask>();
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
                                deleteTempFiles, sortCounts));
                    }
                }
            }
        }

        AbsoluteCounterTask.setNumTasks(tasks.size());

        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfCores);

        // execute tasks
        for (Runnable task : tasks) {
            executorService.execute(task);
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

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
