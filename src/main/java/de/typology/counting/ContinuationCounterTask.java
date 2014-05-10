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
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexing.WordIndex;
import de.typology.patterns.Pattern;
import de.typology.patterns.PatternElem;
import de.typology.utils.StringUtils;

public class ContinuationCounterTask implements Runnable {

    private static Logger logger = LogManager.getLogger();

    private Path inputDir;

    private Path outputDir;

    private WordIndex wordIndex;

    private Pattern pattern;

    private String delimiter;

    private int bufferSize;

    private boolean fromAbsolute;

    public ContinuationCounterTask(
            Path inputDir,
            Path outputDir,
            WordIndex wordIndex,
            Pattern pattern,
            String delimiter,
            int bufferSize,
            boolean fromAbsolute) {
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.wordIndex = wordIndex;
        this.pattern = pattern;
        this.delimiter = delimiter;
        this.bufferSize = bufferSize;
        this.fromAbsolute = fromAbsolute;
    }

    @Override
    public void run() {
        try {
            Files.createDirectory(outputDir);

            Map<String, Counter> sequenceCounts =
                    new HashMap<String, Counter>();

            try (DirectoryStream<Path> inputFiles =
                    Files.newDirectoryStream(inputDir)) {
                for (Path inputFile : inputFiles) {
                    try (InputStream input = Files.newInputStream(inputFile);
                            BufferedReader reader =
                                    new BufferedReader(new InputStreamReader(
                                            input), bufferSize)) {
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

                            String[] words = StringUtils.splitAtSpace(sequence);
                            String patternSequence = pattern.apply(words);

                            Counter counter =
                                    sequenceCounts.get(patternSequence);
                            if (counter == null) {
                                counter = new Counter();
                                sequenceCounts.put(patternSequence, counter);
                            }
                            counter.add(count);
                        }
                    }
                }
            }

            SortedMap<String, Counter> sortedSequenceCounts =
                    new TreeMap<String, Counter>(sequenceCounts);

            List<BufferedWriter> writers =
                    wordIndex.openWriters(outputDir, bufferSize);

            for (Map.Entry<String, Counter> sequenceCount : sortedSequenceCounts
                    .entrySet()) {
                String sequence = sequenceCount.getKey();
                Counter counter = sequenceCount.getValue();

                String[] words = StringUtils.splitAtSpace(sequence);
                String indexWord = PatternElem.SKIPPED_WORD;
                for (int i = 0; indexWord.equals(PatternElem.SKIPPED_WORD)
                        && i != pattern.length(); ++i) {
                    indexWord = pattern.get(i).apply(words[i]);
                }

                writers.get(wordIndex.rank(indexWord)).write(
                        sequence + delimiter + counter.toString(delimiter)
                                + "\n");
            }

            wordIndex.closeWriters(writers);

            logger.info("Finished continuation counts for: "
                    + outputDir.getFileName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
