package de.typology.tagging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.sequencing.Sequencer;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class PosTagger {

    public static long UPDATE_INTERVAL = 5 * 1000; // 5s

    private static Logger logger = LogManager.getLogger(Sequencer.class);

    private Path inputFile;

    private Path outputFile;

    private MaxentTagger tagger;

    public PosTagger(
            Path inputFile,
            Path outputFile,
            Path modelFile) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;

        tagger = new MaxentTagger(modelFile.toString());
    }

    public void tag() throws IOException {
        logger.info("Tagging training data.");

        long readSize = 0;
        long totalSize = Files.size(inputFile);
        long time = System.currentTimeMillis();

        try (BufferedReader reader =
                Files.newBufferedReader(inputFile, Charset.defaultCharset());
                BufferedWriter writer =
                        Files.newBufferedWriter(outputFile,
                                Charset.defaultCharset(),
                                StandardOpenOption.CREATE)) {
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

                if (System.currentTimeMillis() - time >= UPDATE_INTERVAL) {
                    time = System.currentTimeMillis();
                    logger.info(String.format("%6.2f", 100.f * readSize
                            / totalSize)
                            + "%");
                }
            }
        }
    }

    private static List<HasWord> arrayToListHasWords(String[] array) {
        List<HasWord> result = new LinkedList<HasWord>();
        for (String word : array) {
            result.add(new Word(word));
        }
        return result;
    }
}
