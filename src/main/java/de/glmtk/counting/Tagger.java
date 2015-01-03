package de.glmtk.counting;

import static de.glmtk.Config.CONFIG;
import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Constants;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StringUtils;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public enum Tagger {
    TAGGER;

    private static final Logger LOGGER = LogManager.getFormatterLogger(Tagger.class);

    private MaxentTagger tagger = new MaxentTagger(CONFIG.getModel().toString());
    private int readerMemory;
    private int writerMemory;

    public void tag(Path inputFile,
                    Path outputFile) throws IOException {
        OUTPUT.setPhase(Phase.TAGGING, true);
        Progress progress = new Progress(Files.size(inputFile));

        if (inputFile.equals(outputFile))
            throw new IllegalArgumentException(String.format(
                    "Input- equals OutputFile: '%s'.", inputFile));

        calculateMemory();

        try (BufferedReader reader = NioUtils.newBufferedReader(inputFile,
                     Constants.CHARSET, readerMemory);
             BufferedWriter writer = NioUtils.newBufferedWriter(outputFile,
                     Constants.CHARSET, writerMemory)) {
            String line;
            while ((line = reader.readLine()) != null) {
                progress.increase(line.getBytes(Constants.CHARSET).length);

                // Tag
                List<HasWord> sequence = new LinkedList<HasWord>();
                for (String token : StringUtils.splitAtChar(line, ' '))
                    sequence.add(new Word(token));
                List<TaggedWord> taggedSequence = tagger.tagSentence(sequence);

                // Write
                boolean first = true;
                for (TaggedWord tagged : taggedSequence) {
                    if (first)
                        first = false;
                    else
                        writer.write(' ');
                    writer.write(tagged.word());
                    writer.write('/');
                    writer.write(tagged.tag());
                }
                writer.write('\n');
            }
        }
    }

    private void calculateMemory() {
        double AVAILABLE_MEM_RATIO = 0.35;

        Runtime r = Runtime.getRuntime();
        r.gc();

        long totalFreeMem = r.maxMemory() - r.totalMemory() + r.freeMemory();
        long availableMem = (long) (AVAILABLE_MEM_RATIO * totalFreeMem);

        readerMemory = (int) (availableMem / 2);
        writerMemory = (int) (availableMem - readerMemory);

        LOGGER.debug("totalFreeMem = %s", humanReadableByteCount(totalFreeMem));
        LOGGER.debug("availableMem = %s", humanReadableByteCount(availableMem));
        LOGGER.debug("readerMemory = %s", humanReadableByteCount(readerMemory));
        LOGGER.debug("writerMemory = %s", humanReadableByteCount(writerMemory));
    }
}
