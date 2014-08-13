package de.glmtk.counting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class Tagger {

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(Tagger.class);

    private static final int MEMORY_PERCENT = 30;

    private int updateInterval;

    private MaxentTagger tagger;

    public Tagger(
            int updateInterval,
            Path modelFile) {
        this.updateInterval = updateInterval;
        tagger = new MaxentTagger(modelFile.toString());
    }

    public void tag(Path inputFile, Path outputFile) throws IOException {
        LOGGER.info("Tagging: %s -> %s", inputFile, outputFile);

        Runtime r = Runtime.getRuntime();
        r.gc();
        long totalFreeMemory = r.maxMemory() - r.totalMemory() + r.freeMemory();
        long memory = (MEMORY_PERCENT * totalFreeMemory) / 100;

        long readerMemory = memory * 50 / 100;
        long writerMemory = memory * 50 / 100;

        long readSize = 0;
        long totalSize = Files.size(inputFile);
        long time = System.currentTimeMillis();

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(
                        Files.newInputStream(inputFile)), (int) readerMemory);
                BufferedWriter writer =
                        new BufferedWriter(new OutputStreamWriter(
                                Files.newOutputStream(outputFile)),
                                (int) writerMemory)) {
            String line;
            while ((line = reader.readLine()) != null) {
                readSize += line.getBytes().length;

                // Tag
                String[] sentence = line.split("\\s");
                List<TaggedWord> taggedSentence =
                        tagger.tagSentence(arrayToListHasWords(sentence));

                // Write
                boolean first = true;
                for (TaggedWord tagged : taggedSentence) {
                    if (first) {
                        first = false;
                    } else {
                        writer.write(" ");
                    }
                    writer.write(tagged.word());
                    writer.write("/");
                    writer.write(tagged.tag());
                }
                writer.write("\n");

                if (updateInterval != 0) {
                    long t = System.currentTimeMillis();
                    if (t - time >= updateInterval) {
                        time = t;
                        LOGGER.info("%6.2f%%", 100.f * readSize / totalSize);
                    }
                }
            }
        }

        LOGGER.info("Tagging: done.");
    }

    private static List<HasWord> arrayToListHasWords(String[] array) {
        List<HasWord> result = new LinkedList<HasWord>();
        for (String word : array) {
            result.add(new Word(word));
        }
        return result;
    }

}
