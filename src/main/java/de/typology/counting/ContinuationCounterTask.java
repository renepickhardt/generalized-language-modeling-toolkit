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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.typology.indexing.WordIndex;
import de.typology.patterns.Pattern;
import de.typology.patterns.PatternElem;
import de.typology.utils.StringUtils;

public class ContinuationCounterTask implements Runnable {

    private static Logger logger = LoggerFactory
            .getLogger(ContinuationCounterTask.class);

    private static int numTasks = 0;

    private static int numCompleteTasks = 0;

    private Path inputDir;

    private Path outputDir;

    private WordIndex wordIndex;

    private Pattern pattern;

    private String delimiter;

    private int bufferSize;

    private boolean fromAbsolute;

    private boolean sortCounts;

    public ContinuationCounterTask(
            Path inputDir,
            Path outputDir,
            WordIndex wordIndex,
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
            logger.info(String.format("%6.2f", 100.f * numCompleteTasks
                    / numTasks)
                    + "% Finished continuation counts for: "
                    + outputDir.getFileName());
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
        List<BufferedWriter> writers =
                wordIndex.openWriters(outputDir, bufferSize);

        for (Map.Entry<String, Counter> sequenceCount : sequenceCounts
                .entrySet()) {
            String sequence = sequenceCount.getKey();
            Counter counter = sequenceCount.getValue();

            Object[] words = StringUtils.splitAtSpace(sequence).toArray();
            String indexWord = PatternElem.SKIPPED_WORD;
            for (int i = 0; indexWord.equals(PatternElem.SKIPPED_WORD)
                    && i != pattern.length(); ++i) {
                indexWord = pattern.get(i).apply((String) words[i]);
            }

            BufferedWriter writer = writers.get(wordIndex.rank(indexWord));
            writer.write(sequence);
            writer.write(delimiter);
            writer.write(counter.toString(delimiter));
            writer.write("\n");
        }

        wordIndex.closeWriters(writers);
    }

    public static void setNumTasks(int numTasks) {
        ContinuationCounterTask.numTasks = numTasks;
    }

}
