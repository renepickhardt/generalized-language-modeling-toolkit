package de.glmtk.learning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    /**
     * How much percent of total free memory to be allocated.
     *
     * Careful: Java allocates memory for other tasks, so we can't just set this
     * to 100%. I manually tested estimated this number experimentally.
     */
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
        LOGGER.info("Tagging '%s' -> '%s'.", inputFile, outputFile);

        System.out.println(inputFile);
        if (inputFile.equals(outputFile)) {
            Path tmpFile = Paths.get(inputFile + ".tmp");
            System.out.println(tmpFile);
            Files.deleteIfExists(tmpFile);
            Files.move(inputFile, tmpFile);
            inputFile = tmpFile;
        }

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
                    long curTime = System.currentTimeMillis();
                    if (curTime - time >= updateInterval) {
                        time = curTime;
                        LOGGER.info("%6.2f%%", 100.f * readSize / totalSize);
                    }
                }
            }
        }

        LOGGER.info("Tagging done.");
    }

    private static List<HasWord> arrayToListHasWords(String[] array) {
        List<HasWord> result = new LinkedList<HasWord>();
        for (String word : array) {
            result.add(new Word(word));
        }
        return result;
    }

}
