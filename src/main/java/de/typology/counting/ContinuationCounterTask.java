package de.typology.counting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexing.Index;
import de.typology.indexing.IndexWriter;
import de.typology.patterns.Pattern;
import de.typology.utils.StringUtils;

public class ContinuationCounterTask implements Runnable {

    private static Logger logger = LogManager
            .getLogger(ContinuationCounterTask.class);

    private static int numTasks = 0;

    private static int numCompleteTasks = 0;

    private Path inputDir;

    private Path outputDir;

    private Index wordIndex;

    private Pattern pattern;

    private String delimiter;

    private int bufferSize;

    private boolean fromAbsolute;

    private boolean sortCounts;

    public ContinuationCounterTask(
            Path inputDir,
            Path outputDir,
            Index wordIndex,
            Pattern pattern,
            String delimiter,
            int bufferSize,
            boolean fromAbsolute,
            boolean sortCounts) {
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.wordIndex = wordIndex;
        this.pattern = pattern;
        this.delimiter = delimiter;
        this.bufferSize = bufferSize;
        this.fromAbsolute = fromAbsolute;
        this.sortCounts = sortCounts;
    }

    @Override
    public void run() {
        try {
            Map<String, Counter> sequenceCounts = getSequenceCounts();

            if (sortCounts) {
                sequenceCounts = sortCounts(sequenceCounts);
            }

            writeSequenceCounts(sequenceCounts);

            ++numCompleteTasks;
            logger.info("%6.2f%% Finished continuation counts for: {}", 100.f
                    * numCompleteTasks / numTasks, outputDir.getFileName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Counter> getSequenceCounts() throws IOException {
        Map<String, Counter> sequenceCounts = new HashMap<String, Counter>();

        // inputDir is the directory containing the files for one pattern
        try (DirectoryStream<Path> inputFiles =
                Files.newDirectoryStream(inputDir)) {
            for (Path inputFile : inputFiles) {
                try (InputStream input = Files.newInputStream(inputFile);
                        BufferedReader reader =
                                new BufferedReader(
                                        new InputStreamReader(input),
                                        bufferSize)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        int sequencePos = line.indexOf(delimiter);
                        String sequence = line.substring(0, sequencePos);
                        int count = 1; // count is 1 if reading absolute
                        if (!fromAbsolute) {
                            int countPos =
                                    line.indexOf(delimiter, sequencePos + 1);
                            count =
                                    Integer.parseInt(line.substring(
                                            sequencePos + 1, countPos));
                        }

                        Object[] words =
                                StringUtils.splitAtSpace(sequence).toArray();
                        String patternSequence = pattern.apply(words);

                        Counter counter = sequenceCounts.get(patternSequence);
                        if (counter == null) {
                            counter = new Counter();
                            sequenceCounts.put(patternSequence, counter);
                        }
                        counter.add(count);
                    }
                }
            }
        }

        return sequenceCounts;
    }

    private Map<String, Counter>
        sortCounts(Map<String, Counter> sequenceCounts) {
        sequenceCounts = new TreeMap<String, Counter>(sequenceCounts);
        return sequenceCounts;
    }

    private void writeSequenceCounts(Map<String, Counter> sequenceCounts)
            throws IOException {
        Files.createDirectory(outputDir);
        try (IndexWriter indexWriter =
                wordIndex.openIndexWriter(pattern, outputDir, bufferSize)) {
            for (Map.Entry<String, Counter> sequenceCount : sequenceCounts
                    .entrySet()) {
                String sequence = sequenceCount.getKey();
                Counter counter = sequenceCount.getValue();

                Object[] words = StringUtils.splitAtSpace(sequence).toArray();

                BufferedWriter writer = indexWriter.get(words);
                writer.write(sequence);
                writer.write(delimiter);
                writer.write(counter.toString(delimiter));
                writer.write("\n");
            }
        }
    }

    public static void setNumTasks(int numTasks) {
        ContinuationCounterTask.numTasks = numTasks;
    }

}
